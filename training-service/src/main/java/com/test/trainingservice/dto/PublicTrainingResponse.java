package com.test.trainingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Anonymous-safe — no instructorId/createdBy, no description, no status/createdAt/updatedAt.
 * For the public landing page only. Never add owner-identifying fields to this DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicTrainingResponse {
    private String id;
    private String title;
    private String image;
    private BigDecimal price;
    private BigDecimal finalPrice;
    private LocalDate startDate;
}
