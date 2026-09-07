package com.test.courseservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswerRequest {

    @NotBlank(message = "Question id is required")
    private String questionId;

    @NotBlank(message = "Selected option id is required")
    private String selectedOptionId;
}