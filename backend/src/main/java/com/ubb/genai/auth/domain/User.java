package com.ubb.genai.auth.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String email;
    private String passwordHash;
    private String username;
    private String displayName;
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    private Boolean platformAdmin;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public User() {}

    public User(String email, String passwordHash, String username, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.username = username;
        this.displayName = displayName;
        this.status = UserStatus.PENDING;
        this.platformAdmin = false;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public UserStatus getStatus() { return status; }
    public Boolean getPlatformAdmin() { return platformAdmin; }
    public void setStatus(UserStatus status) { this.status = status; }
}
