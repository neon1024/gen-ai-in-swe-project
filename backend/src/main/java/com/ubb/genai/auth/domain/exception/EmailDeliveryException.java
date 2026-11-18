package com.ubb.genai.auth.domain.exception;

/** The verification email could not be handed to the mail provider; the request is rolled back. */
public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(Throwable cause) {
        super("Verification email could not be sent", cause);
    }
}
