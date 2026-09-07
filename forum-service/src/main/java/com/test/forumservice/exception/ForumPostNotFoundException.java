package com.test.forumservice.exception;

public class ForumPostNotFoundException extends RuntimeException {
    public ForumPostNotFoundException(String message) {
        super(message);
    }
}