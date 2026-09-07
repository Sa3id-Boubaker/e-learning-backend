package com.test.forumservice.repository;

import com.test.forumservice.entity.ForumComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ForumCommentRepository extends MongoRepository<ForumComment, String> {

    List<ForumComment> findByPostIdOrderByCreatedAtAsc(String postId, Pageable pageable);

    /** REDÉLIVRÉ (Phase 6) : nécessaire pour paginer GET /api/forum/posts/{postId}/comments. */
    long countByPostId(String postId);

    long deleteByPostId(String postId);
}