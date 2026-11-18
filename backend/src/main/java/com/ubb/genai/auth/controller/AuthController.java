package com.ubb.genai.auth.controller;

import com.ubb.genai.auth.domain.*;
import com.ubb.genai.auth.service.AuthService;
import com.ubb.genai.auth.domain.exception.EmailDeliveryException;
import com.ubb.genai.auth.domain.exception.UsernameAlreadyExistsException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        try {
            authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (UsernameAlreadyExistsException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(java.util.Map.of("code", "USERNAME_ALREADY_EXISTS", "detail", "That username is already in use."));
        }
    }

    @PostMapping("/verification/confirm")
    public ResponseEntity<Void> confirmEmailVerification(
            @RequestBody Map<String, String> request
    ) {
        authService.verifyEmail(request.get("token"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verification/resend")
    public ResponseEntity<Void> resendVerificationEmail(
            @RequestBody @Valid EmailRequest request) {
        authService.resendVerificationEmail(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        LoginResponse response;
        try {
            response = authService.login(request);
        } catch (BadCredentialsException e) {
            return loginRefused("INVALID_CREDENTIALS", "Invalid email or password.");
        }
        if (response.status() == UserStatus.PENDING) {
            return loginRefused("ACCOUNT_NOT_VERIFIED", "Verify your email address before signing in.");
        }
        if (response.status() == UserStatus.DEACTIVATED) {
            return loginRefused("ACCOUNT_DEACTIVATED", "This account is deactivated.");
        }

        // A new session id at login, so an id planted before login cannot be reused (session fixation).
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                response.userId().toString(), null, java.util.List.of());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, httpRequest, httpResponse);
        return ResponseEntity.ok(response);
    }

    // Without this, the exception reaches the protected /error page and the client sees a misleading 401.
    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<Map<String, String>> emailDeliveryFailed() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "code", "EMAIL_DELIVERY_FAILED",
                "detail", "We couldn't send the verification email. Try again later."));
    }

    private static ResponseEntity<Map<String, String>> loginRefused(String code, String detail) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("code", code, "detail", detail));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
