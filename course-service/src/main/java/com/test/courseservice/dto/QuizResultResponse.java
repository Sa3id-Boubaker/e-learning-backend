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
public class QuizResultResponse {
    private String quizId;
    private String courseId;
    private Integer score;
    private Integer passingScore;
    private Boolean passed;
    private LocalDateTime submittedAt;
}