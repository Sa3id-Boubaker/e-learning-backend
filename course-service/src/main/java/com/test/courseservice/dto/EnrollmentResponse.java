package com.test.courseservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.test.courseservice.model.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {
    private String id;
    private String studentId;
    private String courseId;
    private String courseTitle;
    private BigDecimal amountAtEnrollment;
    private EnrollmentStatus enrollmentStatus;
    private String studentName;
    private String studentEmail;
    private LocalDateTime enrolledAt;
    private LocalDateTime activatedAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String activatedBy;
}