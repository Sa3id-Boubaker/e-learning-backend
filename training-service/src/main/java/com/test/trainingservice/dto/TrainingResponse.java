package com.test.trainingservice.dto;

import com.test.trainingservice.entity.TrainingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** imagePublicId n'est jamais exposé — c'est un détail technique interne Cloudinary. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingResponse {

    private String id;
    private String title;
    private String description;

    private BigDecimal price;
    private BigDecimal discountPercentage;
    private BigDecimal finalPrice;

    private String image;

    /** Null for trainings created before this field existed and not yet backfilled — see the migration report. */
    private String createdBy;
    private String instructorId;
    private TrainingStatus status;

    private LocalDate startDate;
    private LocalDate endDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}