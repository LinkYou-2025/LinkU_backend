package com.umc.linkyou.infra.email;

import org.springframework.stereotype.Component;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.umc.linkyou.config.properties.ResendProperties;
import com.umc.linkyou.service.email.EmailSendException;
import com.umc.linkyou.service.email.EmailSender;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ResendEmailSender implements EmailSender {

    private final Resend resend;
    private final ResendProperties resendProperties;

    @Override
    public String send(String toEmail, String subject, String htmlContent) {
        CreateEmailOptions options =
                CreateEmailOptions.builder()
                        .from(resendProperties.from())
                        .to(toEmail)
                        .subject(subject)
                        .html(htmlContent)
                        .build();

        try {
            CreateEmailResponse response = resend.emails().send(options);
            return response.getId();
        } catch (ResendException e) {
            throw new EmailSendException(e.getStatusCode(), e.getErrorName(), e);
        } catch (RuntimeException e) {
            throw new EmailSendException(null, "transport_error", e);
        }
    }
}
