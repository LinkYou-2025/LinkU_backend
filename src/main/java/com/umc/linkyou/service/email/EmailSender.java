package com.umc.linkyou.service.email;

public interface EmailSender {

    String send(String toEmail, String subject, String htmlContent);
}
