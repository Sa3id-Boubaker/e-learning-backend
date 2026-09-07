package com.test.forumservice.mapper;

import com.test.forumservice.dto.ForumAuthorSummary;
import com.test.forumservice.dto.ForumCommentResponse;
import com.test.forumservice.entity.ForumComment;
import org.springframework.stereotype.Component;

@Component
public class ForumCommentMapper {

    public ForumCommentResponse toResponse(ForumComment comment, ForumAuthorSummary author) {
        return ForumCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .author(author)
                .body(comment.getBody())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}