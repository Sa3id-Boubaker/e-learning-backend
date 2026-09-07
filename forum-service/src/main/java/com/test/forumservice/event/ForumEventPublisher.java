package com.test.forumservice.event;

import com.test.forumservice.config.RabbitMQConfig;
import com.test.forumservice.entity.ForumComment;
import com.test.forumservice.entity.ForumPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mirrors TrainingEnrollmentEventPublisher exactly: fresh UUID eventId per publish call, same
 * shared exchange, fire-and-forget (a RabbitMQ outage must never fail a post/comment creation
 * that has already been persisted — see the try/catch below on both methods).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ForumEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /** Caller (ForumPostService) has already checked: author is ÉTUDIANT, type is TRAINING, instructorId resolved. */
    public void publishTrainingPostCreated(ForumPost post, String instructorId) {
        ForumTrainingPostCreatedEvent event = new ForumTrainingPostCreatedEvent(
                UUID.randomUUID().toString(), post.getId(), post.getTrainingId(), instructorId,
                post.getAuthorId(), post.getTitle(), LocalDateTime.now());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_FORUM_TRAINING_POST_CREATED, event);
        } catch (AmqpException e) {
            log.error("Failed to publish ForumTrainingPostCreatedEvent for postId={}: {}", post.getId(), e.getMessage());
        }
    }

    /** Caller (ForumCommentService) has already checked commenter != postAuthorId before calling this. */
    public void publishPostCommentCreated(ForumComment comment, String postAuthorId, String postTitle) {
        ForumPostCommentCreatedEvent event = new ForumPostCommentCreatedEvent(
                UUID.randomUUID().toString(), comment.getPostId(), comment.getId(), postAuthorId,
                comment.getAuthorId(), postTitle, LocalDateTime.now());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_FORUM_POST_COMMENT_CREATED, event);
        } catch (AmqpException e) {
            log.error("Failed to publish ForumPostCommentCreatedEvent for commentId={}: {}", comment.getId(), e.getMessage());
        }
    }
}