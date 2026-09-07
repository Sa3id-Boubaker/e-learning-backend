package com.test.trainingservice.service;

import com.test.trainingservice.config.AppClock;
import com.test.trainingservice.dto.LiveSessionCreateRequest;
import com.test.trainingservice.dto.LiveSessionResponse;
import com.test.trainingservice.dto.LiveSessionUpdateRequest;
import com.test.trainingservice.entity.LiveSession;
import com.test.trainingservice.entity.Training;
import com.test.trainingservice.exception.InvalidSessionTimeRangeException;
import com.test.trainingservice.exception.LiveSessionNotFoundException;
import com.test.trainingservice.exception.NotTrainingOwnerException;
import com.test.trainingservice.exception.SessionConflictException;
import com.test.trainingservice.exception.TrainingAccessDeniedException;
import com.test.trainingservice.exception.TrainingNotFoundException;
import com.test.trainingservice.repository.LiveSessionRepository;
import com.test.trainingservice.repository.TrainingRepository;
import com.test.trainingservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LiveSessionService {

    private final LiveSessionRepository liveSessionRepository;
    private final TrainingRepository trainingRepository;
    private final TrainingEnrollmentService trainingEnrollmentService;
    private final AppClock appClock;

    public LiveSessionResponse create(String trainingId, LiveSessionCreateRequest request, AuthenticatedUser user) {
        Training training = findTrainingOrThrow(trainingId);
        assertManageAccess(training, user);

        validateTimeRange(request.getStartAt(), request.getEndAt());
        assertNoOverlap(trainingId, request.getStartAt(), request.getEndAt(), null);

        LiveSession session = LiveSession.builder()
                .trainingId(trainingId)
                .title(request.getTitle())
                .description(request.getDescription())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .meetingUrl(request.getMeetingUrl())
                .status(request.getStatus())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        session = liveSessionRepository.save(session);
        return toResponse(session, training, user);
    }

    public List<LiveSessionResponse> listByTraining(String trainingId, AuthenticatedUser user) {
        Training training = findTrainingOrThrow(trainingId);
        assertViewAccess(training, user);

        return liveSessionRepository.findByTrainingIdOrderByStartAtAsc(trainingId)
                .stream()
                .map(session -> toResponse(session, training, user))
                .toList();
    }

    public LiveSessionResponse getById(String id, AuthenticatedUser user) {
        LiveSession session = findSessionOrThrow(id);
        Training training = findTrainingOrThrow(session.getTrainingId());
        assertViewAccess(training, user);
        return toResponse(session, training, user);
    }

    public LiveSessionResponse update(String id, LiveSessionUpdateRequest request, AuthenticatedUser user) {
        LiveSession session = findSessionOrThrow(id);
        Training training = findTrainingOrThrow(session.getTrainingId());
        assertManageAccess(training, user);

        if (request.getTitle() != null) session.setTitle(request.getTitle());
        if (request.getDescription() != null) session.setDescription(request.getDescription());
        if (request.getStartAt() != null) session.setStartAt(request.getStartAt());
        if (request.getEndAt() != null) session.setEndAt(request.getEndAt());
        if (request.getMeetingUrl() != null) session.setMeetingUrl(request.getMeetingUrl());
        if (request.getStatus() != null) session.setStatus(request.getStatus());

        validateTimeRange(session.getStartAt(), session.getEndAt());
        assertNoOverlap(session.getTrainingId(), session.getStartAt(), session.getEndAt(), session.getId());

        session.setUpdatedAt(LocalDateTime.now());
        session = liveSessionRepository.save(session);
        return toResponse(session, training, user);
    }

    public void delete(String id, AuthenticatedUser user) {
        LiveSession session = findSessionOrThrow(id);
        Training training = findTrainingOrThrow(session.getTrainingId());
        assertManageAccess(training, user);

        liveSessionRepository.delete(session);
    }

    // --- Access control — mirrors TrainingService's assertOwnership()/assertViewAccess() ---

    private void assertManageAccess(Training training, AuthenticatedUser user) {
        if (user.isAdmin()) {
            return;
        }
        if (!training.getInstructorId().equals(user.userId())) {
            throw new NotTrainingOwnerException("You do not own this training");
        }
    }

    private void assertViewAccess(Training training, AuthenticatedUser user) {
        if (user.isAdmin()) {
            return;
        }
        if (user.isFormateur()) {
            if (!training.getInstructorId().equals(user.userId())) {
                throw new TrainingAccessDeniedException("You cannot view another instructor's sessions");
            }
            return;
        }
        // ETUDIANT — session existence/public metadata (title, description, startAt, endAt,
        // status) stays visible to any student once the training is published, exactly as
        // before. Private content (meetingUrl) is gated separately in toResponse() below, based
        // on TrainingEnrollment — this method only controls whether the session is visible at all.
        if (!training.isPublishedOrBeyond()) {
            throw new TrainingAccessDeniedException("This training is not published");
        }
    }

    // --- Helpers ---

    private Training findTrainingOrThrow(String trainingId) {
        return trainingRepository.findById(trainingId)
                .orElseThrow(() -> new TrainingNotFoundException("Training not found: " + trainingId));
    }

    private LiveSession findSessionOrThrow(String id) {
        return liveSessionRepository.findById(id)
                .orElseThrow(() -> new LiveSessionNotFoundException("Live session not found: " + id));
    }

    private void validateTimeRange(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && !endAt.isAfter(startAt)) {
            throw new InvalidSessionTimeRangeException("End date/time must be after start date/time");
        }
    }

    /**
     * Rejects a session whose time window overlaps another session of the SAME training.
     * excludeSessionId is passed on update so a session doesn't conflict with its own
     * previous version.
     */
    private void assertNoOverlap(String trainingId, LocalDateTime newStart, LocalDateTime newEnd, String excludeSessionId) {
        List<LiveSession> existing = liveSessionRepository.findByTrainingId(trainingId);

        boolean conflict = existing.stream()
                .filter(s -> excludeSessionId == null || !s.getId().equals(excludeSessionId))
                .anyMatch(s -> newStart.isBefore(s.getEndAt()) && newEnd.isAfter(s.getStartAt()));

        if (conflict) {
            throw new SessionConflictException("Another session already exists during this time period.");
        }
    }

    /**
     * meetingUrl is only included when the caller has actual content access to the training
     * (ADMIN, the owning FORMATEUR, or a student with an ACTIVE TrainingEnrollment) — see
     * TrainingEnrollmentService.hasTrainingContentAccess(). Everyone who reaches this method has
     * already passed assertViewAccess()/assertManageAccess() above, so title/description/
     * startAt/endAt/status stay visible to any authenticated viewer allowed to see the session at
     * all; only the private meetingUrl field is conditionally stripped.
     */
    private LiveSessionResponse toResponse(LiveSession session, Training training, AuthenticatedUser user) {
        boolean contentAccess = trainingEnrollmentService.hasTrainingContentAccess(training, user);

        return LiveSessionResponse.builder()
                .id(session.getId())
                .trainingId(session.getTrainingId())
                .title(session.getTitle())
                .description(session.getDescription())
                .startAt(session.getStartAt())
                .endAt(session.getEndAt())
                .meetingUrl(contentAccess ? session.getMeetingUrl() : null)
                .status(session.effectiveStatus(appClock.now()))
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}