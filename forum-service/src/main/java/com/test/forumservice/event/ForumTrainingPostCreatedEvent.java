package com.test.forumservice.event;

import java.time.LocalDateTime;

/**
 * Published to RabbitMQ (exchange "omarise.events", routing key "forum.training-post.created")
 * whenever an ÉTUDIANT creates a TRAINING-type ForumPost. instructorId is resolved authoritatively
 * from TRAINING-SERVICE (Training.instructorId) — never accepted from the frontend. No JWT,
 * password, or payment information — deliberately minimal, same convention as
 * TrainingEnrollmentActivatedEvent.
 */
public record ForumTrainingPostCreatedEvent(
        String eventId,
        String postId,
        String trainingId,
        String instructorId,
        String authorId,
        String postTitle,
        LocalDateTime createdAt
) {}