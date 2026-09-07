package com.test.courseservice.exception;

public class QuizAlreadyPassedException extends RuntimeException {
    public QuizAlreadyPassedException(String message) {
        super(message);
    }
}