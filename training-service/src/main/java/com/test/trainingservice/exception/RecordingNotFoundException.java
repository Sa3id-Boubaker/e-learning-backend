package com.test.trainingservice.exception;

public class RecordingNotFoundException extends RuntimeException {
    public RecordingNotFoundException(String message) {
        super(message);
    }
}