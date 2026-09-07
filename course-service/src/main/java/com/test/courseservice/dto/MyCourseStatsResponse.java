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
public class MyCourseStatsResponse {
    private BigDecimal totalRevenue;
    private long totalCount;
    private List<TopCourseResponse> courses; // réutilise TopCourseResponse existant : courseId, courseTitle, enrollmentCount, revenue
}