package com.test.trainingservice.exception;

public class TrainingAccessDeniedException extends RuntimeException {
    public TrainingAccessDeniedException(String message) {
        super(message);
    }
}