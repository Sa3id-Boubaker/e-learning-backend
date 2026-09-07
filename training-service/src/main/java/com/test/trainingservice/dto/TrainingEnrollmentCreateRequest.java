package com.test.trainingservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * The ADMIN provides only studentId + trainingId. amountAtEnrollment, status, enrolledAt and
 * activatedAt are always computed/controlled by the backend — never accepted from the frontend.
 */
@Data
public class TrainingEnrollmentCreateRequest {

    @NotBlank(message = "Student id is required")
    private String studentId;

    @NotBlank(message = "Training id is required")
    private String trainingId;
}