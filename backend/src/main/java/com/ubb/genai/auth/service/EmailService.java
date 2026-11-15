package com.ubb.genai.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final RestClient resendRestClient;

    @Value("${resend.from-email}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String baseUrl;

    public void sendVerificationEmail(String recipient, String token) {
        String verificationUrl = baseUrl + "/verify-email?token=" + token;

        String emailBody = """
                <h2>Welcome!</h2>
                <p>Please verify your email by clicking the link below:</p>
                <a href="%s">Verify email</a>
                """.formatted(verificationUrl);

        Map<String, Object> requestBody = Map.of(
                "from", fromEmail,
                "to", List.of(recipient),
                "subject", "Verify your email address",
                "html", emailBody
        );

        resendRestClient.post()
                .uri("/emails")
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }
}
