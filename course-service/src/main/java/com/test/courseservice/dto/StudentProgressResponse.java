package com.test.courseservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProgressResponse {
    private String id;
    private String courseId;
    private List<String> completedVideoIds;
    private Integer completedVideosCount;
    private Integer totalVideosCount;
    private Double progressPercentage;
    private Boolean completed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}