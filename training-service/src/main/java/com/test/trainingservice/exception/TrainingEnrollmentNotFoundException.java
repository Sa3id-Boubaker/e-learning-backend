package com.test.trainingservice.exception;

public class TrainingEnrollmentNotFoundException extends RuntimeException {
    public TrainingEnrollmentNotFoundException(String message) {
        super(message);
    }
}