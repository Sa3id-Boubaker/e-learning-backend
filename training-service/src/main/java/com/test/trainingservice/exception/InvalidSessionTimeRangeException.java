package com.test.trainingservice.exception;

public class InvalidSessionTimeRangeException extends RuntimeException {
    public InvalidSessionTimeRangeException(String message) {
        super(message);
    }
}