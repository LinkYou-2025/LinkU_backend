package com.umc.linkyou.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.oauth2.mobile.client.GoogleTokenVerifier;
import com.umc.linkyou.oauth2.mobile.client.KakaoTokenClient;
import com.umc.linkyou.oauth2.mobile.client.NaverTokenClient;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SocialTokenClientTest {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String NAVER_USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    @Mock private RestTemplate restTemplate;

    @Nested
    @DisplayName("카카오 사용자 정보 조회")
    class Kakao {

        @Test
        @DisplayName("빈 액세스 토큰이면 외부 호출 없이 토큰 오류를 반환한다")
        void 빈_액세스_토큰이면_외부_호출_없이_오류를_반환한다() {
            KakaoTokenClient client = new KakaoTokenClient(restTemplate);

            GeneralException ex = assertThrows(GeneralException.class, () -> client.getUserInfo(" "));

            assertEquals(ErrorStatus._INVALID_ID_TOKEN, ex.getCode());
            verifyNoInteractions(restTemplate);
        }

        @Test
        @DisplayName("정상 응답에서 식별자와 카카오 계정 프로필을 매핑한다")
        void 정상_응답에서_식별자와_프로필을_매핑한다() {
            Map<String, Object> profile = Map.of(
                    "nickname", "Kakao Name", "profile_image_url", "https://image.example/kakao.jpg");
            Map<String, Object> kakaoAccount = Map.of(
                    "email", "kakao@example.com", "profile", profile);
            Map<String, Object> body = Map.of("id", 12345L, "kakao_account", kakaoAccount);
            when(restTemplate.exchange(
                            eq(KAKAO_USER_INFO_URL),
                            eq(HttpMethod.GET),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenReturn(response(body));
            KakaoTokenClient client = new KakaoTokenClient(restTemplate);

            KakaoTokenClient.KakaoUserInfo result = client.getUserInfo(" valid-token ");

            assertEquals("12345", result.externalId());
            assertEquals("kakao@example.com", result.email());
            assertEquals("Kakao Name", result.name());
            assertEquals("https://image.example/kakao.jpg", result.profileImage());
        }

        @Test
        @DisplayName("이메일 동의가 없으면 이메일 필수 오류를 반환한다")
        void 이메일_동의가_없으면_이메일_필수_오류를_반환한다() {
            Map<String, Object> body = Map.of("id", 12345L, "kakao_account", Map.of());
            when(restTemplate.exchange(
                            eq(KAKAO_USER_INFO_URL),
                            eq(HttpMethod.GET),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenReturn(response(body));
            KakaoTokenClient client = new KakaoTokenClient(restTemplate);

            GeneralException ex = assertThrows(GeneralException.class,
                    () -> client.getUserInfo("valid-token"));

            assertEquals(ErrorStatus._SOCIAL_EMAIL_REQUIRED, ex.getCode());
        }

        @Test
        @DisplayName("사용자 정보 조회 실패 시 토큰 오류를 반환한다")
        void 사용자_정보_조회_실패_시_토큰_오류를_반환한다() {
            when(restTemplate.exchange(
                            eq(KAKAO_USER_INFO_URL),
                            eq(HttpMethod.GET),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenThrow(new RestClientException("provider unavailable"));
            KakaoTokenClient client = new KakaoTokenClient(restTemplate);

            GeneralException ex = assertThrows(GeneralException.class,
                    () -> client.getUserInfo("valid-token"));

            assertEquals(ErrorStatus._INVALID_ID_TOKEN, ex.getCode());
        }
    }

    @Nested
    @DisplayName("네이버 사용자 정보 조회")
    class Naver {

        @Test
        @DisplayName("정상 응답의 response 객체에서 사용자 정보를 매핑한다")
        void 정상_응답의_response_객체에서_사용자_정보를_매핑한다() {
            Map<String, Object> naverUser = Map.of(
                    "id", "naver-id",
                    "email", "naver@example.com",
                    "name", "Naver Name",
                    "profile_image", "https://image.example/naver.jpg");
            when(restTemplate.exchange(
                            eq(NAVER_USER_INFO_URL),
                            eq(HttpMethod.GET),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenReturn(response(Map.of("response", naverUser)));
            NaverTokenClient client = new NaverTokenClient(restTemplate);

            NaverTokenClient.NaverUserInfo result = client.getUserInfo("valid-token");

            assertEquals("naver-id", result.externalId());
            assertEquals("naver@example.com", result.email());
            assertEquals("Naver Name", result.name());
            assertEquals("https://image.example/naver.jpg", result.profileImage());
        }

        @Test
        @DisplayName("사용자 정보 응답에 이메일이 없으면 이메일 필수 오류를 반환한다")
        void 사용자_정보_응답에_이메일이_없으면_이메일_필수_오류를_반환한다() {
            when(restTemplate.exchange(
                            eq(NAVER_USER_INFO_URL),
                            eq(HttpMethod.GET),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenReturn(response(Map.of("response", Map.of("id", "naver-id"))));
            NaverTokenClient client = new NaverTokenClient(restTemplate);

            GeneralException ex = assertThrows(GeneralException.class,
                    () -> client.getUserInfo("valid-token"));

            assertEquals(ErrorStatus._SOCIAL_EMAIL_REQUIRED, ex.getCode());
        }

        @Test
        @DisplayName("사용자 정보 응답에 식별자가 없으면 식별자 오류를 반환한다")
        void 사용자_정보_응답에_식별자가_없으면_식별자_오류를_반환한다() {
            when(restTemplate.exchange(
                            eq(NAVER_USER_INFO_URL),
                            eq(HttpMethod.GET),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenReturn(response(Map.of("response", Map.of("email", "naver@example.com"))));
            NaverTokenClient client = new NaverTokenClient(restTemplate);

            GeneralException ex = assertThrows(GeneralException.class,
                    () -> client.getUserInfo("valid-token"));

            assertEquals(ErrorStatus._SOCIAL_EXTERNAL_ID_REQUIRED, ex.getCode());
        }

        @Test
        @DisplayName("사용자 정보 조회 실패 시 토큰 오류를 반환한다")
        void 사용자_정보_조회_실패_시_토큰_오류를_반환한다() {
            when(restTemplate.exchange(
                            eq(NAVER_USER_INFO_URL),
                            eq(HttpMethod.GET),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenThrow(new RestClientException("provider unavailable"));
            NaverTokenClient client = new NaverTokenClient(restTemplate);

            GeneralException ex = assertThrows(GeneralException.class,
                    () -> client.getUserInfo("valid-token"));

            assertEquals(ErrorStatus._INVALID_ID_TOKEN, ex.getCode());
        }
    }

    @Nested
    @DisplayName("구글 ID 토큰 검증")
    class Google {

        @Test
        @DisplayName("형식이 잘못된 ID 토큰은 검증 오류로 거부한다")
        void 형식이_잘못된_ID_토큰은_검증_오류로_거부한다() {
            GoogleTokenVerifier verifier = new GoogleTokenVerifier();
            ReflectionTestUtils.setField(verifier, "webClientId", "test-client-id");

            GeneralException ex = assertThrows(GeneralException.class,
                    () -> verifier.verify("not-a-valid-id-token"));

            assertEquals(ErrorStatus._INVALID_ID_TOKEN, ex.getCode());
        }
    }

    private ResponseEntity<Map> response(Map<String, Object> body) {
        return new ResponseEntity<>(body, HttpStatus.OK);
    }
}
