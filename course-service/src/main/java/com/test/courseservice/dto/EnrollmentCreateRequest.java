package com.test.courseservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentCreateRequest {

    @NotBlank(message = "Student id is required")
    private String studentId;

    @NotBlank(message = "Course id is required")
    private String courseId;
}