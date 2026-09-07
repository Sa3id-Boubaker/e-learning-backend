package com.test.courseservice.exception;
public class CertificateNotFoundException extends RuntimeException {
    public CertificateNotFoundException(String message) { super(message); }
}