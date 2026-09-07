package com.test.courseservice.controller;

import com.test.courseservice.dto.ChapterCreateRequest;
import com.test.courseservice.dto.ChapterResponse;
import com.test.courseservice.dto.ChapterUpdateRequest;
import com.test.courseservice.security.AuthenticatedUser;
import com.test.courseservice.service.ChapterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;

    @PostMapping("/api/courses/{courseId}/chapters")
    public ResponseEntity<ChapterResponse> createChapter(@PathVariable String courseId,
                                                         @Valid @RequestBody ChapterCreateRequest request,
                                                         @AuthenticationPrincipal AuthenticatedUser currentUser) {
        ChapterResponse response = chapterService.createChapter(courseId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/courses/{courseId}/chapters")
    public ResponseEntity<List<ChapterResponse>> getChaptersByCourse(@PathVariable String courseId,
                                                                     @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(chapterService.getChaptersByCourse(courseId, currentUser));
    }

    @GetMapping("/api/chapters/{id}")
    public ResponseEntity<ChapterResponse> getChapterById(@PathVariable String id,
                                                          @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(chapterService.getChapterById(id, currentUser));
    }

    @PutMapping("/api/chapters/{id}")
    public ResponseEntity<ChapterResponse> updateChapter(@PathVariable String id,
                                                         @Valid @RequestBody ChapterUpdateRequest request,
                                                         @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(chapterService.updateChapter(id, request, currentUser));
    }

    @DeleteMapping("/api/chapters/{id}")
    public ResponseEntity<Void> deleteChapter(@PathVariable String id,
                                              @AuthenticationPrincipal AuthenticatedUser currentUser) {
        chapterService.deleteChapter(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}