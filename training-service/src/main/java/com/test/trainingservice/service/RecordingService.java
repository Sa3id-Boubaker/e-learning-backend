package com.test.trainingservice.service;

import com.test.trainingservice.cloudinary.CloudinaryService;
import com.test.trainingservice.config.AppClock;
import com.test.trainingservice.dto.RecordingResponse;
import com.test.trainingservice.dto.RecordingUpdateRequest;
import com.test.trainingservice.entity.LiveSession;
import com.test.trainingservice.entity.LiveSessionStatus;
import com.test.trainingservice.entity.Recording;
import com.test.trainingservice.entity.Training;
import com.test.trainingservice.exception.LiveSessionNotFoundException;
import com.test.trainingservice.exception.NotTrainingOwnerException;
import com.test.trainingservice.exception.RecordingAlreadyExistsException;
import com.test.trainingservice.exception.RecordingNotFoundException;
import com.test.trainingservice.exception.SessionNotCompletedException;
import com.test.trainingservice.exception.TrainingNotFoundException;
import com.test.trainingservice.repository.LiveSessionRepository;
import com.test.trainingservice.repository.RecordingRepository;
import com.test.trainingservice.repository.TrainingRepository;
import com.test.trainingservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RecordingService {

    private final RecordingRepository recordingRepository;
    private final LiveSessionRepository liveSessionRepository;
    private final TrainingRepository trainingRepository;
    private final TrainingEnrollmentService trainingEnrollmentService;
    private final CloudinaryService cloudinaryService;
    private final AppClock appClock;

    public RecordingResponse create(String sessionId, MultipartFile file, AuthenticatedUser user) {
        LiveSession session = findSessionOrThrow(sessionId);
        Training training = findTrainingOrThrow(session.getTrainingId());
        assertManageAccess(training, user);

        if (session.effectiveStatus(appClock.now()) != LiveSessionStatus.COMPLETED) {
            throw new SessionNotCompletedException("Recording can only be uploaded after the live session is completed.");
        }

        // Application-level check for a fast, friendly error — the unique index on
        // Recording.sessionId is what actually prevents a duplicate under concurrent requests.
        if (recordingRepository.existsBySessionId(sessionId)) {
            throw new RecordingAlreadyExistsException("A recording already exists for this live session.");
        }

        CloudinaryService.VideoUploadResult uploaded = cloudinaryService.uploadRecordingVideo(file, sessionId);

        Recording recording = Recording.builder()
                .sessionId(sessionId)
                .title(session.getTitle() + " - Replay")
                .description(null)
                .videoUrl(uploaded.url())
                .videoPublicId(uploaded.publicId())
                .duration(uploaded.durationSeconds())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        try {
            recording = recordingRepository.save(recording);
        } catch (DuplicateKeyException e) {
            // Lost a race against a concurrent upload for the same session: the video we just
            // uploaded would otherwise be orphaned on Cloudinary, so clean it up before failing.
            cloudinaryService.deleteVideoIfExists(uploaded.publicId());
            throw new RecordingAlreadyExistsException("A recording already exists for this live session.");
        }

        return toResponse(recording);
    }

    public RecordingResponse getBySession(String sessionId, AuthenticatedUser user) {
        LiveSession session = findSessionOrThrow(sessionId);
        Training training = findTrainingOrThrow(session.getTrainingId());
        trainingEnrollmentService.assertTrainingContentAccess(training, user);

        Recording recording = recordingRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RecordingNotFoundException("No recording found for live session: " + sessionId));

        return toResponse(recording);
    }

    /**
     * NOUVEAU — GET /api/recordings/{id}. N'existait pas jusqu'ici : seul
     * GET /api/sessions/{sessionId}/recording permettait de lire un
     * enregistrement. Ajouté pour permettre à FORUM-SERVICE (ou tout autre
     * consommateur) de résoudre un recordingId directement. Même contrôle
     * d'accès que getBySession() : un ÉTUDIANT doit être inscrit (ou la
     * formation publiée) pour voir l'enregistrement, un FORMATEUR doit être
     * propriétaire, un ADMIN passe toujours.
     */
    public RecordingResponse getById(String id, AuthenticatedUser user) {
        Recording recording = findRecordingOrThrow(id);
        LiveSession session = findSessionOrThrow(recording.getSessionId());
        Training training = findTrainingOrThrow(session.getTrainingId());
        trainingEnrollmentService.assertTrainingContentAccess(training, user);

        return toResponse(recording);
    }

    public RecordingResponse updateMetadata(String id, RecordingUpdateRequest request, AuthenticatedUser user) {
        Recording recording = findRecordingOrThrow(id);
        LiveSession session = findSessionOrThrow(recording.getSessionId());
        Training training = findTrainingOrThrow(session.getTrainingId());
        assertManageAccess(training, user);

        if (request.getTitle() != null) recording.setTitle(request.getTitle());
        if (request.getDescription() != null) recording.setDescription(request.getDescription());

        recording.setUpdatedAt(LocalDateTime.now());
        recording = recordingRepository.save(recording);
        return toResponse(recording);
    }

    public RecordingResponse replaceVideo(String id, MultipartFile file, AuthenticatedUser user) {
        Recording recording = findRecordingOrThrow(id);
        LiveSession session = findSessionOrThrow(recording.getSessionId());
        Training training = findTrainingOrThrow(session.getTrainingId());
        assertManageAccess(training, user);

        // Upload the new video FIRST — if this fails, the existing recording is untouched.
        CloudinaryService.VideoUploadResult uploaded = cloudinaryService.uploadRecordingVideo(file, recording.getSessionId());
        String previousPublicId = recording.getVideoPublicId();

        recording.setVideoUrl(uploaded.url());
        recording.setVideoPublicId(uploaded.publicId());
        recording.setDuration(uploaded.durationSeconds());
        recording.setUpdatedAt(LocalDateTime.now());
        recording = recordingRepository.save(recording);

        // Only delete the old asset once the DB update has actually succeeded.
        cloudinaryService.deleteVideoIfExists(previousPublicId);

        return toResponse(recording);
    }

    public void delete(String id, AuthenticatedUser user) {
        Recording recording = findRecordingOrThrow(id);
        LiveSession session = findSessionOrThrow(recording.getSessionId());
        Training training = findTrainingOrThrow(session.getTrainingId());
        assertManageAccess(training, user);

        cloudinaryService.deleteVideoIfExists(recording.getVideoPublicId());
        recordingRepository.delete(recording);
    }

    // --- Access control — même pattern que TrainingService/LiveSessionService ---

    private void assertManageAccess(Training training, AuthenticatedUser user) {
        if (user.isAdmin()) {
            return;
        }
        if (!training.getInstructorId().equals(user.userId())) {
            throw new NotTrainingOwnerException("You do not own this training");
        }
    }

    // --- Helpers ---

    private LiveSession findSessionOrThrow(String sessionId) {
        return liveSessionRepository.findById(sessionId)
                .orElseThrow(() -> new LiveSessionNotFoundException("Live session not found: " + sessionId));
    }

    private Training findTrainingOrThrow(String trainingId) {
        return trainingRepository.findById(trainingId)
                .orElseThrow(() -> new TrainingNotFoundException("Training not found: " + trainingId));
    }

    private Recording findRecordingOrThrow(String id) {
        return recordingRepository.findById(id)
                .orElseThrow(() -> new RecordingNotFoundException("Recording not found: " + id));
    }

    private RecordingResponse toResponse(Recording recording) {
        return RecordingResponse.builder()
                .id(recording.getId())
                .sessionId(recording.getSessionId())
                .title(recording.getTitle())
                .description(recording.getDescription())
                .videoUrl(recording.getVideoUrl())
                .duration(recording.getDuration())
                .createdAt(recording.getCreatedAt())
                .updatedAt(recording.getUpdatedAt())
                .build();
    }
}