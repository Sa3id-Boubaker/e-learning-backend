package com.test.forumservice.repository;

import com.test.forumservice.entity.ForumUpvote;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ForumUpvoteRepository extends MongoRepository<ForumUpvote, String> {

    Optional<ForumUpvote> findByUserIdAndPostId(String userId, String postId);

    boolean existsByUserIdAndPostId(String userId, String postId);

    /**
     * REDÉLIVRÉ (Phase 5) : renvoie le nombre de documents supprimés (0 ou 1)
     * au lieu de void, pour que ForumUpvoteService ne décrémente
     * ForumPost.upvoteCount QUE si un upvote existait vraiment — sinon un
     * DELETE répété (idempotent par design) ferait dériver le compteur en
     * négatif.
     */
    long deleteByUserIdAndPostId(String userId, String postId);

    List<ForumUpvote> findByUserIdAndPostIdIn(String userId, List<String> postIds);

    long deleteByPostId(String postId);
}