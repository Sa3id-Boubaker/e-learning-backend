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
public class TopCourseResponse {
    private String courseId;
    private String courseTitle;
    private long enrollmentCount;
    private BigDecimal revenue;
}