package com.test.trainingservice.exception;

public class InvalidCalendarStatusException extends RuntimeException {
    public InvalidCalendarStatusException(String message) {
        super(message);
    }
}