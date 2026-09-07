package com.test.forumservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumCommentResponse {

    private String id;
    private String postId;
    private ForumAuthorSummary author;
    private String body;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}