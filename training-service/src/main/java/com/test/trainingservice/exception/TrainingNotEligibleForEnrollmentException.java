package com.test.trainingservice.exception;

public class TrainingNotEligibleForEnrollmentException extends RuntimeException {
    public TrainingNotEligibleForEnrollmentException(String message) {
        super(message);
    }
}