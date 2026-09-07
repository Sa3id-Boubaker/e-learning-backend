package com.test.courseservice.security;

public record AuthenticatedUser(String userId, String email, String role, String token) {}