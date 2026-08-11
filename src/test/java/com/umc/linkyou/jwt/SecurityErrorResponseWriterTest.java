package com.umc.linkyou.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.umc.linkyou.apiPayload.code.status.auth.AuthErrorStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// {@link CustomAuthenticationEntryPoint}, {@link CustomAccessDeniedHandler}가 공통으로 쓰는 401/403 응답 포맷 검증
@DisplayName("SecurityErrorResponseWriter 단위 테스트")
class SecurityErrorResponseWriterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final SecurityErrorResponseWriter writer = new SecurityErrorResponseWriter(objectMapper);

    @Nested
    @DisplayName("write")
    class Write {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("에러코드가 주어지면 ApiResponse 포맷의 바디를 작성한다")
            void 에러코드가_주어지면_ApiResponse_포맷의_바디를_작성한다() throws Exception {
                MockHttpServletResponse response = new MockHttpServletResponse();

                writer.write(response, AuthErrorStatus.UNAUTHORIZED);

                assertThat(response.getStatus()).isEqualTo(401);
                boolean isJson = MediaType.parseMediaType(response.getContentType())
                        .isCompatibleWith(MediaType.APPLICATION_JSON);
                assertThat(isJson).isTrue();
                assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");

                Map<String, Object> body = objectMapper.readValue(response.getContentAsString(), Map.class);
                assertThat(body.get("isSuccess")).isEqualTo(false);
                assertThat(body.get("code")).isEqualTo(AuthErrorStatus.UNAUTHORIZED.getCode());
                assertThat(body.get("message")).isEqualTo(AuthErrorStatus.UNAUTHORIZED.getMessage());
                assertThat(body.get("result")).isNull();
            }
        }

        @Nested
        @DisplayName("이미 커밋된 응답")
        class AlreadyCommitted {
            @Test
            @DisplayName("이미 커밋된 응답이면 아무것도 쓰지 않는다")
            void 커밋된_응답이면_아무것도_쓰지_않는다() throws Exception {
                MockHttpServletResponse response = new MockHttpServletResponse();
                response.setCommitted(true);

                writer.write(response, AuthErrorStatus.UNAUTHORIZED);

                assertThat(response.getContentAsString()).isEmpty();
            }
        }
    }
}
