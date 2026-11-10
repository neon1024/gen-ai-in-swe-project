package com.ubb.genai.events.authoring.api;

import com.ubb.genai.events.authoring.application.AuthoringException;
import jakarta.persistence.OptimisticLockException;
import java.util.List;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(basePackageClasses = EventAuthoringController.class)
public class AuthoringExceptionHandler {
  public record ErrorView(
      String code, String message, List<AuthoringException.Violation> violations) {}

  @ExceptionHandler(AuthoringException.class)
  ResponseEntity<ErrorView> authoring(AuthoringException ex) {
    return ResponseEntity.status(ex.status())
        .body(new ErrorView(ex.code(), ex.getMessage(), ex.violations()));
  }

  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<ErrorView> invalid(Exception ex) {
    return ResponseEntity.badRequest()
        .body(new ErrorView("INVALID_INPUT", "Invalid request", List.of()));
  }

  @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
  ResponseEntity<ErrorView> conflict(Exception ex) {
    return ResponseEntity.status(409)
        .body(new ErrorView("VERSION_CONFLICT", "Event changed; reload and retry", List.of()));
  }
}
