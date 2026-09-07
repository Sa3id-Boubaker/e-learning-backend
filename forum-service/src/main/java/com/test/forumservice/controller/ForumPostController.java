package com.test.forumservice.controller;

import com.test.forumservice.dto.ForumPostCreateRequest;
import com.test.forumservice.dto.ForumPostResponse;
import com.test.forumservice.dto.ForumPostUpdateRequest;
import com.test.forumservice.dto.PageResponse;
import com.test.forumservice.entity.ForumPostType;
import com.test.forumservice.exception.InvalidForumPostTypeException;
import com.test.forumservice.security.AuthenticatedUser;
import com.test.forumservice.service.ForumBookmarkService;
import com.test.forumservice.service.ForumPostService;
import com.test.forumservice.service.ForumUpvoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ForumPostController {

    private final ForumPostService forumPostService;
    private final ForumBookmarkService forumBookmarkService;
    private final ForumUpvoteService forumUpvoteService;

    @PostMapping("/api/forum/posts")
    public ResponseEntity<ForumPostResponse> create(@Valid @RequestBody ForumPostCreateRequest request,
                                                    @AuthenticationPrincipal AuthenticatedUser user) {
        ForumPostResponse response = forumPostService.createPost(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/forum/posts")
    public ResponseEntity<PageResponse<ForumPostResponse>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String trainingId,
            @RequestParam(required = false) String authorId,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(
                forumPostService.listPosts(parseType(type), courseId, trainingId, authorId, sort, page, size, user));
    }

    /**
     * Route littérale "/bookmarked" avant "/{id}" : sans risque avec Spring MVC
     * (AntPathMatcher priorise toujours le pattern le plus spécifique, peu
     * importe l'ordre de déclaration) — même précédent que
     * GET /api/courses/summary côté COURSE-SERVICE.
     */
    @GetMapping("/api/forum/posts/bookmarked")
    public ResponseEntity<PageResponse<ForumPostResponse>> listBookmarked(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(forumBookmarkService.listMine(user, page, size));
    }

    @GetMapping("/api/forum/posts/{id}")
    public ResponseEntity<ForumPostResponse> getById(@PathVariable String id,
                                                     @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(forumPostService.getPost(id, user));
    }

    @PutMapping("/api/forum/posts/{id}")
    public ResponseEntity<ForumPostResponse> update(@PathVariable String id,
                                                    @Valid @RequestBody ForumPostUpdateRequest request,
                                                    @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(forumPostService.updatePost(id, request, user));
    }

    @DeleteMapping("/api/forum/posts/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        forumPostService.deletePost(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/forum/posts/{id}/bookmark")
    public ResponseEntity<Void> bookmark(@PathVariable String id,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        forumBookmarkService.create(id, user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/forum/posts/{id}/bookmark")
    public ResponseEntity<Void> unbookmark(@PathVariable String id,
                                           @AuthenticationPrincipal AuthenticatedUser user) {
        forumBookmarkService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/forum/posts/{id}/upvote")
    public ResponseEntity<Void> upvote(@PathVariable String id,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        forumUpvoteService.create(id, user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/forum/posts/{id}/upvote")
    public ResponseEntity<Void> removeUpvote(@PathVariable String id,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        forumUpvoteService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    private ForumPostType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return ForumPostType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidForumPostTypeException("Invalid type: " + type);
        }
    }
}