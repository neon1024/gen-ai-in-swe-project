package com.ubb.genai.eventlifecycle.shared.web;

import com.ubb.genai.eventlifecycle.shared.model.IllegalEventTransitionException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/** Maps lifecycle failures to RFC 9457 problem responses with an extra {@code code} property. */
@RestControllerAdvice(basePackages = "com.ubb.genai.eventlifecycle")
public class EventLifecycleExceptionHandler {

    @ExceptionHandler(LifecycleProblem.class)
    ProblemDetail handle(LifecycleProblem e) {
        return problem(e.status(), e.code(), e.getMessage());
    }

    @ExceptionHandler(IllegalEventTransitionException.class)
    ProblemDetail handle(IllegalEventTransitionException e) {
        return problem(HttpStatus.CONFLICT, e.code(), e.getMessage());
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class,
        HandlerMethodValidationException.class
    })
    ProblemDetail handleValidation(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentModification(ObjectOptimisticLockingFailureException e) {
        return problem(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "The resource was modified concurrently");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        return problem;
    }
}
