package com.test.forumservice.exception;

public class ForumAccessDeniedException extends RuntimeException {
    public ForumAccessDeniedException(String message) {
        super(message);
    }
}