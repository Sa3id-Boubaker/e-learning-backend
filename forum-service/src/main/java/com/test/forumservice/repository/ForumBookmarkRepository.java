package com.test.forumservice.repository;

import com.test.forumservice.entity.ForumBookmark;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ForumBookmarkRepository extends MongoRepository<ForumBookmark, String> {

    Optional<ForumBookmark> findByUserIdAndPostId(String userId, String postId);

    boolean existsByUserIdAndPostId(String userId, String postId);

    void deleteByUserIdAndPostId(String userId, String postId);

    List<ForumBookmark> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /** REDÉLIVRÉ (Phase 6) : nécessaire pour paginer GET /api/forum/posts/bookmarked. */
    long countByUserId(String userId);

    List<ForumBookmark> findByUserIdAndPostIdIn(String userId, List<String> postIds);

    long deleteByPostId(String postId);
}