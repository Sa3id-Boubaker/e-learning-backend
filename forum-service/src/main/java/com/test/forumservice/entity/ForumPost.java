package com.test.forumservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Une discussion de forum, rattachée soit à un Course (COURSE-SERVICE) soit à un Training
 * (TRAINING-SERVICE) selon `type`. Les deux groupes de champs de référence ci-dessous sont
 * mutuellement exclusifs par construction applicative (ForumPostService les valide selon
 * `type`) — Mongo lui-même ne les contraint pas, un document COURSE laisse simplement les champs
 * TRAINING à null, et inversement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "forum_posts")
public class ForumPost {

    @Id
    private String id;

    @Indexed
    private String authorId;

    private String title;
    private String body;

    @Indexed
    private ForumPostType type;

    // --- Références COURSE — renseignées uniquement si type == COURSE ---
    @Indexed
    private String courseId;
    private String chapterId;
    /** Référence l'entité réelle Video de COURSE-SERVICE (pas de classe "Lesson" côté COURSE-SERVICE). */
    private String videoId;

    // --- Références TRAINING — renseignées uniquement si type == TRAINING ---
    @Indexed
    private String trainingId;
    private String liveSessionId;
    private String recordingId;

    /** Dénormalisé — incrémenté/décrémenté atomiquement par ForumCommentService via MongoTemplate. */
    @Builder.Default
    private long commentCount = 0;

    /** Dénormalisé — incrémenté/décrémenté atomiquement par ForumPostService.upvote()/removeUpvote(). */
    @Indexed
    @Builder.Default
    private long upvoteCount = 0;

    @Indexed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}