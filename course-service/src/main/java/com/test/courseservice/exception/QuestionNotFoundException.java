package com.test.courseservice.exception;
public class QuestionNotFoundException extends RuntimeException {
    public QuestionNotFoundException(String message) { super(message); }
}