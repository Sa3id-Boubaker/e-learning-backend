package com.test.trainingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopTrainingResponse {
    private String trainingId;
    private String trainingTitle;
    private long enrollmentCount;
    private BigDecimal revenue;
}