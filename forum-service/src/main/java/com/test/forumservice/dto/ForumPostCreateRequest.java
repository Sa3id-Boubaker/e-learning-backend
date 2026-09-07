package com.test.forumservice.dto;

import com.test.forumservice.entity.ForumPostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Type is required")
    private ForumPostType type;

    @NotBlank(message = "Body is required")
    private String body;

    /** Requis si type = COURSE, ignoré sinon — la nullité conditionnelle est validée dans ForumPostService. */
    private String courseId;
    private String chapterId;
    private String videoId;

    /** Requis si type = TRAINING, ignoré sinon — même logique. */
    private String trainingId;
    private String liveSessionId;
    private String recordingId;
}