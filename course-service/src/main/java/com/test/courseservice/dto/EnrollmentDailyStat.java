package com.test.courseservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDailyStat {
    /** Format ISO yyyy-MM-dd. */
    private String date;
    private BigDecimal revenue;
    private long count;
}