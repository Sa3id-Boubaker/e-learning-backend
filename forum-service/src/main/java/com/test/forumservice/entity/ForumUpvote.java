package com.test.forumservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "forum_upvotes")
@CompoundIndex(name = "user_post_unique_idx", def = "{'userId': 1, 'postId': 1}", unique = true)
public class ForumUpvote {

    @Id
    private String id;

    private String postId;
    private String userId;

    private LocalDateTime createdAt;
}