package com.test.courseservice.exception;

public class UnauthorizedProgressAccessException extends RuntimeException {
    public UnauthorizedProgressAccessException(String message) {
        super(message);
    }
}