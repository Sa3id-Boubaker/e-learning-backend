package com.test.courseservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionCreateRequest {

    @NotBlank(message = "Question text is required")
    private String questionText;

    @NotNull(message = "Order is required")
    @Positive(message = "Order must be a positive number")
    private Integer order;

    @NotEmpty(message = "Options are required")
    @Valid
    private List<OptionRequest> options;
}