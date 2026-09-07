package com.test.notificationservice.listener;

import com.test.notificationservice.config.RabbitMQConfig;
import com.test.notificationservice.event.EnrollmentActivatedEvent;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrollmentActivatedListener {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ENROLLMENT_ACTIVATED)
    public void handleEnrollmentActivated(EnrollmentActivatedEvent event) {

        if (notificationRepository.existsByEventId(event.eventId())) {
            log.info("Duplicate EnrollmentActivatedEvent ignored, eventId={}", event.eventId());
            return;
        }

        Notification notification = Notification.builder()
                .eventId(event.eventId())
                .userId(event.studentId())
                .type(NotificationType.ENROLLMENT_ACTIVATED)
                .title("Course activated")
                .message("Your enrollment in the course \"" + event.courseTitle() + "\" has been activated. You can now access the course.")
                .referenceId(event.courseId())
                .referenceType("COURSE")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            Notification saved = notificationRepository.save(notification);
            log.info("Notification created for userId={}, eventId={}", event.studentId(), event.eventId());

            sseEmitterRegistry.sendToUser(event.studentId(), notificationService.toResponse(saved));

        } catch (DuplicateKeyException e) {
            log.info("Duplicate EnrollmentActivatedEvent ignored (race condition), eventId={}", event.eventId());
        }
    }
}