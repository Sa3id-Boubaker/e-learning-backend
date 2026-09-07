package com.test.trainingservice.service;

import com.test.trainingservice.client.UserBasicInfoResponse;
import com.test.trainingservice.client.UserServiceClient;
import com.test.trainingservice.cloudinary.CloudinaryService;
import com.test.trainingservice.config.AppClock;
import com.test.trainingservice.dto.*;
import com.test.trainingservice.entity.LiveSession;
import com.test.trainingservice.entity.Recording;
import com.test.trainingservice.entity.Training;
import com.test.trainingservice.entity.TrainingEnrollmentStatus;
import com.test.trainingservice.entity.TrainingStatus;
import com.test.trainingservice.exception.InstructorNotFoundException;
import com.test.trainingservice.exception.InstructorRequiredException;
import com.test.trainingservice.exception.InvalidInstructorRoleException;
import com.test.trainingservice.exception.InvalidTrainingDateRangeException;
import com.test.trainingservice.exception.NotTrainingOwnerException;
import com.test.trainingservice.exception.TrainingAccessDeniedException;
import com.test.trainingservice.exception.TrainingNotFoundException;
import com.test.trainingservice.repository.LiveSessionRepository;
import com.test.trainingservice.repository.RecordingRepository;
import com.test.trainingservice.repository.TrainingEnrollmentRepository;
import com.test.trainingservice.repository.TrainingRepository;
import com.test.trainingservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingRepository trainingRepository;
    private final LiveSessionRepository liveSessionRepository;
    private final RecordingRepository recordingRepository;
    private final TrainingEnrollmentRepository trainingEnrollmentRepository;
    private final UserServiceClient userServiceClient;
    private final CloudinaryService cloudinaryService;
    private final AppClock appClock;

    public TrainingResponse create(TrainingCreateRequest request, AuthenticatedUser user, String jwtToken) {
        validateDateRange(request.getStartDate(), request.getEndDate());

        String createdBy = user.userId();
        String instructorId = resolveInstructorForCreate(request.getInstructorId(), user, jwtToken);

        Training training = Training.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .discountPercentage(request.getDiscountPercentage() != null ? request.getDiscountPercentage() : BigDecimal.ZERO)
                .createdBy(createdBy)
                .instructorId(instructorId)
                .status(request.getStatus() != null ? request.getStatus() : TrainingStatus.DRAFT)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            CloudinaryService.UploadResult uploaded = cloudinaryService.uploadTrainingImage(request.getImage(), user.userId());
            training.setImage(uploaded.url());
            training.setImagePublicId(uploaded.publicId());
        }

        training = trainingRepository.save(training);
        return toResponse(training);
    }

    public List<TrainingResponse> list(AuthenticatedUser user) {
        List<Training> trainings;

        if (user.isAdmin()) {
            trainings = trainingRepository.findAll();
        } else if (user.isFormateur()) {
            trainings = trainingRepository.findByInstructorId(user.userId());
        } else {
            trainings = trainingRepository.findByStatusNotIn(List.of(TrainingStatus.DRAFT, TrainingStatus.CANCELLED));
        }

        return trainings.stream().map(this::toResponse).toList();
    }

    public TrainingResponse getById(String id, AuthenticatedUser user) {
        Training training = findOrThrow(id);
        assertViewAccess(training, user);
        return toResponse(training);
    }

    public TrainingResponse update(String id, TrainingUpdateRequest request, AuthenticatedUser user, String jwtToken) {
        Training training = findOrThrow(id);
        assertOwnership(training, user);

        if (request.getInstructorId() != null) {
            if (!user.isAdmin()) {
                throw new TrainingAccessDeniedException("Only an administrator can reassign a training's instructor");
            }
            training.setInstructorId(validateInstructor(request.getInstructorId(), jwtToken));
        }

        if (request.getTitle() != null) training.setTitle(request.getTitle());
        if (request.getDescription() != null) training.setDescription(request.getDescription());
        if (request.getPrice() != null) training.setPrice(request.getPrice());
        if (request.getDiscountPercentage() != null) training.setDiscountPercentage(request.getDiscountPercentage());
        if (request.getStatus() != null) training.setStatus(request.getStatus());
        if (request.getStartDate() != null) training.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) training.setEndDate(request.getEndDate());

        validateDateRange(training.getStartDate(), training.getEndDate());

        training.setUpdatedAt(LocalDateTime.now());
        training = trainingRepository.save(training);
        return toResponse(training);
    }

    /**
     * Deletion impact preview — called by the frontend right before it shows the delete
     * confirmation modal, so the admin sees exactly how many sessions/recordings/enrollments
     * are about to be wiped out before confirming a cascade delete.
     */
    public TrainingDeletionImpactResponse getDeletionImpact(String id, AuthenticatedUser user) {
        Training training = findOrThrow(id);
        assertOwnership(training, user);

        Set<String> sessionIds = liveSessionRepository.findByTrainingId(training.getId()).stream()
                .map(LiveSession::getId)
                .collect(Collectors.toSet());

        long liveSessionCount = sessionIds.size();
        long recordingCount = sessionIds.isEmpty() ? 0 : recordingRepository.countBySessionIdIn(sessionIds);
        long totalEnrollmentCount = trainingEnrollmentRepository.countByTrainingId(training.getId());
        long activeEnrollmentCount = trainingEnrollmentRepository
                .countByTrainingIdAndStatus(training.getId(), TrainingEnrollmentStatus.ACTIVE);

        return TrainingDeletionImpactResponse.builder()
                .trainingId(training.getId())
                .liveSessionCount(liveSessionCount)
                .recordingCount(recordingCount)
                .totalEnrollmentCount(totalEnrollmentCount)
                .activeEnrollmentCount(activeEnrollmentCount)
                .build();
    }

    /**
     * Cascade delete: wipes out every LiveSession, Recording (including their Cloudinary video
     * assets) and TrainingEnrollment attached to this training, then the training itself. The
     * frontend is expected to have shown the admin the exact counts via getDeletionImpact()
     * beforehand — this method itself does not ask for confirmation again.
     */
    public void delete(String id, AuthenticatedUser user) {
        Training training = findOrThrow(id);
        assertOwnership(training, user);

        List<LiveSession> sessions = liveSessionRepository.findByTrainingId(training.getId());
        Set<String> sessionIds = sessions.stream().map(LiveSession::getId).collect(Collectors.toSet());

        if (!sessionIds.isEmpty()) {
            List<Recording> recordings = recordingRepository.findAllBySessionIdIn(sessionIds);
            for (Recording recording : recordings) {
                cloudinaryService.deleteVideoIfExists(recording.getVideoPublicId());
            }
            recordingRepository.deleteBySessionIdIn(sessionIds);
        }

        liveSessionRepository.deleteByTrainingId(training.getId());
        trainingEnrollmentRepository.deleteByTrainingId(training.getId());

        cloudinaryService.deleteIfExists(training.getImagePublicId());
        trainingRepository.delete(training);
    }

    public TrainingResponse replaceImage(String id, MultipartFile image, AuthenticatedUser user) {
        Training training = findOrThrow(id);
        assertOwnership(training, user);

        CloudinaryService.UploadResult uploaded = cloudinaryService.uploadTrainingImage(image, user.userId());
        String previousPublicId = training.getImagePublicId();

        training.setImage(uploaded.url());
        training.setImagePublicId(uploaded.publicId());
        training.setUpdatedAt(LocalDateTime.now());
        training = trainingRepository.save(training);

        cloudinaryService.deleteIfExists(previousPublicId);

        return toResponse(training);
    }

    // --- Contrôles d'accès, calqués sur Course Service ---

    private void assertOwnership(Training training, AuthenticatedUser user) {
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
                throw new TrainingAccessDeniedException("You cannot view another instructor's training");
            }
            return;
        }
        // ETUDIANT
        if (!training.isPublishedOrBeyond()) {
            throw new TrainingAccessDeniedException("This training is not published");
        }
    }

    // --- Instructor assignment ---

    /**
     * FORMATEUR: always assigned to themselves — any instructorId they might have sent is
     * intentionally ignored, a FORMATEUR can never create a training "for" someone else.
     * ADMIN: must supply an instructorId, verified against USER-SERVICE to actually be a
     * FORMATEUR before it is accepted. SecurityConfig only allows ADMIN/FORMATEUR on this
     * endpoint, so no other role reaches this method.
     */
    private String resolveInstructorForCreate(String requestedInstructorId, AuthenticatedUser user, String jwtToken) {
        if (user.isFormateur()) {
            return user.userId();
        }
        if (requestedInstructorId == null || requestedInstructorId.isBlank()) {
            throw new InstructorRequiredException("An administrator must assign a FORMATEUR to create a training");
        }
        return validateInstructor(requestedInstructorId, jwtToken);
    }

    /** Same strict lookup pattern as TrainingEnrollmentService.activate() for students. */
    private String validateInstructor(String instructorId, String jwtToken) {
        UserBasicInfoResponse instructor = userServiceClient.getBasicInfo(instructorId, jwtToken)
                .orElseThrow(() -> new InstructorNotFoundException("Instructor not found: " + instructorId));

        if (!"FORMATEUR".equals(instructor.getRole())) {
            throw new InvalidInstructorRoleException(
                    "User " + instructorId + " is not a formateur (role=" + instructor.getRole() + ")");
        }

        return instructor.getId();
    }

    // --- Aides ---

    private Training findOrThrow(String id) {
        return trainingRepository.findById(id)
                .orElseThrow(() -> new TrainingNotFoundException("Training not found: " + id));
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidTrainingDateRangeException("End date must not be before start date");
        }
    }

    /**
     * finalPrice n'est jamais stocké — toujours recalculé à la lecture. Public (was private)
     * so TrainingEnrollmentService can reuse this exact logic for amountAtEnrollment instead
     * of duplicating the discount math.
     */
    public BigDecimal computeFinalPrice(BigDecimal price, BigDecimal discountPercentage) {
        if (price == null) {
            return null;
        }
        if (discountPercentage == null || discountPercentage.signum() == 0) {
            return price.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal discountAmount = price.multiply(discountPercentage)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        return price.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
    }

    private TrainingResponse toResponse(Training training) {
        return TrainingResponse.builder()
                .id(training.getId())
                .title(training.getTitle())
                .description(training.getDescription())
                .price(training.getPrice())
                .discountPercentage(training.getDiscountPercentage())
                .finalPrice(computeFinalPrice(training.getPrice(), training.getDiscountPercentage()))
                .image(training.getImage())
                .createdBy(training.getCreatedBy())
                .instructorId(training.getInstructorId())
                .status(training.effectiveStatus(LocalDate.now(appClock.zone())))
                .startDate(training.getStartDate())
                .endDate(training.getEndDate())
                .createdAt(training.getCreatedAt())
                .updatedAt(training.getUpdatedAt())
                .build();
    }

    public List<PublicTrainingResponse> getPublicTrainings(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);

        return trainingRepository.findByStatusNotIn(List.of(TrainingStatus.DRAFT, TrainingStatus.CANCELLED)).stream()
                .sorted(Comparator.comparing(Training::getCreatedAt).reversed())
                .limit(safeLimit)
                .map(training -> PublicTrainingResponse.builder()
                        .id(training.getId())
                        .title(training.getTitle())
                        .image(training.getImage())
                        .price(training.getPrice())
                        .finalPrice(computeFinalPrice(training.getPrice(), training.getDiscountPercentage()))
                        .startDate(training.getStartDate())
                        .build())
                .toList();
    }

}