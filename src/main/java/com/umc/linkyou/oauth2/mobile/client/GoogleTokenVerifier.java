package com.umc.linkyou.oauth2.mobile.client;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
@Slf4j
@Component
public class GoogleTokenVerifier {

    @Value("${google.client-id}")
    private String webClientId;

    public record GoogleUserInfo(String externalId, String email,
                                 String name, String profileImage) {}

    public GoogleUserInfo verify(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(List.of(webClientId)) // Web Client ID 하나로만 검증
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                // androidClientId 변수를 제거했으므로 로그에서도 제거해야 컴파일 에러가 안 납니다.
                log.error("검증 실패: GoogleIdToken이 null입니다. 설정된 webClientId: {}", webClientId);
                throw new GeneralException(ErrorStatus._INVALID_ID_TOKEN);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            log.info("Google Token 검증 성공 - User: {}, aud: {}", payload.getEmail(), payload.getAudience());

            return new GoogleUserInfo(
                    payload.getSubject(),
                    payload.getEmail(),
                    (String) payload.get("name"),
                    (String) payload.get("picture")
            );
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google ID Token 검증 중 예외 발생: {}", e.getMessage());
            throw new GeneralException(ErrorStatus._INVALID_ID_TOKEN);
        }
    }
}
