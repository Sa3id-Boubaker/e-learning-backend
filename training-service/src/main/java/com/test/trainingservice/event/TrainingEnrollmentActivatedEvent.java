package com.test.trainingservice.event;

import java.time.LocalDateTime;

/**
 * Published to RabbitMQ (exchange "omarise.events", routing key "training-enrollment.activated")
 * whenever a TrainingEnrollment transitions to ACTIVE. Hand-duplicated in notification-service
 * as its own copy of the same shape — mirrors exactly how EnrollmentActivatedEvent already
 * exists as two independent copies between course-service and notification-service.
 *
 * No JWT, password, payment, or bank information — deliberately minimal.
 */
public record TrainingEnrollmentActivatedEvent(
        String eventId,
        String enrollmentId,
        String studentId,
        String trainingId,
        String trainingTitle,
        LocalDateTime activatedAt
) {}