package com.umc.linkyou.service.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService 테스트")
class EmailServiceTest {

    @InjectMocks private EmailService emailService;

    @Mock private EmailSender emailSender;

    @Mock private TemplateEngine templateEngine;

    @Nested
    @DisplayName("성공")
    class Success {

        @Test
        @DisplayName("인증 메일 전송 시 템플릿을 렌더링하고 발송한다")
        void 인증_메일_전송_시_템플릿을_렌더링하고_발송한다() {
            given(templateEngine.process(eq("email/email-verification"), any(IContext.class)))
                    .willReturn("<html>verification</html>");
            given(
                            emailSender.send(
                                    "user@example.com",
                                    "Link You 이메일 인증 번호",
                                    "<html>verification</html>"))
                    .willReturn("email-id");

            emailService.sendVerificationEmailTemplate("user@example.com", "링큐 회원", "123456", 10);

            ArgumentCaptor<IContext> contextCaptor = ArgumentCaptor.forClass(IContext.class);
            verify(templateEngine).process(eq("email/email-verification"), contextCaptor.capture());
            assertEquals("링큐 회원", contextCaptor.getValue().getVariable("nickname"));
            assertEquals("123456", contextCaptor.getValue().getVariable("code"));
            assertEquals(10, contextCaptor.getValue().getVariable("expiresInMinutes"));
            verify(emailSender)
                    .send("user@example.com", "Link You 이메일 인증 번호", "<html>verification</html>");
        }

        @Test
        @DisplayName("비밀번호 재설정 메일 전송 시 템플릿을 렌더링하고 발송한다")
        void 비밀번호_재설정_메일_전송_시_템플릿을_렌더링하고_발송한다() {
            String resetUrl = "https://linku.example/password/reset?token=token";
            given(templateEngine.process(eq("email/password-reset"), any(IContext.class)))
                    .willReturn("<html>password reset</html>");
            given(
                            emailSender.send(
                                    "user@example.com",
                                    "LinkU 비밀번호 재설정 링크",
                                    "<html>password reset</html>"))
                    .willReturn("email-id");

            emailService.sendPasswordResetEmail("user@example.com", "링큐 회원", resetUrl, 10);

            ArgumentCaptor<IContext> contextCaptor = ArgumentCaptor.forClass(IContext.class);
            verify(templateEngine).process(eq("email/password-reset"), contextCaptor.capture());
            assertEquals("링큐 회원", contextCaptor.getValue().getVariable("nickname"));
            assertEquals(resetUrl, contextCaptor.getValue().getVariable("resetUrl"));
            assertEquals(10, contextCaptor.getValue().getVariable("expiresInMinutes"));
            verify(emailSender)
                    .send("user@example.com", "LinkU 비밀번호 재설정 링크", "<html>password reset</html>");
        }
    }

    @Nested
    @DisplayName("실패")
    class Failure {

        @Test
        @DisplayName("메일 공급자 호출 실패 시 메일 전송 실패 예외를 반환한다")
        void 메일_공급자_호출_실패_시_메일_전송_실패_예외를_반환한다() {
            given(templateEngine.process(eq("email/email-verification"), any(IContext.class)))
                    .willReturn("<html>verification</html>");
            given(
                            emailSender.send(
                                    "user@example.com",
                                    "Link You 이메일 인증 번호",
                                    "<html>verification</html>"))
                    .willThrow(new EmailSendException(429, "rate_limit_exceeded", null));

            UserHandler exception =
                    assertThrows(
                            UserHandler.class,
                            () ->
                                    emailService.sendVerificationEmailTemplate(
                                            "user@example.com", "링큐 회원", "123456", 10));

            assertEquals(UserErrorStatus._SEND_MAIL_FAILED, exception.getCode());
        }
    }
}
