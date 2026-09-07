package com.test.trainingservice.exception;

public class InvalidEnrollmentStatusException extends RuntimeException {
    public InvalidEnrollmentStatusException(String message) {
        super(message);
    }
}