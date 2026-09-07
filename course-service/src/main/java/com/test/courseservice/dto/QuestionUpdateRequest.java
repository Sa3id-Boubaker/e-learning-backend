package com.test.courseservice.dto;

import jakarta.validation.Valid;
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
public class QuestionUpdateRequest {

    private String questionText;

    @Positive(message = "Order must be a positive number")
    private Integer order;

    @Valid
    private List<OptionRequest> options;
}