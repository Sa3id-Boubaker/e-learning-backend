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
public class MyCourseResponse {
    private String courseId;
    private String courseTitle;
    private String category;
    private String image;
    private Double progressPercentage;
    private Boolean completed;
    private LocalDateTime enrolledAt;
}