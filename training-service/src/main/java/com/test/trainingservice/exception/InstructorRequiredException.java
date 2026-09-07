package com.test.trainingservice.exception;

public class InstructorRequiredException extends RuntimeException {
    public InstructorRequiredException(String message) {
        super(message);
    }
}