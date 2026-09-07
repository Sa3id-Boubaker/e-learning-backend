package com.test.trainingservice.security;

/**
 * Représente l'utilisateur authentifié tel que porté par le JWT (userId, email, role).
 * Training Service ne possède pas sa propre base d'utilisateurs : il ne fait jamais
 * d'appel à Mongo pour reconstituer ce principal, il le déduit uniquement des claims du token.
 */
public record AuthenticatedUser(String userId, String email, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isFormateur() {
        return "FORMATEUR".equals(role);
    }

    public boolean isEtudiant() {
        return "ETUDIANT".equals(role);
    }
}