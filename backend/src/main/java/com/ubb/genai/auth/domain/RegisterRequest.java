package com.ubb.genai.auth.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
public class RegisterRequest {
    @NotBlank @Email private String email;
    @NotBlank
    private String password;
    @NotBlank
    private String username;
    @NotBlank
    private String displayName;
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
}
