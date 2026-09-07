package com.test.trainingservice.exception;

public class SessionNotCompletedException extends RuntimeException {
    public SessionNotCompletedException(String message) {
        super(message);
    }
}