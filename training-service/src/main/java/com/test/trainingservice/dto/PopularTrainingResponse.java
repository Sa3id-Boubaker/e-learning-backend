package com.test.trainingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularTrainingResponse {
    private String trainingId;
    private String trainingTitle;
    private long enrollmentCount;
}