package com.test.notificationservice.security;

public record AuthenticatedUser(String userId, String email, String role, String token) {}