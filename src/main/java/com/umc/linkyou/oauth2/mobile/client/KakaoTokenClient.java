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
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    USER_INFO_URL, HttpMethod.GET, entity, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) throw new GeneralException(ErrorStatus._INVALID_ID_TOKEN);

            String externalId = String.valueOf(body.get("id"));

            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
            @SuppressWarnings("unchecked")
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            String email = (String) kakaoAccount.get("email");
            String name = (String) profile.get("nickname");
            String profileImage = (String) profile.get("profile_image_url");

            if (email == null || email.isBlank()) {
                throw new GeneralException(ErrorStatus._SOCIAL_EMAIL_REQUIRED);
            }

            return new KakaoUserInfo(externalId, email, name, profileImage);

        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패", e);
            throw new GeneralException(ErrorStatus._INVALID_ID_TOKEN);
        }
    }
}
