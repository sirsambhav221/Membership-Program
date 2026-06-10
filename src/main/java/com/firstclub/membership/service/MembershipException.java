package com.firstclub.membership.service;

public final class MembershipException extends RuntimeException {
    private final int statusCode;

    public MembershipException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
