package com.umc.linkyou.infra.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.umc.linkyou.config.properties.ResendProperties;
import com.umc.linkyou.service.email.EmailSendException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResendEmailSender 테스트")
class ResendEmailSenderTest {

    @Mock private Resend resend;

    @Mock private Emails emails;

    private ResendEmailSender resendEmailSender;

    @BeforeEach
    void setUp() {
        resendEmailSender =
                new ResendEmailSender(
                        resend,
                        new ResendProperties("test-api-key", "Link You <no-reply@example.com>"));
    }

    @Nested
    @DisplayName("성공")
    class Success {

        @Test
        @DisplayName("메일 전송 시 Resend 요청을 생성하고 이메일 ID를 반환한다")
        void 메일_전송_시_Resend_요청을_생성하고_이메일_ID를_반환한다() throws ResendException {
            given(resend.emails()).willReturn(emails);
            given(emails.send(any(CreateEmailOptions.class)))
                    .willReturn(new CreateEmailResponse("email-id"));

            String emailId =
                    resendEmailSender.send("user@example.com", "이메일 제목", "<html>본문</html>");

            ArgumentCaptor<CreateEmailOptions> optionsCaptor =
                    ArgumentCaptor.forClass(CreateEmailOptions.class);
            verify(emails).send(optionsCaptor.capture());
            CreateEmailOptions options = optionsCaptor.getValue();
            assertEquals("email-id", emailId);
            assertEquals("Link You <no-reply@example.com>", options.getFrom());
            assertEquals(java.util.List.of("user@example.com"), options.getTo());
            assertEquals("이메일 제목", options.getSubject());
            assertEquals("<html>본문</html>", options.getHtml());
        }
    }

    @Nested
    @DisplayName("실패")
    class Failure {

        @Test
        @DisplayName("Resend 호출 실패 시 공급자 오류 정보를 내부 예외로 변환한다")
        void Resend_호출_실패_시_공급자_오류_정보를_내부_예외로_변환한다() throws ResendException {
            given(resend.emails()).willReturn(emails);
            given(emails.send(any(CreateEmailOptions.class)))
                    .willThrow(
                            new ResendException(
                                    429,
                                    "{\"name\":\"rate_limit_exceeded\",\"message\":\"Too many requests\"}"));

            EmailSendException exception =
                    assertThrows(
                            EmailSendException.class,
                            () ->
                                    resendEmailSender.send(
                                            "user@example.com", "이메일 제목", "<html>본문</html>"));

            assertEquals(429, exception.getStatusCode());
            assertEquals("rate_limit_exceeded", exception.getErrorName());
            assertEquals("Email provider request failed", exception.getMessage());
        }

        @Test
        @DisplayName("메일 전송 중 통신 오류 발생 시 내부 메일 예외로 변환한다")
        void 메일_전송_중_통신_오류_발생_시_내부_메일_예외로_변환한다() throws ResendException {
            given(resend.emails()).willReturn(emails);
            given(emails.send(any(CreateEmailOptions.class)))
                    .willThrow(new RuntimeException("Connection timed out"));

            EmailSendException exception =
                    assertThrows(
                            EmailSendException.class,
                            () ->
                                    resendEmailSender.send(
                                            "user@example.com", "이메일 제목", "<html>본문</html>"));

            assertNull(exception.getStatusCode());
            assertEquals("transport_error", exception.getErrorName());
            assertEquals("Email provider request failed", exception.getMessage());
        }
    }
}
