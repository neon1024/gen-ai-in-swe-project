package com.ubb.genai.auth.domain.exception;

public class UsernameAlreadyExistsException extends Exception {
    public UsernameAlreadyExistsException() {
        super("Username already exists!");
    }
}
