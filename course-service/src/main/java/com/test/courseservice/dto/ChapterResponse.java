package com.test.courseservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterResponse {
    private String id;
    private String courseId;
    private String title;
    private String description;
    private Integer order;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}