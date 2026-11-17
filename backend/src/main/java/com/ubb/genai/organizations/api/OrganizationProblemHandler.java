package com.ubb.genai.organizations.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrganizationProblemHandler {

    @ExceptionHandler(OrganizationProblem.class)
    public ResponseEntity<ProblemDetail> handle(OrganizationProblem problem) {
        HttpStatus status = HttpStatus.valueOf(problem.status());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, problem.getMessage());
        detail.setTitle(status.getReasonPhrase());
        detail.setProperty("code", problem.code());
        return ResponseEntity.status(status).body(detail);
    }
}
