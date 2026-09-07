package com.test.trainingservice.exception;

public class InvalidStudentRoleException extends RuntimeException {
    public InvalidStudentRoleException(String message) {
        super(message);
    }
}