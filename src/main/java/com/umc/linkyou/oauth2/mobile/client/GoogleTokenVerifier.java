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
    // TODO: [환경변수 확인 요청] Google Cloud Console의 "웹 애플리케이션" 클라이언트 ID여야 합니다.
    // Android 클라이언트 ID 넣으면 토큰 검증 실패합니다.
    // EC2 도커 환경변수 확인 결과 해당 값 누락인 것 같은데(제가 aws 콘솔을 잘못 봣을 수 있어요 확인 한 번 부탁드립니다.)
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String webClientId;

    @Value(("${google.client-id.android}"))
    private String androidClientId;

    public record GoogleUserInfo(String externalId, String email,
                                 String name, String profileImage) {}

    public GoogleUserInfo verify(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(List.of(webClientId, androidClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) throw new GeneralException(ErrorStatus._INVALID_ID_TOKEN);

            GoogleIdToken.Payload payload = idToken.getPayload();
            return new GoogleUserInfo(
                    payload.getSubject(),
                    payload.getEmail(),
                    (String) payload.get("name"),
                    (String) payload.get("picture")
            );
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google ID Token 검증 실패", e);
            throw new GeneralException(ErrorStatus._INVALID_ID_TOKEN);
        }
    }
}
