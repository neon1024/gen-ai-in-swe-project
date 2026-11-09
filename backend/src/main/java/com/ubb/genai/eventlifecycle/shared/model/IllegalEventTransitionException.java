package com.ubb.genai.eventlifecycle.shared.model;

/**
 * Provisional: owned by the event-authoring share (jose03pedro); replaced at reconciliation.
 */
public class IllegalEventTransitionException extends RuntimeException {

    private final String code;

    public IllegalEventTransitionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
