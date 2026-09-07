package com.test.courseservice.repository;

import com.test.courseservice.model.Chapter;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChapterRepository extends MongoRepository<Chapter, String> {
    List<Chapter> findByCourseIdOrderByOrderAsc(String courseId);
}