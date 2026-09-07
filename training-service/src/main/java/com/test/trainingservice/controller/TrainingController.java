package com.test.trainingservice.controller;

import com.test.trainingservice.dto.*;
import com.test.trainingservice.security.AuthenticatedUser;
import com.test.trainingservice.service.TrainingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.test.trainingservice.service.TrainingEnrollmentService;

import java.util.List;

@RestController
@RequestMapping("/api/trainings")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;
    private final TrainingEnrollmentService trainingEnrollmentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrainingResponse> create(
            @Valid @ModelAttribute TrainingCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            @CookieValue(value = "jwt", required = false) String jwtToken) {

        TrainingResponse response = trainingService.create(request, user, jwtToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TrainingResponse>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(trainingService.list(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrainingResponse> getById(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(trainingService.getById(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrainingResponse> update(
            @PathVariable String id,
            @Valid @RequestBody TrainingUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            @CookieValue(value = "jwt", required = false) String jwtToken) {

        return ResponseEntity.ok(trainingService.update(id, request, user, jwtToken));
    }

    /** Called by the frontend right before showing the delete confirmation modal. */
    @GetMapping("/{id}/deletion-impact")
    public ResponseEntity<TrainingDeletionImpactResponse> getDeletionImpact(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(trainingService.getDeletionImpact(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        trainingService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrainingResponse> replaceImage(
            @PathVariable String id,
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(trainingService.replaceImage(id, image, user));
    }

    @GetMapping("/{id}/access")
    public ResponseEntity<TrainingAccessResponse> access(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(trainingEnrollmentService.getAccess(id, user));
    }

    @GetMapping("/public")
    public ResponseEntity<List<PublicTrainingResponse>> getPublicTrainings(
            @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(trainingService.getPublicTrainings(limit));
    }


}