package com.ubb.genai.auth.controller;

import com.ubb.genai.auth.domain.*;
import com.ubb.genai.auth.domain.exception.AccountDeactivatedException;
import com.ubb.genai.auth.domain.exception.AccountNotVerifiedException;
import com.ubb.genai.auth.service.AuthService;
import com.ubb.genai.auth.domain.exception.UsernameAlreadyExistsException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        try {
            LoginResponse response = authService.login(request);

            boolean isAccountVerified = response.status() != UserStatus.PENDING;

            if (!isAccountVerified) {
                throw new AccountNotVerifiedException();
            }

            boolean isAccountDeactivated = response.status() == UserStatus.DEACTIVATED;

            if (isAccountDeactivated) {
                throw new AccountDeactivatedException();
            }

            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    response.userId().toString(), null, java.util.List.of());
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            new HttpSessionSecurityContextRepository().saveContext(context, httpRequest, httpResponse);
            return ResponseEntity.ok(response);
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
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
