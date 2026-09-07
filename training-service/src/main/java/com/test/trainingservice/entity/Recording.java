package com.test.trainingservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * A Recording belongs to exactly one LiveSession, referenced by sessionId only —
 * no embedded LiveSession object, no DB-level relationship, exactly like LiveSession
 * itself only references Training through trainingId.
 * A LiveSession can have AT MOST ONE Recording: enforced here with a unique index on
 * sessionId (relying on the DB constraint, not just an application-level existence check,
 * so two concurrent uploads for the same session can't both succeed).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "recordings")
public class Recording {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sessionId;

    private String title;
    private String description;

    /** Cloudinary secure_url — the public delivery URL used by the frontend video player. */
    private String videoUrl;

    /** Cloudinary public_id — internal technical field, NEVER exposed in any response DTO. */
    private String videoPublicId;

    /** Duration in seconds, rounded from Cloudinary's reported duration — never client-supplied. */
    private Long duration;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}