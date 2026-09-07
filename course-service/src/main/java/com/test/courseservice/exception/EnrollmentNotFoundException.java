package com.test.courseservice.exception;
public class EnrollmentNotFoundException extends RuntimeException {
    public EnrollmentNotFoundException(String message) { super(message); }
}