package com.test.courseservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "certificates")
@CompoundIndex(name = "student_course_cert_unique_idx", def = "{'studentId': 1, 'courseId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificate {

    @Id
    private String id;

    @Indexed(unique = true)
    private String certificateNumber;

    private String studentId;
    private String courseId;
    private String courseTitle;
    private String studentName;
    private String instructorId;
    private String instructorName;
    private Integer quizScore;
    private String studentProfileImage;
    private String instructorProfileImage;

    private LocalDateTime issuedAt;
    private CertificateStatus status;
}