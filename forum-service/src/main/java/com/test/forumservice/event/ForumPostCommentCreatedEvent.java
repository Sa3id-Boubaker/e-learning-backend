package com.test.forumservice.event;

import java.time.LocalDateTime;

/**
 * Published to RabbitMQ (exchange "omarise.events", routing key "forum.post-comment.created")
 * whenever a comment is added to a ForumPost — except when the commenter is the post's own
 * author (never notify yourself, enforced by the caller before this event is even built).
 * postAuthorId is resolved from the persisted ForumPost, never from the frontend.
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