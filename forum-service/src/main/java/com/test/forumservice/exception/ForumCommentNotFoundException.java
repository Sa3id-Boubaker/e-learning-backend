package com.test.forumservice.exception;

public class ForumCommentNotFoundException extends RuntimeException {
    public ForumCommentNotFoundException(String message) {
        super(message);
    }
}