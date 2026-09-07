package com.test.trainingservice.dto;

import com.test.trainingservice.entity.TrainingStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Mise à jour partielle — tous les champs sont optionnels, seuls les champs non-null sont appliqués.
 * imagePublicId, finalPrice, category et createdBy ne figurent pas ici : non modifiables par ce
 * endpoint. L'image se gère via l'endpoint dédié POST /api/trainings/{id}/image.
 *
 * instructorId est optionnel et réservé à l'ADMIN (voir TrainingService.update()) : un FORMATEUR
 * qui l'inclut dans sa requête se voit refuser l'appel (TrainingAccessDeniedException), même s'il
 * est propriétaire de la formation. Quand un ADMIN le fournit, la nouvelle valeur est vérifiée
 * comme étant réellement un FORMATEUR auprès de USER-SERVICE avant d'être appliquée.
 */
@Data
public class TrainingUpdateRequest {

    private String title;
    private String description;

    @DecimalMin(value = "0.0", message = "Price must not be negative")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "Discount percentage must not be negative")
    @DecimalMax(value = "100.0", message = "Discount percentage must not exceed 100")
    private BigDecimal discountPercentage;

    private LocalDate startDate;
    private LocalDate endDate;

    private TrainingStatus status;

    /** ADMIN-only reassignment — see class javadoc. */
    private String instructorId;
}