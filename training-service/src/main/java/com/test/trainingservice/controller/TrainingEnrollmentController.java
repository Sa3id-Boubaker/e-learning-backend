package com.test.trainingservice.controller;

import com.test.trainingservice.dto.*;
import com.test.trainingservice.entity.TrainingEnrollmentStatus;
import com.test.trainingservice.exception.InvalidEnrollmentStatusException;
import com.test.trainingservice.security.AuthenticatedUser;
import com.test.trainingservice.service.TrainingEnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/training-enrollments")
@RequiredArgsConstructor
public class TrainingEnrollmentController {

    private final TrainingEnrollmentService trainingEnrollmentService;

    /** ADMIN only (enforced by SecurityConfig). Idempotent: see ActivationOutcome. */
    @PostMapping
    public ResponseEntity<TrainingEnrollmentResponse> activate(
            @Valid @RequestBody TrainingEnrollmentCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin,
            @CookieValue(value = "jwt", required = false) String jwtToken) {

        TrainingEnrollmentService.ActivationOutcome outcome = trainingEnrollmentService.activate(request, admin, jwtToken);
        HttpStatus status = outcome.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(outcome.response());
    }

    /** studentId always comes from the JWT — never accepted as a query/body parameter. */
    @GetMapping("/my")
    public ResponseEntity<PageResponse<StudentEnrollmentResponse>> myEnrollments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(trainingEnrollmentService.myEnrollments(user, page, size));
    }

    @GetMapping
    public ResponseEntity<PageResponse<TrainingEnrollmentResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String trainingId,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String status,
            @CookieValue(value = "jwt", required = false) String jwtToken) {

        return ResponseEntity.ok(trainingEnrollmentService.adminList(trainingId, studentId, parseStatus(status), page, size, jwtToken));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TrainingEnrollmentResponse> revoke(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser admin,
            @CookieValue(value = "jwt", required = false) String jwtToken) {

        return ResponseEntity.ok(trainingEnrollmentService.revoke(id, admin, jwtToken));
    }

    private TrainingEnrollmentStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return TrainingEnrollmentStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidEnrollmentStatusException("Invalid status: " + status);
        }
    }

    /** ADMIN only. Revenue included — see SecurityConfig. */
    @GetMapping("/stats")
    public ResponseEntity<TrainingEnrollmentStatsResponse> getStats() {
        return ResponseEntity.ok(trainingEnrollmentService.getEnrollmentStats());
    }

    /** ADMIN only. Revenue included — see SecurityConfig. */
    @GetMapping("/stats/top-trainings")
    public ResponseEntity<List<TopTrainingResponse>> getTopTrainings(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "revenue") String sortBy) {
        return ResponseEntity.ok(trainingEnrollmentService.getTopTrainings(limit, sortBy));
    }

    /** Any authenticated role — no revenue, popularity only. See SecurityConfig. */
    @GetMapping("/stats/popular-trainings")
    public ResponseEntity<List<PopularTrainingResponse>> getPopularTrainings(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(trainingEnrollmentService.getPopularTrainings(limit));
    }

    /** FORMATEUR only — their own trainings' stats, never another instructor's. */
    @GetMapping("/stats/my-trainings")
    public ResponseEntity<MyTrainingStatsResponse> getMyTrainingStats(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(trainingEnrollmentService.getMyTrainingStats(currentUser.userId()));
    }
}