package com.test.courseservice.dto;

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
public class EnrollmentStatsResponse {
    /** Somme d'amountAtEnrollment sur TOUS les enrollments (y compris REVOKED — une révocation
     * est une correction d'accès, pas un remboursement, voir décision équivalente documentée
     * côté training-service). */
    private BigDecimal totalRevenue;
    private long totalCount;
    /** 7 derniers jours calendaires, du plus ancien au plus récent, jours sans activité inclus à zéro. */
    private List<EnrollmentDailyStat> dailyStats;
}