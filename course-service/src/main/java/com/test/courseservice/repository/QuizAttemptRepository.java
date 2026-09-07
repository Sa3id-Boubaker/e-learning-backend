package com.test.courseservice.repository;

import com.test.courseservice.model.QuizAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends MongoRepository<QuizAttempt, String> {
    List<QuizAttempt> findByQuizIdAndStudentIdOrderBySubmittedAtDesc(String quizId, String studentId);
    Optional<QuizAttempt> findFirstByQuizIdAndStudentIdOrderBySubmittedAtDesc(String quizId, String studentId);
}