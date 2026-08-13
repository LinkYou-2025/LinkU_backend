package com.umc.linkyou.service.email;

import java.time.Year;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 이메일 전송을 담당하는 서비스 이메일 템플릿은 Thymeleaf를 사용하여 렌더링 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String VERIFICATION_EMAIL = "VERIFICATION";
    private static final String PASSWORD_RESET_EMAIL = "PASSWORD_RESET";

    private final EmailSender emailSender;
    private final TemplateEngine templateEngine;

    // 이메일 인증 메일 전송
    public void sendVerificationEmailTemplate(
            String toEmail, String nickname, String code, int expiresInMinutes) {
        Context context = new Context();
        context.setVariable("nickname", nickname);
        context.setVariable("code", code);
        context.setVariable("expiresInMinutes", expiresInMinutes);
        context.setVariable("year", Year.now().getValue());

        String htmlContent = templateEngine.process("email/email-verification", context);
        send(toEmail, "Link You 이메일 인증 번호", htmlContent, VERIFICATION_EMAIL);
    }

    // 비밀번호 재설정 메일 전송
    public void sendPasswordResetEmail(
            String toEmail, String nickname, String resetUrl, int expiresInMinutes) {
        Context context = new Context();
        context.setVariable("nickname", nickname);
        context.setVariable("resetUrl", resetUrl);
        context.setVariable("expiresInMinutes", expiresInMinutes);
        context.setVariable("year", Year.now().getValue());

        String htmlContent = templateEngine.process("email/password-reset", context);
        send(toEmail, "Link You 비밀번호 재설정", htmlContent, PASSWORD_RESET_EMAIL);
    }

    private void send(String toEmail, String subject, String htmlContent, String emailType) {
        try {
            String emailId = emailSender.send(toEmail, subject, htmlContent);
            log.info("이메일 전송 성공 type={} emailId={}", emailType, emailId);
        } catch (EmailSendException e) {
            log.error(
                    "이메일 전송 실패 type={} status={} error={}",
                    emailType,
                    e.getStatusCode(),
                    e.getErrorName());
            throw new UserHandler(UserErrorStatus._SEND_MAIL_FAILED);
        }
    }
}
