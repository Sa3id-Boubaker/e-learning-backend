package com.test.courseservice.repository;

import com.test.courseservice.model.StudentProgress;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface StudentProgressRepository extends MongoRepository<StudentProgress, String> {
    Optional<StudentProgress> findByStudentIdAndCourseId(String studentId, String courseId);

    /** Utilisé uniquement par CourseService.deleteCourse() pour la cascade — supprime toute la progression liée au cours. */
    void deleteByCourseId(String courseId);
}