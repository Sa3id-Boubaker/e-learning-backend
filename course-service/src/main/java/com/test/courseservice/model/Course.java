package com.test.courseservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "courses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    private String id;

    private String title;
    private String description;
    private String category;
    private BigDecimal price;
    private BigDecimal discountPercentage;
    private String image;
    private String imagePublicId;

    /** Référence vers l'id MongoDB de l'utilisateur formateur, côté User Service — jamais un objet User complet. */
    private String instructorId;

    private Boolean published;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}