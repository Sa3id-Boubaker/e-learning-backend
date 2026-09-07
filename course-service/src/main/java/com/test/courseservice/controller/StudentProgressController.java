package com.test.courseservice.controller;

import com.test.courseservice.dto.StudentProgressResponse;
import com.test.courseservice.dto.VideoProgressResponse;
import com.test.courseservice.security.AuthenticatedUser;
import com.test.courseservice.service.StudentProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/{courseId}/progress")
@RequiredArgsConstructor
public class StudentProgressController {

    private final StudentProgressService studentProgressService;

    @PostMapping("/videos/{videoId}/complete")
    public ResponseEntity<StudentProgressResponse> markVideoComplete(@PathVariable String courseId,
                                                                     @PathVariable String videoId,
                                                                     @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(studentProgressService.markVideoComplete(courseId, videoId, currentUser));
    }

    @GetMapping
    public ResponseEntity<StudentProgressResponse> getCourseProgress(@PathVariable String courseId,
                                                                     @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(studentProgressService.getCourseProgress(courseId, currentUser));
    }

    @GetMapping("/videos/{videoId}")
    public ResponseEntity<VideoProgressResponse> getVideoProgress(@PathVariable String courseId,
                                                                  @PathVariable String videoId,
                                                                  @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(studentProgressService.getVideoProgress(courseId, videoId, currentUser));
    }
}