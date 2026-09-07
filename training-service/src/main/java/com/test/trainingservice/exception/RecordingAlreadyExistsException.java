package com.test.trainingservice.exception;

public class RecordingAlreadyExistsException extends RuntimeException {
    public RecordingAlreadyExistsException(String message) {
        super(message);
    }
}