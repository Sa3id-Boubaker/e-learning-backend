package com.test.courseservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponse {
    private String id;
    private String chapterId;
    private String title;
    private String description;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String videoUrl;

    private Integer duration;
    private Integer order;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}