package com.ubb.genai.events.authoring.application;

import java.util.List;

public class AuthoringException extends RuntimeException {
  public record Violation(String path, String code, String message) {}

  private final int status;
  private final String code;
  private final List<Violation> violations;

  public AuthoringException(int status, String code, String message) {
    this(status, code, message, List.of());
  }

  public AuthoringException(int status, String code, String message, List<Violation> violations) {
    super(message);
    this.status = status;
    this.code = code;
    this.violations = List.copyOf(violations);
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }

  public List<Violation> violations() {
    return violations;
  }

  public static AuthoringException unavailable() {
    return new AuthoringException(
        503, "DEPENDENCY_UNAVAILABLE", "Authoring dependency unavailable");
  }
}
