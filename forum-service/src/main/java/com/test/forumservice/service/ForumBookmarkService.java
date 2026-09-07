package com.test.forumservice.service;

import com.test.forumservice.dto.ForumPostResponse;
import com.test.forumservice.dto.PageResponse;
import com.test.forumservice.entity.ForumBookmark;
import com.test.forumservice.entity.ForumPost;
import com.test.forumservice.exception.DuplicateBookmarkException;
import com.test.forumservice.repository.ForumBookmarkRepository;
import com.test.forumservice.repository.ForumPostRepository;
import com.test.forumservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ForumBookmarkService {

    private final ForumBookmarkRepository bookmarkRepository;
    private final ForumPostRepository postRepository;
    private final ForumPostService forumPostService;

    /** POST strict : 409 si déjà bookmarké, via l'index unique (userId, postId). */
    public void create(String postId, AuthenticatedUser user) {
        forumPostService.findOrThrow(postId); // 404 si le post n'existe pas

        ForumBookmark bookmark = ForumBookmark.builder()
                .postId(postId)
                .userId(user.userId())
                .createdAt(LocalDateTime.now())
                .build();

        try {
            bookmarkRepository.save(bookmark);
        } catch (DuplicateKeyException e) {
            throw new DuplicateBookmarkException("This post is already bookmarked.");
        }
    }

    /** DELETE idempotent : jamais de 404 si le bookmark n'existait déjà pas. */
    public void delete(String postId, AuthenticatedUser user) {
        bookmarkRepository.deleteByUserIdAndPostId(user.userId(), postId);
    }

    public PageResponse<ForumPostResponse> listMine(AuthenticatedUser user, int page, int size) {
        List<ForumBookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(
                user.userId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<String> postIds = bookmarks.stream().map(ForumBookmark::getPostId).toList();
        Map<String, ForumPost> postsById = new LinkedHashMap<>();
        postRepository.findAllById(postIds).forEach(p -> postsById.put(p.getId(), p));

        // Préserve l'ordre du bookmark (le plus récent d'abord), pas l'ordre de findAllById.
        List<ForumPostResponse> content = bookmarks.stream()
                .map(b -> postsById.get(b.getPostId()))
                .filter(java.util.Objects::nonNull) // défensif : le post a pu être supprimé entre-temps
                .map(post -> forumPostService.buildResponse(post, user))
                .toList();

        long total = bookmarkRepository.countByUserId(user.userId());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }
}