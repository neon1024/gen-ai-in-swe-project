package com.ubb.genai.events.authoring.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.ubb.genai.events.authoring.application.AuthoringException;
import io.swagger.v3.oas.annotations.media.Schema;
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

  public record ActivityWrite(
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
      @JsonDeserialize(using = StrictTextDeserializer.class) String title,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
      @JsonDeserialize(using = StrictTextDeserializer.class) String description,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
      @JsonDeserialize(using = StrictTextDeserializer.class) String start,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
      @JsonDeserialize(using = StrictTextDeserializer.class) String end,
      @JsonDeserialize(using = StrictTextDeserializer.class) String location,
      @JsonDeserialize(using = StrictTextDeserializer.class) String onlineLink,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
      @JsonDeserialize(using = StrictIntegerDeserializer.class) Integer capacity,
      @Schema(format = "date-time")
      @JsonDeserialize(using = StrictTextDeserializer.class) String registrationOpen,
      @Schema(format = "date-time")
      @JsonDeserialize(using = StrictTextDeserializer.class) String registrationClose,
      @JsonDeserialize(using = StrictTextDeserializer.class) String visibility,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
      @JsonDeserialize(using = StrictLongDeserializer.class) Long expectedVersion) {
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
      throw new AuthoringException(400, "INVALID_INPUT", "Invalid request");
    }
  }

  public record AgendaWrite(
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
      @JsonDeserialize(using = StrictTextDeserializer.class) String time,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
      @JsonDeserialize(using = StrictTextDeserializer.class) String title,
      @JsonDeserialize(using = StrictTextDeserializer.class) String speaker,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
      @JsonDeserialize(using = StrictLongDeserializer.class) Long expectedVersion) {
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
      throw new AuthoringException(400, "INVALID_INPUT", "Invalid request");
    }
  }

  public static final class StrictIntegerDeserializer extends StdDeserializer<Integer> {
    public StrictIntegerDeserializer() { super(Integer.class); }
    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) {
      if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT)
        return context.reportInputMismatch(Integer.class, "Expected a JSON integer");
      return parser.getIntValue();
    }
  }

  public static final class StrictLongDeserializer extends StdDeserializer<Long> {
    public StrictLongDeserializer() { super(Long.class); }
    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context) {
      if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT)
        return context.reportInputMismatch(Long.class, "Expected a JSON integer");
      return parser.getLongValue();
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
