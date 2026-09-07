package com.test.trainingservice.controller;

import com.test.trainingservice.dto.RecordingResponse;
import com.test.trainingservice.dto.RecordingUpdateRequest;
import com.test.trainingservice.security.AuthenticatedUser;
import com.test.trainingservice.service.RecordingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Trois bases sur ce contrôleur désormais :
 *  - /api/sessions/{sessionId}/recording  → upload + fetch, scoped to the parent LiveSession
 *  - /api/recordings/{id}                 → fetch direct / update metadata / replace video / delete
 */
@RestController
@RequiredArgsConstructor
public class RecordingController {

    private final RecordingService recordingService;

    @PostMapping(value = "/api/sessions/{sessionId}/recording", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecordingResponse> create(
            @PathVariable String sessionId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser user) {

        RecordingResponse response = recordingService.create(sessionId, file, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/sessions/{sessionId}/recording")
    public ResponseEntity<RecordingResponse> getBySession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(recordingService.getBySession(sessionId, user));
    }

    /** NOUVEAU — comble le manque identifié pendant l'analyse FORUM-SERVICE. */
    @GetMapping("/api/recordings/{id}")
    public ResponseEntity<RecordingResponse> getById(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(recordingService.getById(id, user));
    }

    @PutMapping("/api/recordings/{id}")
    public ResponseEntity<RecordingResponse> updateMetadata(
            @PathVariable String id,
            @Valid @RequestBody RecordingUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(recordingService.updateMetadata(id, request, user));
    }

    @PostMapping(value = "/api/recordings/{id}/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecordingResponse> replaceVideo(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(recordingService.replaceVideo(id, file, user));
    }

    @DeleteMapping("/api/recordings/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        recordingService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}