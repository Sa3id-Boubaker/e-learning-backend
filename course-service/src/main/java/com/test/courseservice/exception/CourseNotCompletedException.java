package com.test.courseservice.exception;
public class CourseNotCompletedException extends RuntimeException {
    public CourseNotCompletedException(String message) { super(message); }
}