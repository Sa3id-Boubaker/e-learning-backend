package com.test.courseservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @DecimalMin(value = "0", message = "Passing score must be at least 0")
    @DecimalMax(value = "100", message = "Passing score must not exceed 100")
    private Integer passingScore;
}