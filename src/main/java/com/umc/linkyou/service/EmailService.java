package com.umc.linkyou.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.converter.EmailConverter;
import com.umc.linkyou.domain.EmailVerification;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.repository.EmailRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@Component
@RequiredArgsConstructor
public class EmailService {

    //private final JavaMailSender emailSender;
    private final SendGrid sendGrid;
    private final EmailRepository emailRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.sendgrid.from}")
    private String fromEmail;

    // 다이나믹 템플릿 ID & 로고 URL (S3 공개 URL)
    @Value("${spring.sendgrid.templates.verify-id}")
    private String verifyTemplateId;

    @Value("${spring.sendgrid.templates.temp-id}")
    private String tempTemplateId;

    @Value("${cloud.aws.s3.base-url}")
    private String s3BaseUrl;

    public void sendEmail(String toEmail,
                          String title,
                          String text) {
        // SMTP 과정
        //SimpleMailMessage emailForm = createEmailForm(toEmail, title, text);

        // 보내는 사람
        Email from = new Email(fromEmail);

        // 제목
        String subject = title;

        // 받는 사람
        Email to = new Email(toEmail);

        // 내용
        Content content = new Content("text/plain", text);

        // 발신자, 제목, 수신자, 내용을 합쳐 Mail 객체 생성
        Mail mail = new Mail(from, subject, to, content);

        try {
            // SMTP 과정
            //emailSender.send(emailForm);
            send(mail);
            log.info("메일 전송 성공: {}", toEmail);
        } catch (IOException e) {
            log.error("메일 전송 중 오류 발생 toEmail: {}, title: {}, text: {}", toEmail, title, text, e);
            throw new UserHandler(ErrorStatus._SEND_MAIL_FAILED);
        }
    }

    /**
     * ✅ 신규: 다이나믹 템플릿(HTML)로 인증 메일 전송
     * 템플릿 변수: nickname, code, expiresInMinutes, year, logoUrl, (옵션) verifyUrl
     */
    public void sendVerificationEmailTemplate(String toEmail,
                                              String nickname,
                                              String code,
                                              int expiresInMinutes) {
        try {
            Mail mail = new Mail();
            // From 표시 이름 추가
            mail.setFrom(new Email(fromEmail, "Link You"));

            // 템플릿 ID 지정
            mail.setTemplateId(verifyTemplateId);

            // 수신자 & 템플릿 변수
            Personalization p = new Personalization();
            p.addTo(new Email(toEmail));
            p.addDynamicTemplateData("nickname", nickname);
            p.addDynamicTemplateData("code", code);
            p.addDynamicTemplateData("expiresInMinutes", expiresInMinutes);
            p.addDynamicTemplateData("year", Year.now().getValue());
            p.addDynamicTemplateData("logoUrl", s3BaseUrl + "/linkuLogo/logo_white.png");

            mail.addPersonalization(p);

            // 발송
            send(mail);
            log.info("템플릿 인증 메일 전송 성공: {}", toEmail);
        } catch (Exception e) {
            log.error("템플릿 인증 메일 전송 실패 toEmail: {}, code: {}", toEmail, code, e);
            throw new UserHandler(ErrorStatus._SEND_MAIL_FAILED);
        }
    }
    /** ✅ 임시 비밀번호 템플릿 메일 전송 */
    public void sendTempPasswordTemplate(String toEmail,
                                         String nickname,
                                         String tempPassword,
                                         int expiresInMinutes) {
        try {
            Mail mail = new Mail();
            mail.setFrom(new Email(fromEmail, "Link You"));

            // 템플릿 ID 지정
            mail.setTemplateId(tempTemplateId);

            // 동적 데이터 세팅 (HTML의 핸들바와 일치)
            Personalization p = new Personalization();
            p.addTo(new Email(toEmail));
            p.addDynamicTemplateData("nickname", nickname);
            p.addDynamicTemplateData("tempPassword", tempPassword);
            p.addDynamicTemplateData("expiresInMinutes", expiresInMinutes);
            p.addDynamicTemplateData("year", Year.now().getValue());
            p.addDynamicTemplateData("logoUrl", s3BaseUrl + "/linkuLogo/logo_white.png");

            mail.addPersonalization(p);

            // 발송
            send(mail);
            log.info("템플릿 임시 비밀번호 메일 전송 성공: {}", toEmail);
        } catch (Exception e) {
            log.error("템플릿 임시 비밀번호 메일 전송 실패: {}", toEmail, e);
            throw new UserHandler(ErrorStatus._SEND_MAIL_FAILED);
        }
    }

    public void saveCode(String toEmail, String code) {
        Optional<EmailVerification> existing = emailRepository.findByEmail(toEmail);

        if (existing.isPresent()) {
            EmailVerification emailVerification = existing.get();
            emailVerification.setVerificationCode(code);
            emailVerification.setExpiresAt(LocalDateTime.now().plusMinutes(10));
            emailVerification.setIsVerified(false);
            emailRepository.save(emailVerification); // update
        } else {
            EmailVerification emailVerification = EmailConverter.toEmailVerification(toEmail, code, false);
            emailRepository.save(emailVerification); // insert
        }
    }

    // 실제로 보내는 함수
    private void send(Mail mail) throws IOException {
        // SendGrid 테스트용
        //sendGrid.addRequestHeader("X-Mock", "true");

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sendGrid.api(request);
    }


    // 임시 비밀번호 저장
    public void savePassword(String toEmail, String password) {
        Optional<Users> optionalUser = userRepository.findByEmail(toEmail);

        if (optionalUser.isPresent()) {
            Users user = optionalUser.get();

            String encodedPassword = passwordEncoder.encode(password);

            user.setPassword(encodedPassword);
            userRepository.save(user);
        } else {
            throw new UserHandler(ErrorStatus._USER_NOT_FOUND);
        }
    }
}