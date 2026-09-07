package com.test.forumservice.security;

/**
 * Convention alignée sur COURSE-SERVICE (token embarqué), car FORUM-SERVICE
 * doit relayer le JWT vers USER-SERVICE, COURSE-SERVICE et TRAINING-SERVICE
 * sur la quasi-totalité des chemins d'écriture et de lecture enrichie.
 */
public record AuthenticatedUser(String userId, String email, String role, String token) {
}