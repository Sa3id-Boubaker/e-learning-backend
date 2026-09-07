package com.test.courseservice.exception;
public class InvalidQuizSubmissionException extends RuntimeException {
    public InvalidQuizSubmissionException(String message) { super(message); }
}