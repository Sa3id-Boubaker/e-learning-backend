package com.test.notificationservice.listener;

import com.test.notificationservice.config.RabbitMQConfig;
import com.test.notificationservice.event.ForumPostCommentCreatedEvent;
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
 * existing SseEmitterRegistry. Recipient is the original post author (event.postAuthorId()) —
 * forum-service never publishes this event when the commenter is the author themselves, but this
 * listener stays defensive and would simply notify that same user if it ever happened.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ForumPostCommentCreatedListener {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_FORUM_POST_COMMENT_CREATED)
    public void handleForumPostCommentCreated(ForumPostCommentCreatedEvent event) {

        if (event.eventId() == null || event.postAuthorId() == null || event.postId() == null) {
            log.error("Discarding malformed ForumPostCommentCreatedEvent (missing required fields): {}", event);
            return;
        }

        if (notificationRepository.existsByEventId(event.eventId())) {
            log.info("Duplicate ForumPostCommentCreatedEvent ignored, eventId={}", event.eventId());
            return;
        }

        Notification notification = Notification.builder()
                .eventId(event.eventId())
                .userId(event.postAuthorId())
                .type(NotificationType.FORUM_POST_COMMENT)
                .title("New reply to your post")
                .message("Quelqu'un a répondu à votre question dans le forum.")
                .referenceId(event.postId())
                .referenceType("FORUM_POST")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            Notification saved = notificationRepository.save(notification);
            log.info("Notification created for userId={}, eventId={}", event.postAuthorId(), event.eventId());

            sseEmitterRegistry.sendToUser(event.postAuthorId(), notificationService.toResponse(saved));

        } catch (DuplicateKeyException e) {
            log.info("Duplicate ForumPostCommentCreatedEvent ignored (race condition), eventId={}", event.eventId());
        }
    }
}