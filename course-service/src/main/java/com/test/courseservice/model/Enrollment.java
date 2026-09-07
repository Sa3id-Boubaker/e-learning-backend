package com.test.courseservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "enrollments")
@CompoundIndex(name = "student_course_enrollment_unique_idx", def = "{'studentId': 1, 'courseId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @Id
    private String id;

    private String studentId;
    private String courseId;
    private String courseTitle;
    private BigDecimal amountAtEnrollment;
    private EnrollmentStatus enrollmentStatus;

    private LocalDateTime enrolledAt;
    private LocalDateTime activatedAt;
    private String activatedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}