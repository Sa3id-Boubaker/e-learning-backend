package com.test.forumservice.service;

import com.test.forumservice.entity.ForumPost;
import com.test.forumservice.entity.ForumUpvote;
import com.test.forumservice.exception.DuplicateUpvoteException;
import com.test.forumservice.repository.ForumUpvoteRepository;
import com.test.forumservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ForumUpvoteService {

    private final ForumUpvoteRepository upvoteRepository;
    private final MongoTemplate mongoTemplate;
    private final ForumPostService forumPostService;

    /** POST strict : 409 si déjà upvoté, via l'index unique (userId, postId). */
    public void create(String postId, AuthenticatedUser user) {
        forumPostService.findOrThrow(postId); // 404 si le post n'existe pas

        ForumUpvote upvote = ForumUpvote.builder()
                .postId(postId)
                .userId(user.userId())
                .createdAt(LocalDateTime.now())
                .build();

        try {
            upvoteRepository.save(upvote);
        } catch (DuplicateKeyException e) {
            throw new DuplicateUpvoteException("This post is already upvoted.");
        }

        incrementUpvoteCount(postId, 1);
    }

    /**
     * DELETE idempotent : jamais de 404 si l'upvote n'existait déjà pas.
     * Le décrément n'a lieu QUE si un document a réellement été supprimé
     * (deleteByUserIdAndPostId renvoie le count depuis la Phase 5), pour ne
     * jamais faire dériver upvoteCount en négatif sous des appels répétés.
     */
    public void delete(String postId, AuthenticatedUser user) {
        long deleted = upvoteRepository.deleteByUserIdAndPostId(user.userId(), postId);
        if (deleted > 0) {
            incrementUpvoteCount(postId, -1);
        }
    }

    private void incrementUpvoteCount(String postId, int delta) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(postId)),
                new Update().inc("upvoteCount", delta),
                ForumPost.class);
    }
}