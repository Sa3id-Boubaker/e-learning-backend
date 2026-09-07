package com.test.courseservice.exception;

public class NotCourseOwnerException extends RuntimeException {
    public NotCourseOwnerException(String message) {
        super(message);
    }
}