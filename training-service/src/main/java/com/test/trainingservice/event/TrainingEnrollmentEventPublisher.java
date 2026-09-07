package com.test.trainingservice.event;

import com.test.trainingservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mirrors course-service's EnrollmentEventPublisher exactly: fresh UUID eventId per publish
 * call, same exchange, fire-and-forget (a RabbitMQ outage must never fail an activation that
 * has already been persisted — see the try/catch below).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TrainingEnrollmentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishTrainingEnrollmentActivated(String enrollmentId, String studentId, String trainingId,
                                                   String trainingTitle, LocalDateTime activatedAt) {

        TrainingEnrollmentActivatedEvent event = new TrainingEnrollmentActivatedEvent(
                UUID.randomUUID().toString(), enrollmentId, studentId, trainingId, trainingTitle, activatedAt);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_TRAINING_ENROLLMENT_ACTIVATED, event);
        } catch (AmqpException e) {
            // The TrainingEnrollment itself has ALREADY been saved successfully at this point —
            // a RabbitMQ outage must never fail the activation from the admin's point of view.
            // Same fire-and-forget contract as CourseService's EnrollmentEventPublisher (no
            // outbox pattern in this architecture yet — a dropped message here is silently
            // lost, matching the existing, accepted Course behavior).
            log.error("Failed to publish TrainingEnrollmentActivatedEvent for studentId={}, trainingId={}: {}",
                    studentId, trainingId, e.getMessage());
        }
    }
}