package com.ubb.genai.auth.domain.exception;

public class AccountNotVerifiedException extends Exception {
    public AccountNotVerifiedException() {
        super("Account is not verified!");
    }
}
