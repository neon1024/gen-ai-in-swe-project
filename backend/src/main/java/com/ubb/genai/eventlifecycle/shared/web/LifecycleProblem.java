package com.ubb.genai.eventlifecycle.shared.web;

import org.springframework.http.HttpStatus;

/** Business error carrying an HTTP status and a machine-readable code. */
public class LifecycleProblem extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    LifecycleProblem(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static LifecycleProblem notFound(String code) {
        return new LifecycleProblem(HttpStatus.NOT_FOUND, code, "Resource not found");
    }

    public static LifecycleProblem forbidden(String code) {
        return new LifecycleProblem(HttpStatus.FORBIDDEN, code, "Insufficient rights");
    }

    public static LifecycleProblem unauthenticated() {
        return new LifecycleProblem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication required");
    }

    public static LifecycleProblem conflict(String code, String message) {
        return new LifecycleProblem(HttpStatus.CONFLICT, code, message);
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
