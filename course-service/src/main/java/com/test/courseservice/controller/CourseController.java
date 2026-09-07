package com.test.courseservice.controller;

import com.test.courseservice.dto.*;
import com.test.courseservice.security.AuthenticatedUser;
import com.test.courseservice.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseResponse> createCourse(@Valid @ModelAttribute CourseCreateRequest request,
                                                       @AuthenticationPrincipal AuthenticatedUser currentUser) {
        CourseResponse response = courseService.createCourse(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<CourseResponse>> getAllCourses(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(courseService.getAllCourses(search, page, size, currentUser));
    }

    @GetMapping("/summary")
    public ResponseEntity<List<CourseSummaryResponse>> getAllCourseSummaries(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(courseService.getAllCourseSummaries(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourseById(@PathVariable String id,
                                                        @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(courseService.getCourseById(id, currentUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(@PathVariable String id,
                                                       @Valid @RequestBody CourseUpdateRequest request,
                                                       @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(courseService.updateCourse(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String id,
                                             @AuthenticationPrincipal AuthenticatedUser currentUser) {
        courseService.deleteCourse(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseResponse> uploadCourseImage(@PathVariable String id,
                                                            @RequestParam("file") MultipartFile file,
                                                            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(courseService.uploadCourseImage(id, file, currentUser));
    }

    @PutMapping("/{id}/discount")
    public ResponseEntity<CourseResponse> applyDiscount(@PathVariable String id,
                                                        @Valid @RequestBody DiscountRequest request,
                                                        @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(courseService.applyDiscount(id, request, currentUser));
    }

    @DeleteMapping("/{id}/discount")
    public ResponseEntity<CourseResponse> removeDiscount(@PathVariable String id,
                                                         @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(courseService.removeDiscount(id, currentUser));
    }

    @GetMapping("/public")
    public ResponseEntity<List<PublicCourseResponse>> getPublicCourses(
            @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(courseService.getPublicCourses(limit));
    }
}