package com.test.trainingservice.dto;

import com.test.trainingservice.entity.TrainingEnrollmentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Student-facing shape — used by GET /api/training-enrollments/my. Deliberately leaner than
 * TrainingEnrollmentResponse (no studentId/createdAt/updatedAt) and enriched with Training
 * display fields (title/image/dates) fetched at read time — never duplicated into the
 * TrainingEnrollment document itself.
 */
@Data
@Builder
public class StudentEnrollmentResponse {

    private String enrollmentId;
    private String trainingId;
    private String trainingTitle;
    private String trainingImage;
    private BigDecimal amountAtEnrollment;
    private TrainingEnrollmentStatus status;
    private LocalDateTime activatedAt;
    private LocalDate startDate;
    private LocalDate endDate;
}