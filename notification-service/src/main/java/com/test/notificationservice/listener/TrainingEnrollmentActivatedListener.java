package com.test.notificationservice.listener;

import com.test.notificationservice.config.RabbitMQConfig;
import com.test.notificationservice.event.TrainingEnrollmentActivatedEvent;
import com.test.notificationservice.model.Notification;
import com.test.notificationservice.model.NotificationType;
import com.test.notificationservice.repository.NotificationRepository;
import com.test.notificationservice.service.NotificationService;
import com.test.notificationservice.service.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mirrors EnrollmentActivatedListener exactly: existsByEventId check first, build+save
 * Notification, catch DuplicateKeyException as the race-condition fallback, push over the
 * existing SseEmitterRegistry. Adds a minimal required-fields guard before touching the
 * database, since this is a brand-new event type without production history yet.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TrainingEnrollmentActivatedListener {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_TRAINING_ENROLLMENT_ACTIVATED)
    public void handleTrainingEnrollmentActivated(TrainingEnrollmentActivatedEvent event) {

        if (event.eventId() == null || event.studentId() == null || event.trainingId() == null) {
            log.error("Discarding malformed TrainingEnrollmentActivatedEvent (missing required fields): {}", event);
            return;
        }

        if (notificationRepository.existsByEventId(event.eventId())) {
            log.info("Duplicate TrainingEnrollmentActivatedEvent ignored, eventId={}", event.eventId());
            return;
        }

        Notification notification = Notification.builder()
                .eventId(event.eventId())
                .userId(event.studentId())
                .type(NotificationType.TRAINING_ENROLLMENT_ACTIVATED)
                .title("Enrollment activated")
                .message("Your enrollment in " + event.trainingTitle() + " has been activated.")
                .referenceId(event.trainingId())
                .referenceType("TRAINING")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            Notification saved = notificationRepository.save(notification);
            log.info("Notification created for userId={}, eventId={}", event.studentId(), event.eventId());

            sseEmitterRegistry.sendToUser(event.studentId(), notificationService.toResponse(saved));

        } catch (DuplicateKeyException e) {
            log.info("Duplicate TrainingEnrollmentActivatedEvent ignored (race condition), eventId={}", event.eventId());
        }
    }
}