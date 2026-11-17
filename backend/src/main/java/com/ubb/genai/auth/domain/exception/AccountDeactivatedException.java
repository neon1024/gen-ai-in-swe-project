package com.ubb.genai.auth.domain.exception;

public class AccountDeactivatedException extends Exception {
    public AccountDeactivatedException() {
        super("Account is not active!");
    }
}
