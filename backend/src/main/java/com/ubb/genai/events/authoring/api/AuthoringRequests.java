package com.ubb.genai.events.authoring.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.ubb.genai.events.authoring.application.AuthoringException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.std.StdDeserializer;

public final class AuthoringRequests {
  private AuthoringRequests() {}

  public record CreateEvent(
      @JsonDeserialize(using = StrictTextDeserializer.class) String title,
      @JsonDeserialize(using = StrictTextDeserializer.class) String description,
      @JsonDeserialize(using = StrictTextDeserializer.class) String category,
      @JsonDeserialize(using = StrictTextDeserializer.class) String organizationRef) {
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
      throw new AuthoringException(400, "INVALID_INPUT", "Invalid request");
    }
  }

  /** Refuses Jackson scalar coercion only on this feature's request text fields. */
  public static final class StrictTextDeserializer extends StdDeserializer<String> {
    public StrictTextDeserializer() {
      super(String.class);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) {
      if (parser.currentToken() != JsonToken.VALUE_STRING) {
        return context.reportInputMismatch(String.class, "Expected a JSON string");
      }
      return parser.getString();
    }
  }
}
