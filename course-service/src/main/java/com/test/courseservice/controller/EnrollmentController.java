package com.test.courseservice.controller;

import com.test.courseservice.dto.*;
import com.test.courseservice.security.AuthenticatedUser;
import com.test.courseservice.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.test.courseservice.model.EnrollmentStatus;
import com.test.courseservice.exception.InvalidEnrollmentStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/api/enrollments")
    public ResponseEntity<EnrollmentResponse> createEnrollment(@Valid @RequestBody EnrollmentCreateRequest request,
                                                               @AuthenticationPrincipal AuthenticatedUser currentUser) {
        EnrollmentResponse response = enrollmentService.createEnrollment(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/enrollments")
    public ResponseEntity<PageResponse<EnrollmentResponse>> listAllEnrollments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(enrollmentService.listAllEnrollments(courseId, studentId, parseStatus(status), page, size, currentUser));
    }

    private EnrollmentStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return EnrollmentStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidEnrollmentStatusException("Invalid status: " + status);
        }
    }

    @GetMapping("/api/enrollments/my")
    public ResponseEntity<PageResponse<EnrollmentResponse>> listMyEnrollments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(enrollmentService.listMyEnrollments(page, size, currentUser));
    }

    @DeleteMapping("/api/enrollments/{enrollmentId}")
    public ResponseEntity<Void> revokeEnrollment(@PathVariable String enrollmentId) {
        enrollmentService.revokeEnrollment(enrollmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/courses/{courseId}/enrollments")
    public ResponseEntity<PageResponse<EnrollmentResponse>> listCourseEnrollments(
            @PathVariable String courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(enrollmentService.listCourseEnrollments(courseId, page, size, currentUser));
    }

    @GetMapping("/api/courses/{courseId}/access")
    public ResponseEntity<CourseAccessResponse> checkAccess(@PathVariable String courseId,
                                                            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(enrollmentService.checkAccess(courseId, currentUser));
    }

    @GetMapping("/api/enrollments/my/courses")
    public ResponseEntity<PageResponse<MyCourseResponse>> listMyCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(enrollmentService.listMyCourses(page, size, currentUser));
    }

    @GetMapping("/api/enrollments/stats")
    public ResponseEntity<EnrollmentStatsResponse> getStats() {
        return ResponseEntity.ok(enrollmentService.getEnrollmentStats());
    }

    @GetMapping("/api/enrollments/stats/top-courses")
    public ResponseEntity<List<TopCourseResponse>> getTopCourses(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "revenue") String sortBy) {
        return ResponseEntity.ok(enrollmentService.getTopCourses(limit, sortBy));
    }

    /** FORMATEUR only — their own courses' stats, never another instructor's. */
    @GetMapping("/api/enrollments/stats/my-courses")
    public ResponseEntity<MyCourseStatsResponse> getMyCourseStats(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(enrollmentService.getMyCourseStats(currentUser.userId()));
    }
}