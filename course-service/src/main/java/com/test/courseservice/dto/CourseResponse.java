package com.test.courseservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {
    private String id;
    private String title;
    private String description;
    private String category;
    private BigDecimal price;
    private BigDecimal discountPercentage;
    private BigDecimal finalPrice;
    private String image;
    private String instructorId;

    // Renseignés uniquement pour ADMIN — restent null pour FORMATEUR/ETUDIANT
    private String instructorFirstName;
    private String instructorLastName;
    private String instructorEmail;

    private Boolean published;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}