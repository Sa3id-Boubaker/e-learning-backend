package com.test.trainingservice.dto;

import com.test.trainingservice.entity.TrainingStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * multipart/form-data — l'image est optionnelle à la création.
 * Ne comporte volontairement ni id, ni imagePublicId, ni finalPrice, ni category, ni createdBy :
 * createdBy est toujours déduit du JWT de l'appelant, jamais accepté depuis le frontend.
 *
 * instructorId est optionnel ici et sa signification dépend du rôle de l'appelant (voir
 * TrainingService.create()) :
 *  - FORMATEUR : ce champ est ignoré s'il est fourni — l'instructeur est toujours l'appelant.
 *  - ADMIN : ce champ est OBLIGATOIRE (vérifié en service, pas via bean validation) et doit
 *    désigner un utilisateur réellement FORMATEUR, vérifié auprès de USER-SERVICE.
 */
@Data
public class TrainingCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must not be negative")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "Discount percentage must not be negative")
    @DecimalMax(value = "100.0", message = "Discount percentage must not exceed 100")
    private BigDecimal discountPercentage;

    @NotNull(message = "Start date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    /** Optionnel — DRAFT par défaut si absent. */
    private TrainingStatus status;

    private MultipartFile image;

    /** Requis pour un ADMIN, ignoré pour un FORMATEUR — voir TrainingService.create(). */
    private String instructorId;
}