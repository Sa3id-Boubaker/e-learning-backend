package com.test.courseservice.exception;

public class InvalidEnrollmentStatusException extends RuntimeException {
    public InvalidEnrollmentStatusException(String message) {
        super(message);
    }
}