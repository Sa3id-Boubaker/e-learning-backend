package com.test.notificationservice.listener;

import com.test.notificationservice.config.RabbitMQConfig;
import com.test.notificationservice.event.ForumTrainingPostCreatedEvent;
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
 * Mirrors TrainingEnrollmentActivatedListener exactly: existsByEventId check first, build+save
 * Notification, catch DuplicateKeyException as the race-condition fallback, push over the
 * existing SseEmitterRegistry. Recipient is the assigned FORMATEUR (event.instructorId()), never
 * the post's author.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ForumTrainingPostCreatedListener {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_FORUM_TRAINING_POST_CREATED)
    public void handleForumTrainingPostCreated(ForumTrainingPostCreatedEvent event) {

        if (event.eventId() == null || event.instructorId() == null || event.postId() == null) {
            log.error("Discarding malformed ForumTrainingPostCreatedEvent (missing required fields): {}", event);
            return;
        }

        if (notificationRepository.existsByEventId(event.eventId())) {
            log.info("Duplicate ForumTrainingPostCreatedEvent ignored, eventId={}", event.eventId());
            return;
        }

        Notification notification = Notification.builder()
                .eventId(event.eventId())
                .userId(event.instructorId())
                .type(NotificationType.FORUM_TRAINING_POST)
                .title("New forum question")
                .message("Un étudiant a posé une question dans votre formation.")
                .referenceId(event.postId())
                .referenceType("FORUM_POST")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            Notification saved = notificationRepository.save(notification);
            log.info("Notification created for userId={}, eventId={}", event.instructorId(), event.eventId());

            sseEmitterRegistry.sendToUser(event.instructorId(), notificationService.toResponse(saved));

        } catch (DuplicateKeyException e) {
            log.info("Duplicate ForumTrainingPostCreatedEvent ignored (race condition), eventId={}", event.eventId());
        }
    }
}