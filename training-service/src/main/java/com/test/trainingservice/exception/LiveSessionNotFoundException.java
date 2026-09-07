package com.test.trainingservice.exception;

public class LiveSessionNotFoundException extends RuntimeException {
    public LiveSessionNotFoundException(String message) {
        super(message);
    }
}