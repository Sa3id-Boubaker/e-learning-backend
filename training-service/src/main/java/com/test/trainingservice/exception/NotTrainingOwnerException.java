package com.test.trainingservice.exception;

public class NotTrainingOwnerException extends RuntimeException {
    public NotTrainingOwnerException(String message) {
        super(message);
    }
}