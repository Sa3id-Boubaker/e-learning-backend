package com.test.courseservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.test.courseservice.model.CertificateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateResponse {

    private String id;
    private String certificateNumber;
    private String courseId;
    private String courseTitle;
    private String studentName;
    private String instructorName;
    private Integer quizScore;
    private LocalDateTime issuedAt;
    private CertificateStatus status;
    private String studentProfileImage;
    private String instructorProfileImage;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String studentId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String instructorId;
}