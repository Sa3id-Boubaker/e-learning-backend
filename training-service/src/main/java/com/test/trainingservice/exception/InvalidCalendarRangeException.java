package com.test.trainingservice.exception;

public class InvalidCalendarRangeException extends RuntimeException {
    public InvalidCalendarRangeException(String message) {
        super(message);
    }
}