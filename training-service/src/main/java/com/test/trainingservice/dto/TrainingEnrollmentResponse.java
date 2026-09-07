package com.test.trainingservice.dto;

import com.test.trainingservice.entity.TrainingEnrollmentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TrainingEnrollmentResponse {

    private String id;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String trainingId;
    private String trainingTitle;
    private BigDecimal amountAtEnrollment;
    private TrainingEnrollmentStatus status;
    private LocalDateTime enrolledAt;
    private LocalDateTime activatedAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}