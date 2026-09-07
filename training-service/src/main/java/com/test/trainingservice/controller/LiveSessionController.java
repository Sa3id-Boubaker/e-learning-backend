package com.test.trainingservice.controller;

import com.test.trainingservice.dto.LiveSessionCreateRequest;
import com.test.trainingservice.dto.LiveSessionResponse;
import com.test.trainingservice.dto.LiveSessionUpdateRequest;
import com.test.trainingservice.security.AuthenticatedUser;
import com.test.trainingservice.service.LiveSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Two base paths on purpose:
 *  - /api/trainings/{trainingId}/sessions  → create + list, scoped to a parent Training
 *  - /api/sessions/{id}                    → read/update/delete a single session directly
 */
@RestController
@RequiredArgsConstructor
public class LiveSessionController {

    private final LiveSessionService liveSessionService;

    @PostMapping("/api/trainings/{trainingId}/sessions")
    public ResponseEntity<LiveSessionResponse> create(
            @PathVariable String trainingId,
            @Valid @RequestBody LiveSessionCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {

        LiveSessionResponse response = liveSessionService.create(trainingId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/trainings/{trainingId}/sessions")
    public ResponseEntity<List<LiveSessionResponse>> listByTraining(
            @PathVariable String trainingId,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(liveSessionService.listByTraining(trainingId, user));
    }

    @GetMapping("/api/sessions/{id}")
    public ResponseEntity<LiveSessionResponse> getById(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(liveSessionService.getById(id, user));
    }

    @PutMapping("/api/sessions/{id}")
    public ResponseEntity<LiveSessionResponse> update(
            @PathVariable String id,
            @Valid @RequestBody LiveSessionUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(liveSessionService.update(id, request, user));
    }

    @DeleteMapping("/api/sessions/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        liveSessionService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}