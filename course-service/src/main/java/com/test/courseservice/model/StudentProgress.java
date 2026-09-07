package com.test.courseservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "student_progress")
@CompoundIndex(name = "student_course_unique_idx", def = "{'studentId': 1, 'courseId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProgress {

    @Id
    private String id;

    private String studentId;
    private String courseId;

    private List<String> completedVideoIds;

    private Double progressPercentage;
    private Boolean completed;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}