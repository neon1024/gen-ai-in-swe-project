package com.ubb.genai.auth.service;

import com.ubb.genai.auth.domain.exception.EmailDeliveryException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Sends verification emails through Resend when an API key is set, otherwise through SMTP when
 * spring.mail.host is configured (Mailpit locally). With neither, nothing is sent.
 */
@Service
public class EmailService {
    private static final String SUBJECT = "Verify your email address";

    private final RestClient resendRestClient;
    private final ObjectProvider<JavaMailSender> smtpSender;
    private final String apiKey;
    private final String fromEmail;
    private final String baseUrl;

    public EmailService(
            RestClient resendRestClient,
            ObjectProvider<JavaMailSender> smtpSender,
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from-email:}") String fromEmail,
            @Value("${app.frontend-url:http://localhost:5173}") String baseUrl) {
        this.resendRestClient = resendRestClient;
        this.smtpSender = smtpSender;
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.baseUrl = baseUrl;
    }

    public void sendVerificationEmail(String recipient, String token) {
        if (fromEmail.isBlank()) {
            return;
        }
        String verificationUrl = baseUrl + "/verify-email?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String emailBody = """
                <h2>Welcome!</h2>
                <p>Please verify your email by clicking the link below:</p>
                <a href="%s">Verify email</a>
                """.formatted(verificationUrl);

        if (!apiKey.isBlank()) {
            sendWithResend(recipient, emailBody);
            return;
        }
        JavaMailSender smtp = smtpSender.getIfAvailable();
        if (smtp != null) {
            sendWithSmtp(smtp, recipient, emailBody);
        }
    }

    private void sendWithResend(String recipient, String emailBody) {
        Map<String, Object> requestBody = Map.of(
                "from", fromEmail,
                "to", List.of(recipient),
                "subject", SUBJECT,
                "html", emailBody
        );
        try {
            resendRestClient.post()
                    .uri("/emails")
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new EmailDeliveryException(exception);
        }
    }

    private void sendWithSmtp(JavaMailSender smtp, String recipient, String emailBody) {
        try {
            MimeMessage message = smtp.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(recipient);
            helper.setSubject(SUBJECT);
            helper.setText(emailBody, true);
            smtp.send(message);
        } catch (MessagingException | MailException exception) {
            throw new EmailDeliveryException(exception);
        }
    }
}
