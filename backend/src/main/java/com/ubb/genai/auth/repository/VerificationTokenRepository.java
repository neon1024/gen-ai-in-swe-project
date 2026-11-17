package com.ubb.genai.auth.repository;

import com.ubb.genai.auth.domain.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
}
