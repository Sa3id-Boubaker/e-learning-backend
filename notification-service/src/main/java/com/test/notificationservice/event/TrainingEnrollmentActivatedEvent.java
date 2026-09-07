package com.test.notificationservice.event;

import java.time.LocalDateTime;

/**
 * Consumer-side copy of TRAINING-SERVICE's event of the same name — hand-duplicated exactly
 * like EnrollmentActivatedEvent already is between course-service and notification-service.
 */
public record TrainingEnrollmentActivatedEvent(
        String eventId,
        String enrollmentId,
        String studentId,
        String trainingId,
        String trainingTitle,
        LocalDateTime activatedAt
) {}