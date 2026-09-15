package com.test.courseservice.service;

import com.test.courseservice.exception.CourseAccessDeniedException;
import com.test.courseservice.model.Course;
import com.test.courseservice.security.AuthenticatedUser;

/**
 * Règles d'accès en lecture partagées entre les modules internes de course-service
 * (cours, chapitres, vidéos). Extrait de CourseService, ChapterService et VideoService
 * pour éliminer la duplication de assertViewAccess, sans introduire de dépendance directe
 * entre ces trois classes — chacune dépend de cet utilitaire, pas des deux autres.
 */
public final class CourseAccessPolicy {

    private CourseAccessPolicy() {
        // classe utilitaire, non instanciable
    }

    /**
     * - ADMIN : accès illimité.
     * - FORMATEUR : doit être le propriétaire du cours.
     * - Autres rôles (ex. ETUDIANT) : le cours doit être publié. Le masquage de
     *   videoUrl/subtitleUrl pour un étudiant non inscrit est géré séparément par
     *   VideoService (voir VideoService#shouldMaskVideoUrl), pas ici.
     */
    public static void assertViewAccess(Course course, AuthenticatedUser currentUser) {
        switch (currentUser.role()) {
            case "ADMIN" -> {
                // Les administrateurs n'ont aucune restriction d'accès à vérifier
            }
            case "FORMATEUR" -> {
                if (!course.getInstructorId().equals(currentUser.userId())) {
                    throw new CourseAccessDeniedException("This course does not belong to you");
                }
            }
            default -> {
                if (!Boolean.TRUE.equals(course.getPublished())) {
                    throw new CourseAccessDeniedException("This course is not available");
                }
            }
        }
    }
}