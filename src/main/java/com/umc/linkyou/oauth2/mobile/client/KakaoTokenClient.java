package com.umc.linkyou.oauth2.mobile.client;


import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoTokenClient {

    private final RestTemplate restTemplate;
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    public record KakaoUserInfo(String externalId, String email, String name, String profileImage) {}

    public KakaoUserInfo getUserInfo(String accessToken) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new GeneralException(ErrorStatus._INVALID_ID_TOKEN);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {

            headers.setBearerAuth(accessToken.trim());  // trim으로 공백 제거

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    USER_INFO_URL, HttpMethod.GET, entity, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) throw new GeneralException(ErrorStatus._INVALID_ID_TOKEN);

            Object idObj = body.get("id");
            if (idObj == null || idObj.toString().trim().isEmpty()) {
                throw new GeneralException(ErrorStatus._INVALID_ID_TOKEN);
            }
            String externalId = idObj.toString();

            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
            if (kakaoAccount == null) throw new GeneralException(ErrorStatus._INVALID_ID_TOKEN);
            @SuppressWarnings("unchecked")
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            String name = profile != null ? (String) profile.get("nickname") : null;
            String profileImage = profile != null ? (String) profile.get("profile_image_url") : null;

            String email = (String) kakaoAccount.get("email");

            if (email == null || email.isBlank()) {
                throw new GeneralException(ErrorStatus._SOCIAL_EMAIL_REQUIRED);
            }

            return new KakaoUserInfo(externalId, email, name, profileImage);
        } catch (GeneralException e) {
            throw e;  // 명시적 재throw (호출자 일관성)
    } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패", e);
            throw new GeneralException(ErrorStatus._INVALID_ID_TOKEN);
        }
    }
}
