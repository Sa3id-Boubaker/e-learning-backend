package com.test.courseservice.repository;

import com.test.courseservice.model.Question;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface QuestionRepository extends MongoRepository<Question, String> {
    List<Question> findByQuizIdOrderByOrderAsc(String quizId);
    void deleteByQuizId(String quizId);
}