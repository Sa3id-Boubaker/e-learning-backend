package com.test.trainingservice.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Fetched by the frontend right before showing the delete confirmation modal, so the admin sees
 * exactly what a cascade delete will wipe out before confirming — never a silent mass-delete.
 */
@Data
@Builder
public class TrainingDeletionImpactResponse {
    private String trainingId;
    private long liveSessionCount;
    private long recordingCount;
    private long totalEnrollmentCount;
    private long activeEnrollmentCount;
}