package com.test.trainingservice.exception;

public class InvalidTrainingDateRangeException extends RuntimeException {
    public InvalidTrainingDateRangeException(String message) {
        super(message);
    }
}