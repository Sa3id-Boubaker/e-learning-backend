package com.test.forumservice.exception;

public class InvalidForumPostTypeException extends RuntimeException {
    public InvalidForumPostTypeException(String message) {
        super(message);
    }
}