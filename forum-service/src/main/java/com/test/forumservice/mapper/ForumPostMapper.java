package com.test.forumservice.mapper;

import com.test.forumservice.dto.ForumAuthorSummary;
import com.test.forumservice.dto.ForumPostResponse;
import com.test.forumservice.entity.ForumPost;
import com.test.forumservice.entity.ForumPostType;
import org.springframework.stereotype.Component;

@Component
public class ForumPostMapper {

    public ForumPostResponse toResponse(ForumPost post, ForumAuthorSummary author, ForumPostReferences references,
                                        boolean bookmarked, boolean upvoted) {

        ForumPostResponse.ForumPostResponseBuilder builder = ForumPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .body(post.getBody())
                .type(post.getType())
                .author(author)
                .commentCount(post.getCommentCount())
                .upvoteCount(post.getUpvoteCount())
                .bookmarked(bookmarked)
                .upvoted(upvoted)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt());

        if (post.getType() == ForumPostType.COURSE) {
            builder.course(references.course()).chapter(references.chapter()).video(references.video());
        } else {
            builder.training(references.training()).liveSession(references.liveSession()).recording(references.recording());
        }

        return builder.build();
    }
}