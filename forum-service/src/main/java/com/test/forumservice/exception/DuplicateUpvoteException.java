package com.test.forumservice.exception;

public class DuplicateUpvoteException extends RuntimeException {
    public DuplicateUpvoteException(String message) {
        super(message);
    }
}