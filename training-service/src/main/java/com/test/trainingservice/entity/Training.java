package com.test.trainingservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Programme de formation professionnelle en ligne (sessions live, calendrier, enregistrements).
 * Entièrement indépendant de Course/Chapter/Video du Course Service — ne réutilise et
 * ne modifie aucune de ses collections.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "trainings")
public class Training {

    @Id
    private String id;

    private String title;
    private String description;

    private BigDecimal price;
    private BigDecimal discountPercentage;

    /** URL Cloudinary publique de l'image. */
    private String image;

    /** Identifiant technique Cloudinary — jamais exposé dans les réponses. */
    private String imagePublicId;

    /**
     * L'utilisateur (ADMIN ou FORMATEUR) qui a créé cette fiche training. Purement informatif/
     * historique — jamais utilisé pour une décision d'autorisation. Null sur les documents créés
     * avant l'introduction de ce champ, tant qu'un backfill Mongo ne l'a pas renseigné à partir de
     * l'ancien instructorId (voir le rapport de migration).
     */
    private String createdBy;

    /**
     * Le FORMATEUR assigné à cette formation — toujours vérifié comme ayant le rôle FORMATEUR
     * via USER-SERVICE au moment où il est défini ou réassigné (voir TrainingService). C'est le
     * SEUL champ utilisé par tous les contrôles d'accès FORMATEUR (LiveSession, Recording,
     * Calendar, TrainingEnrollment.getAccess/hasTrainingContentAccess) — createdBy n'intervient
     * jamais dans ces décisions. Jamais accepté tel quel depuis le frontend sans validation de
     * rôle : soit déduit du JWT du créateur (FORMATEUR créant sa propre formation), soit fourni
     * explicitement par un ADMIN et vérifié.
     */
    private String instructorId;

    private TrainingStatus status;

    private LocalDate startDate;
    private LocalDate endDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * DRAFT and CANCELLED are always manual decisions (never inferred). Once a training has been
     * published (any other stored value), PUBLISHED/IN_PROGRESS/COMPLETED are recomputed from
     * startDate/endDate vs. today every time — an instructor never has to remember to flip a
     * training from "in progress" to "completed" by hand. The stored `status` field itself is
     * left untouched by this method — nothing here writes back to the database, and the DRAFT →
     * PUBLISHED transition (the one real business decision) still happens exactly as before,
     * through a manual PUT.
     */
    public TrainingStatus effectiveStatus(LocalDate today) {
        if (status == TrainingStatus.DRAFT || status == TrainingStatus.CANCELLED) {
            return status;
        }
        if (today.isBefore(startDate)) {
            return TrainingStatus.PUBLISHED;
        }
        if (!today.isAfter(endDate)) {
            return TrainingStatus.IN_PROGRESS;
        }
        return TrainingStatus.COMPLETED;
    }

    /**
     * The access-control gate used everywhere a student's visibility depends on "is this
     * training published" — DRAFT and CANCELLED are the only states that hide a training from
     * an ETUDIANT. A training that has since become IN_PROGRESS or COMPLETED stays visible (a
     * student who took it should still be able to review it), so this checks the stored value
     * directly rather than effectiveStatus(), and deliberately does NOT require status to be
     * exactly PUBLISHED.
     */
    public boolean isPublishedOrBeyond() {
        return status != TrainingStatus.DRAFT && status != TrainingStatus.CANCELLED;
    }
}