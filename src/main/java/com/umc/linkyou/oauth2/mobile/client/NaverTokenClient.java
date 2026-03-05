package com.umc.linkyou.oauth2.mobile.client;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverTokenClient {

    private final RestTemplate restTemplate;
    private static final String USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    public record NaverUserInfo(String externalId, String email, String name, String profileImage) {}

    public NaverUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    USER_INFO_URL, HttpMethod.GET, entity, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) throw new GeneralException(ErrorStatus._INVALID_ID_TOKEN);

            // 네이버 응답: { "response": { id, email, name, profile_image } }
            @SuppressWarnings("unchecked")
            Map<String, Object> naverResponse = (Map<String, Object>) body.get("response");
            if (naverResponse == null) throw new GeneralException(ErrorStatus._INVALID_ID_TOKEN);

            String externalId = (String) naverResponse.get("id");
            String email = (String) naverResponse.get("email");
            String name = (String) naverResponse.get("name");
            String profileImage = (String) naverResponse.get("profile_image");

            if (email == null || email.isBlank()) {
                throw new GeneralException(ErrorStatus._SOCIAL_EMAIL_REQUIRED);
            }
            if (externalId == null || externalId.trim().isEmpty()) {
                throw new GeneralException(ErrorStatus._SOCIAL_EXTERNAL_ID_REQUIRED);
            }

            return new NaverUserInfo(externalId, email, name, profileImage);

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("네이버 사용자 정보 조회 실패", e);
            throw new GeneralException(ErrorStatus._INVALID_ID_TOKEN);
        }
    }
}

