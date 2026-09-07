package com.test.forumservice.service;

import com.test.forumservice.client.UserServiceClient;
import com.test.forumservice.dto.*;
import com.test.forumservice.entity.ForumComment;
import com.test.forumservice.entity.ForumPost;
import com.test.forumservice.event.ForumEventPublisher;
import com.test.forumservice.exception.ForumCommentNotFoundException;
import com.test.forumservice.mapper.ForumCommentMapper;
import com.test.forumservice.repository.ForumCommentRepository;
import com.test.forumservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ForumCommentService {

    private final ForumCommentRepository commentRepository;
    private final MongoTemplate mongoTemplate;
    private final ForumCommentMapper forumCommentMapper;
    private final UserServiceClient userServiceClient;
    private final ForumPostService forumPostService;
    private final ForumEventPublisher forumEventPublisher;

    public ForumCommentResponse createComment(String postId, ForumCommentCreateRequest request, AuthenticatedUser user) {
        ForumPost post = forumPostService.findOrThrow(postId); // 404 si le post n'existe pas

        LocalDateTime now = LocalDateTime.now();
        ForumComment comment = ForumComment.builder()
                .postId(post.getId())
                .authorId(user.userId())
                .body(request.getBody())
                .createdAt(now)
                .updatedAt(now)
                .build();
        comment = commentRepository.save(comment);

        incrementCommentCount(post.getId(), 1);

        // Never notify the post author about their own comment.
        if (!post.getAuthorId().equals(comment.getAuthorId())) {
            forumEventPublisher.publishPostCommentCreated(comment, post.getAuthorId(), post.getTitle());
        }

        ForumAuthorSummary author = userServiceClient.getBasicInfo(comment.getAuthorId(), user.token());
        return forumCommentMapper.toResponse(comment, author);
    }

    public PageResponse<ForumCommentResponse> listComments(String postId, int page, int size, AuthenticatedUser user) {
        forumPostService.findOrThrow(postId); // 404 si le post n'existe pas

        long total = commentRepository.countByPostId(postId);
        List<ForumComment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId, PageRequest.of(page, size));

        List<ForumCommentResponse> content = comments.stream()
                .map(c -> forumCommentMapper.toResponse(c, userServiceClient.getBasicInfo(c.getAuthorId(), user.token())))
                .toList();

        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

    public ForumCommentResponse updateComment(String commentId, ForumCommentUpdateRequest request, AuthenticatedUser user) {
        ForumComment comment = findOrThrow(commentId);
        forumPostService.assertOwnerOrAdmin(comment.getAuthorId(), user);

        comment.setBody(request.getBody());
        comment.setUpdatedAt(LocalDateTime.now());
        comment = commentRepository.save(comment);

        ForumAuthorSummary author = userServiceClient.getBasicInfo(comment.getAuthorId(), user.token());
        return forumCommentMapper.toResponse(comment, author);
    }

    public void deleteComment(String commentId, AuthenticatedUser user) {
        ForumComment comment = findOrThrow(commentId);
        forumPostService.assertOwnerOrAdmin(comment.getAuthorId(), user);

        commentRepository.delete(comment);
        incrementCommentCount(comment.getPostId(), -1);
    }

    private ForumComment findOrThrow(String commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ForumCommentNotFoundException("Forum comment not found: " + commentId));
    }

    /**
     * $inc atomique — évite un read-modify-write sur ForumPost.commentCount
     * sous création/suppression concurrente de commentaires, même logique
     * que ForumUpvoteService pour upvoteCount.
     */
    private void incrementCommentCount(String postId, int delta) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(postId)),
                new Update().inc("commentCount", delta),
                ForumPost.class);
    }

    /** ADMIN-only (imposé par SecurityConfig) — total global des commentaires, tous posts confondus. */
    public ForumCommentCountResponse countAllComments() {
        return new ForumCommentCountResponse(commentRepository.count());
    }
}