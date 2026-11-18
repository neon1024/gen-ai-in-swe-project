package com.ubb.genai.auth.service;

import com.ubb.genai.auth.domain.exception.EmailDeliveryException;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmailServiceTest {
    private static final String FRONTEND = "http://localhost:5173";
    private static final String FROM = "no-reply@localhost";

    private final RestClient.Builder resendBuilder = RestClient.builder().baseUrl("https://api.resend.com");
    private final MockRestServiceServer resend = MockRestServiceServer.bindTo(resendBuilder).build();
    private final JavaMailSender smtp = mock(JavaMailSender.class);

    @Test
    @DisplayName("US-01 without a Resend key the verification email goes through SMTP with the link")
    void sendsThroughSmtpWhenNoResendKey() throws Exception {
        when(smtp.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        service("", smtpAvailable()).sendVerificationEmail("new.user@example.com", "tok en");

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(smtp).send(sent.capture());
        MimeMessage message = sent.getValue();
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("new.user@example.com");
        assertThat(message.getFrom()[0].toString()).isEqualTo(FROM);
        assertThat(message.getSubject()).isEqualTo("Verify your email address");
        assertThat(message.getContent().toString()).contains(FRONTEND + "/verify-email?token=tok+en");
    }

    @Test
    @DisplayName("US-01 with a Resend key the verification email goes through Resend, not SMTP")
    void prefersResendWhenKeyIsSet() {
        resend.expect(requestTo("https://api.resend.com/emails")).andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess());

        service("re_test", smtpAvailable()).sendVerificationEmail("new.user@example.com", "token");

        resend.verify();
        verifyNoInteractions(smtp);
    }

    @Test
    @DisplayName("US-01 without a Resend key or SMTP server nothing is sent")
    void skipsWhenNoTransportConfigured() {
        service("", smtpMissing()).sendVerificationEmail("new.user@example.com", "token");

        resend.verify();
    }

    @Test
    @DisplayName("US-01 a recipient refused by Resend is reported as a delivery failure")
    void resendRefusalBecomesDeliveryFailure() {
        resend.expect(requestTo("https://api.resend.com/emails")).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> service("re_test", smtpMissing()).sendVerificationEmail("other@example.com", "token"))
                .isInstanceOf(EmailDeliveryException.class);
    }

    @Test
    @DisplayName("US-01 an unreachable SMTP server is reported as a delivery failure")
    void smtpFailureBecomesDeliveryFailure() {
        when(smtp.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("connection refused")).when(smtp).send(any(MimeMessage.class));

        assertThatThrownBy(() -> service("", smtpAvailable()).sendVerificationEmail("new.user@example.com", "token"))
                .isInstanceOf(EmailDeliveryException.class);
    }

    private EmailService service(String apiKey, ObjectProvider<JavaMailSender> smtpProvider) {
        return new EmailService(resendBuilder.build(), smtpProvider, apiKey, FROM, FRONTEND);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<JavaMailSender> smtpAvailable() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(smtp);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<JavaMailSender> smtpMissing() {
        return mock(ObjectProvider.class);
    }
}
