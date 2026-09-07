package com.test.trainingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyTrainingStatsResponse {
    private BigDecimal totalRevenue;
    private long totalCount;
    private List<TopTrainingResponse> trainings;
}