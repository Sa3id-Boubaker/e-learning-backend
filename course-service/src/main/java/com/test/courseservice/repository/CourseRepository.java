package com.test.courseservice.repository;

import com.test.courseservice.model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CourseRepository extends MongoRepository<Course, String> {

    // ADMIN
    Page<Course> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // FORMATEUR
    Page<Course> findByInstructorId(String instructorId, Pageable pageable);
    Page<Course> findByInstructorIdAndTitleContainingIgnoreCase(String instructorId, String title, Pageable pageable);

    // ETUDIANT
    Page<Course> findByPublishedTrue(Pageable pageable);
    Page<Course> findByPublishedTrueAndTitleContainingIgnoreCase(String title, Pageable pageable);
}