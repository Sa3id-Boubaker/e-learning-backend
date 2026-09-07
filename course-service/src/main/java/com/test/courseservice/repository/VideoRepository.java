package com.test.courseservice.repository;

import com.test.courseservice.model.Video;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface VideoRepository extends MongoRepository<Video, String> {
    List<Video> findByChapterIdOrderByOrderAsc(String chapterId);
    List<Video> findByChapterIdIn(List<String> chapterIds);
}