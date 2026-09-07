package com.test.trainingservice.dto;

import com.test.trainingservice.entity.TrainingEnrollmentStatus;
import lombok.Builder;
import lombok.Data;

/**
 * status is null for ADMIN and FORMATEUR (they don't hold a real TrainingEnrollment record —
 * their access comes from their role/ownership, not from being "enrolled") and only populated
 * with the real status for an ETUDIANT's actual enrollment.
 */
@Data
@Builder
public class TrainingAccessResponse {
    private String trainingId;
    private boolean enrolled;
    private TrainingEnrollmentStatus status;
}