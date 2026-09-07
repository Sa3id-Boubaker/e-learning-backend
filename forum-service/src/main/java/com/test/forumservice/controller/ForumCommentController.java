package com.test.forumservice.controller;

import com.test.forumservice.dto.ForumCommentCountResponse;
import com.test.forumservice.dto.ForumCommentCreateRequest;
import com.test.forumservice.dto.ForumCommentResponse;
import com.test.forumservice.dto.ForumCommentUpdateRequest;
import com.test.forumservice.dto.PageResponse;
import com.test.forumservice.security.AuthenticatedUser;
import com.test.forumservice.service.ForumCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Deux bases volontairement séparées, même convention que
 * LiveSessionController/RecordingController côté TRAINING-SERVICE :
 *  - /api/forum/posts/{postId}/comments  → créer + lister, scopé au post parent
 *  - /api/forum/comments/{id}            → éditer/supprimer un commentaire directement
 */
@RestController
@RequiredArgsConstructor
public class ForumCommentController {

    private final ForumCommentService forumCommentService;

    @PostMapping("/api/forum/posts/{postId}/comments")
    public ResponseEntity<ForumCommentResponse> create(@PathVariable String postId,
                                                       @Valid @RequestBody ForumCommentCreateRequest request,
                                                       @AuthenticationPrincipal AuthenticatedUser user) {
        ForumCommentResponse response = forumCommentService.createComment(postId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/forum/posts/{postId}/comments")
    public ResponseEntity<PageResponse<ForumCommentResponse>> list(
            @PathVariable String postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(forumCommentService.listComments(postId, page, size, user));
    }

    @GetMapping("/api/forum/comments/count")
    public ResponseEntity<ForumCommentCountResponse> countAll() {
        return ResponseEntity.ok(forumCommentService.countAllComments());
    }

    @PutMapping("/api/forum/comments/{id}")
    public ResponseEntity<ForumCommentResponse> update(@PathVariable String id,
                                                       @Valid @RequestBody ForumCommentUpdateRequest request,
                                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(forumCommentService.updateComment(id, request, user));
    }

    @DeleteMapping("/api/forum/comments/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        forumCommentService.deleteComment(id, user);
        return ResponseEntity.noContent().build();
    }
}