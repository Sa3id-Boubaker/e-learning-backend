package com.test.forumservice.mapper;

import com.test.forumservice.dto.ForumAuthorSummary;
import com.test.forumservice.dto.ForumPostResponse;
import com.test.forumservice.dto.ForumReferenceSummary;
import com.test.forumservice.entity.ForumPost;
import com.test.forumservice.entity.ForumPostType;
import org.springframework.stereotype.Component;

@Component
public class ForumPostMapper {

    public ForumPostResponse toResponse(ForumPost post, ForumAuthorSummary author,
                                        ForumReferenceSummary course, ForumReferenceSummary chapter, ForumReferenceSummary video,
                                        ForumReferenceSummary training, ForumReferenceSummary liveSession, ForumReferenceSummary recording,
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
            builder.course(course).chapter(chapter).video(video);
        } else {
            builder.training(training).liveSession(liveSession).recording(recording);
        }

        return builder.build();
    }
}