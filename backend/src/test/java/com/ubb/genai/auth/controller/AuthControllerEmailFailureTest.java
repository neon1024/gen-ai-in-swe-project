package com.ubb.genai.auth.controller;

import com.ubb.genai.auth.domain.RegisterRequest;
import com.ubb.genai.auth.domain.exception.EmailDeliveryException;
import com.ubb.genai.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerEmailFailureTest {
    private final AuthService authService = mock(AuthService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(authService)).build();

    @Test
    @DisplayName("US-01 a failed verification email on sign-up returns a clear 503, not 401")
    void registerReportsDeliveryFailure() throws Exception {
        doThrow(new EmailDeliveryException(new RuntimeException("refused"))).when(authService).register(any(RegisterRequest.class));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"new.user@example.com","password":"DemoPass123!","username":"newuser","displayName":"New User"}
                        """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EMAIL_DELIVERY_FAILED"))
                .andExpect(jsonPath("$.detail").value("We couldn't send the verification email. Try again later."));
    }

    @Test
    @DisplayName("US-01 a failed verification email on resend returns the same clear 503")
    void resendReportsDeliveryFailure() throws Exception {
        doThrow(new EmailDeliveryException(new RuntimeException("refused"))).when(authService).resendVerificationEmail(anyString());

        mvc.perform(post("/api/auth/verification/resend").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new.user@example.com\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EMAIL_DELIVERY_FAILED"));
    }
}
