package com.test.forumservice.exception;

public class InvalidForumReferenceException extends RuntimeException {
    public InvalidForumReferenceException(String message) {
        super(message);
    }
}