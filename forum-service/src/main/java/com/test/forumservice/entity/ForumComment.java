package com.test.forumservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Une réponse à un ForumPost, référencé par postId uniquement — pas d'objet ForumPost
 * embarqué, même principe que Recording→LiveSession dans TRAINING-SERVICE.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "forum_comments")
@CompoundIndex(name = "post_created_idx", def = "{'postId': 1, 'createdAt': 1}")
public class ForumComment {

    @Id
    private String id;

    private String postId;
    private String authorId;
    private String body;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}