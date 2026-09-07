package com.test.courseservice.repository;

import com.test.courseservice.model.Enrollment;
import com.test.courseservice.model.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EnrollmentRepository extends MongoRepository<Enrollment, String> {
    Optional<Enrollment> findByStudentIdAndCourseId(String studentId, String courseId);
    Page<Enrollment> findByStudentId(String studentId, Pageable pageable);
    Page<Enrollment> findByCourseId(String courseId, Pageable pageable);
    Page<Enrollment> findByStudentIdAndEnrollmentStatus(String studentId, EnrollmentStatus status, Pageable pageable);
    Optional<Enrollment> findByIdAndEnrollmentStatus(String id, EnrollmentStatus status);

    /** Utilisé uniquement par CourseService.deleteCourse() pour la cascade — supprime tous les enrollments du cours. */
    void deleteByCourseId(String courseId);
}