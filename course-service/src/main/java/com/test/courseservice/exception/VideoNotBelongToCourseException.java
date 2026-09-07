package com.test.courseservice.exception;

public class VideoNotBelongToCourseException extends RuntimeException {
    public VideoNotBelongToCourseException(String message) {
        super(message);
    }
}