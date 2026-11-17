package com.ubb.genai.organizations.api;

public class OrganizationProblem extends RuntimeException {

    private final int status;
    private final String code;

    public OrganizationProblem(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }
}
