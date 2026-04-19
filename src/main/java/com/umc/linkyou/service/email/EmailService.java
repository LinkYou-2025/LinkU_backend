package com.umc.linkyou.service.email;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.config.properties.SesProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

import java.time.Year;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final SesV2Client sesV2Client;
    private final TemplateEngine templateEngine;
    private final SesProperties sesProperties;

    // 이메일 인증 메일 전송
    public void sendVerificationEmailTemplate(String toEmail,
                                              String nickname,
                                              String code,
                                              int expiresInMinutes) {
        try {
            Context context = new Context();
            context.setVariable("nickname", nickname);
            context.setVariable("code", code);
            context.setVariable("expiresInMinutes", expiresInMinutes);
            context.setVariable("year", Year.now().getValue());

            String htmlContent = templateEngine.process("email/email-verification", context);
            send(toEmail, "Link You 이메일 인증 번호", htmlContent);
            log.info("인증 메일 전송 성공: {}", toEmail);
        } catch (SesV2Exception e) {
            log.error("인증 메일 전송 실패 toEmail: {}", toEmail, e);
            throw new UserHandler(ErrorStatus._SEND_MAIL_FAILED);
        }
    }

    // 이메일 재설정 메일 전송
    public void sendPasswordResetEmail(String toEmail,
                                       String nickname,
                                       String resetUrl,
                                       int expiresInMinutes) {
        try {
            Context context = new Context();
            context.setVariable("nickname", nickname);
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("expiresInMinutes", expiresInMinutes);
            context.setVariable("year", Year.now().getValue());

            String htmlContent = templateEngine.process("email/password-reset", context);
            send(toEmail, "Link You 비밀번호 재설정", htmlContent);
            log.info("비밀번호 재설정 메일 전송 성공: {}", toEmail);
        } catch (SesV2Exception e) {
            log.error("비밀번호 재설정 메일 전송 실패: {}", toEmail, e);
            throw new UserHandler(ErrorStatus._SEND_MAIL_FAILED);
        }
    }

    // 이메일 전송 메서드
    private void send(String toEmail, String subject, String htmlContent) {
        SendEmailRequest request = SendEmailRequest.builder()
                .fromEmailAddress(sesProperties.getFrom())
                .destination(Destination.builder().toAddresses(toEmail).build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder().data(subject).charset("UTF-8").build())
                                .body(Body.builder()
                                        .html(Content.builder().data(htmlContent).charset("UTF-8").build())
                                        .build())
                                .build())
                        .build())
                .build();

        sesV2Client.sendEmail(request);
    }
}
