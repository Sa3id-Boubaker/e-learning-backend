package com.test.courseservice.repository;

import com.test.courseservice.model.Certificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CertificateRepository extends MongoRepository<Certificate, String> {
    Optional<Certificate> findByStudentIdAndCourseId(String studentId, String courseId);
    Optional<Certificate> findByCertificateNumber(String certificateNumber);
    Page<Certificate> findByStudentId(String studentId, Pageable pageable);
    Page<Certificate> findByCourseId(String courseId, Pageable pageable);
}