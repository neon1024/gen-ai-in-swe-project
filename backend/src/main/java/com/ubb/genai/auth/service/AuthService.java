package com.ubb.genai.auth.service;

import com.ubb.genai.auth.domain.*;
import com.ubb.genai.auth.domain.RegisterRequest;
import com.ubb.genai.auth.domain.User;
import com.ubb.genai.auth.domain.VerificationToken;
import com.ubb.genai.auth.domain.exception.UsernameAlreadyExistsException;
import com.ubb.genai.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TokenService tokenService;

    @Transactional
    public void register(RegisterRequest request) throws UsernameAlreadyExistsException {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if(userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        String username = request.getUsername().trim();

        if(userRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameAlreadyExistsException();
        }

        String password = request.getPassword();
        String displayName = request.getDisplayName().trim();

        String passwordHash = passwordEncoder.encode(password);

        User user = new User(email, passwordHash, username, displayName);

        userRepository.save(user);

        String token = tokenService.generateVerificationToken();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setUser(user);
        verificationToken.setTokenHash(tokenService.hashToken(token));
        verificationToken.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

        tokenService.saveVerificationToken(verificationToken);

        emailService.sendVerificationEmail(email, token);
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = tokenService.findByToken(token);

        if (verificationToken == null) {
            throw new IllegalArgumentException("Invalid verification token");
        }

        if (verificationToken.getUsedAt() != null) {
            throw new IllegalArgumentException("Verification token has already been used");
        }

        if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setStatus(UserStatus.ACTIVE);

        verificationToken.setUsedAt(Instant.now());
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(user -> user.getStatus() == UserStatus.PENDING)
                .ifPresent(user -> {
                    tokenService.deleteVerificationTokensByUser(user.getId());

                    String token = tokenService.generateVerificationToken();

                    VerificationToken verificationToken = new VerificationToken();
                    verificationToken.setUser(user);
                    verificationToken.setTokenHash(tokenService.hashToken(token));
                    verificationToken.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

                    tokenService.saveVerificationToken(verificationToken);

                    emailService.sendVerificationEmail(normalizedEmail, token);
                });
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return new LoginResponse(user.getId(), user.getEmail(), user.getUsername(), user.getDisplayName(), user.getStatus());
    }
}
