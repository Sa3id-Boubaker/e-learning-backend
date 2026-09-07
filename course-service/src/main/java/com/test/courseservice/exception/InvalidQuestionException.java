package com.test.courseservice.exception;
public class InvalidQuestionException extends RuntimeException {
    public InvalidQuestionException(String message) { super(message); }
}