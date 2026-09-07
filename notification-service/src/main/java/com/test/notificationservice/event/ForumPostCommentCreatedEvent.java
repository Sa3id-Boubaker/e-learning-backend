package com.test.notificationservice.event;

import java.time.LocalDateTime;

/**
 * Consumer-side copy of FORUM-SERVICE's event of the same name — hand-duplicated exactly like
 * TrainingEnrollmentActivatedEvent already is between training-service and notification-service.
 */
public record ForumPostCommentCreatedEvent(
        String eventId,
        String postId,
        String commentId,
        String postAuthorId,
        String commenterId,
        String postTitle,
        LocalDateTime createdAt
) {}