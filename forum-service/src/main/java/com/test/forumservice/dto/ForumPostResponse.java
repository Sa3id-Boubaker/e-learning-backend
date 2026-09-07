package com.test.forumservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.test.forumservice.entity.ForumPostType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostResponse {

    private String id;
    private String title;
    private String body;
    private ForumPostType type;

    private ForumAuthorSummary author;

    /** Un seul trio (course/chapter/video OU training/liveSession/recording) est non-null selon le type. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ForumReferenceSummary course;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ForumReferenceSummary chapter;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ForumReferenceSummary video;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ForumReferenceSummary training;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ForumReferenceSummary liveSession;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ForumReferenceSummary recording;

    /** long — aligné sur le type réel de ForumPost.commentCount/upvoteCount (évite toute conversion avec perte). */
    private long commentCount;
    private long upvoteCount;

    private boolean bookmarked;
    private boolean upvoted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}