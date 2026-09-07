package com.test.courseservice.exception;
public class QuizNotFoundException extends RuntimeException {
    public QuizNotFoundException(String message) { super(message); }
}