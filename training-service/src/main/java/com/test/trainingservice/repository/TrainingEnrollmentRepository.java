package com.test.trainingservice.repository;

import com.test.trainingservice.entity.TrainingEnrollment;
import com.test.trainingservice.entity.TrainingEnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TrainingEnrollmentRepository extends MongoRepository<TrainingEnrollment, String> {

    /** The one-enrollment-per-student-per-training lookup — backed by the unique compound index. */
    Optional<TrainingEnrollment> findByStudentIdAndTrainingId(String studentId, String trainingId);

    /** Fast existence check used by hasActiveTrainingEnrollment()/assertActiveTrainingEnrollment(). */
    boolean existsByStudentIdAndTrainingIdAndStatus(String studentId, String trainingId, TrainingEnrollmentStatus status);

    /** Used by TrainingService.getDeletionImpact() to report counts before a cascade delete. */
    long countByTrainingId(String trainingId);

    long countByTrainingIdAndStatus(String trainingId, TrainingEnrollmentStatus status);

    /** Used by TrainingService.delete() to cascade-delete every enrollment of a training. */
    void deleteByTrainingId(String trainingId);

    /** GET /api/training-enrollments/my — sorted/paged at the database level. */
    Page<TrainingEnrollment> findByStudentIdAndStatus(String studentId, TrainingEnrollmentStatus status, Pageable pageable);

    // Admin search (GET /api/training-enrollments) combines up to 3 independent optional
    // filters (trainingId, studentId, status). Rather than a combinatorial explosion of derived
    // query methods here, TrainingEnrollmentService builds that one dynamic query directly with
    // MongoTemplate — the standard approach for a variable filter set in Spring Data MongoDB.

}