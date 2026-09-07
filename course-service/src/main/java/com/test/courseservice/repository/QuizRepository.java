package com.test.courseservice.repository;

import com.test.courseservice.model.Quiz;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface QuizRepository extends MongoRepository<Quiz, String> {
    Optional<Quiz> findByCourseId(String courseId);
    boolean existsByCourseId(String courseId);
}