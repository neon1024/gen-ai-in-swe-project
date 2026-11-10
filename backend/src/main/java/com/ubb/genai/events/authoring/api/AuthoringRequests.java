package com.ubb.genai.events.authoring.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.ubb.genai.events.authoring.application.AuthoringException;

public final class AuthoringRequests {
  private AuthoringRequests() {}

  public record CreateEvent(
      String title, String description, String category, String organizationRef) {
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
      throw new AuthoringException(400, "INVALID_INPUT", "Invalid request");
    }
  }
}
