package com.test.courseservice.exception;
public class QuizAlreadyExistsException extends RuntimeException {
    public QuizAlreadyExistsException(String message) { super(message); }
}