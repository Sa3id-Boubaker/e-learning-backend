package com.test.courseservice.controller;

import com.test.courseservice.dto.VideoCreateRequest;
import com.test.courseservice.dto.VideoResponse;
import com.test.courseservice.dto.VideoUpdateRequest;
import com.test.courseservice.security.AuthenticatedUser;
import com.test.courseservice.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping(value = "/api/chapters/{chapterId}/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoResponse> createVideo(@PathVariable String chapterId,
                                                     @Valid @ModelAttribute VideoCreateRequest request,
                                                     @AuthenticationPrincipal AuthenticatedUser currentUser) {
        VideoResponse response = videoService.createVideo(chapterId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/chapters/{chapterId}/videos")
    public ResponseEntity<List<VideoResponse>> getVideosByChapter(@PathVariable String chapterId,
                                                                  @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(videoService.getVideosByChapter(chapterId, currentUser));
    }

    @GetMapping("/api/videos/{id}")
    public ResponseEntity<VideoResponse> getVideoById(@PathVariable String id,
                                                      @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(videoService.getVideoById(id, currentUser));
    }

    @PutMapping("/api/videos/{id}")
    public ResponseEntity<VideoResponse> updateVideo(@PathVariable String id,
                                                     @Valid @RequestBody VideoUpdateRequest request,
                                                     @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(videoService.updateVideo(id, request, currentUser));
    }

    @DeleteMapping("/api/videos/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable String id,
                                            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        videoService.deleteVideo(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}