package com.test.trainingservice.service;

import com.test.trainingservice.client.UserBasicInfoResponse;
import com.test.trainingservice.client.UserServiceClient;
import com.test.trainingservice.config.AppClock;
import com.test.trainingservice.dto.*;
import com.test.trainingservice.event.TrainingEnrollmentEventPublisher;
import com.test.trainingservice.entity.Training;
import com.test.trainingservice.entity.TrainingEnrollment;
import com.test.trainingservice.entity.TrainingEnrollmentStatus;
import com.test.trainingservice.entity.TrainingStatus;
import com.test.trainingservice.exception.InvalidStudentRoleException;
import com.test.trainingservice.exception.StudentNotFoundException;
import com.test.trainingservice.exception.TrainingEnrollmentNotFoundException;
import com.test.trainingservice.exception.TrainingAccessDeniedException;
import com.test.trainingservice.exception.TrainingNotEligibleForEnrollmentException;
import com.test.trainingservice.exception.TrainingNotFoundException;
import com.test.trainingservice.repository.TrainingEnrollmentRepository;
import com.test.trainingservice.repository.TrainingRepository;
import com.test.trainingservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.DateOperators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Single source of truth for "does this student currently have access to this Training" —
 * LiveSession/Recording/other student-only Training features will later call
 * assertActiveTrainingEnrollment()/hasActiveTrainingEnrollment() instead of duplicating this
 * check (see FUTURE INTEGRATION note on those two methods).
 */
@Service
@RequiredArgsConstructor
public class TrainingEnrollmentService {

    private final TrainingEnrollmentRepository trainingEnrollmentRepository;
    private final TrainingRepository trainingRepository;
    private final TrainingService trainingService;
    private final UserServiceClient userServiceClient;
    private final TrainingEnrollmentEventPublisher trainingEnrollmentEventPublisher;
    private final MongoTemplate mongoTemplate;
    private final AppClock appClock;

    /** Returned by activate() so the controller can pick 201 vs 200 without re-deriving it. */
    public record ActivationOutcome(TrainingEnrollmentResponse response, boolean created) {
    }

    public ActivationOutcome activate(TrainingEnrollmentCreateRequest request, AuthenticatedUser admin, String jwtToken) {
        Training training = trainingRepository.findById(request.getTrainingId())
                .orElseThrow(() -> new TrainingNotFoundException("Training not found: " + request.getTrainingId()));

        assertEligibleForEnrollment(training);

        UserBasicInfoResponse student = userServiceClient.getBasicInfo(request.getStudentId(), jwtToken)
                .orElseThrow(() -> new StudentNotFoundException("Student not found: " + request.getStudentId()));

        if (!"ETUDIANT".equals(student.getRole())) {
            throw new InvalidStudentRoleException(
                    "User " + request.getStudentId() + " is not a student (role=" + student.getRole() + ")");
        }

        BigDecimal finalPrice = trainingService.computeFinalPrice(training.getPrice(), training.getDiscountPercentage());
        LocalDateTime now = appClock.now();

        Optional<TrainingEnrollment> existing =
                trainingEnrollmentRepository.findByStudentIdAndTrainingId(request.getStudentId(), request.getTrainingId());

        boolean created = false;
        // Tracks whether THIS call is the one that actually flipped the enrollment to ACTIVE —
        // the RabbitMQ event must fire exactly once per real activation, not on every call.
        boolean justActivated = false;
        TrainingEnrollment enrollment;

        if (existing.isPresent()) {
            enrollment = existing.get();

            if (enrollment.getStatus() == TrainingEnrollmentStatus.ACTIVE) {
                // Idempotent: repeated activation of an already-ACTIVE enrollment is a no-op —
                // no event published.
                return new ActivationOutcome(toResponse(enrollment, training.getTitle(), student), false);
            }

            // REVOKED -> reactivate. Same document, same id, enrolledAt untouched.
            enrollment.setStatus(TrainingEnrollmentStatus.ACTIVE);
            enrollment.setActivatedAt(now);
            enrollment.setRevokedAt(null);
            enrollment.setAmountAtEnrollment(finalPrice);
            enrollment.setUpdatedAt(now);
            justActivated = true;
        } else {
            created = true;
            justActivated = true;
            enrollment = TrainingEnrollment.builder()
                    .trainingId(request.getTrainingId())
                    .studentId(request.getStudentId())
                    .amountAtEnrollment(finalPrice)
                    .status(TrainingEnrollmentStatus.ACTIVE)
                    .enrolledAt(now)
                    .activatedAt(now)
                    .revokedAt(null)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        }

        try {
            enrollment = trainingEnrollmentRepository.save(enrollment);
        } catch (DuplicateKeyException e) {
            // Lost a race against a concurrent activation for the same student+training — the
            // unique index is the final protection. Return the winner's document instead of
            // failing, so activation stays idempotent even under concurrency. The winning
            // request is the one that actually persisted and published — this thread must NOT
            // publish a second event for the same activation.
            created = false;
            justActivated = false;
            enrollment = trainingEnrollmentRepository
                    .findByStudentIdAndTrainingId(request.getStudentId(), request.getTrainingId())
                    .orElseThrow(() -> e);
        }

        // Publish only after the enrollment is durably ACTIVE in MongoDB, and only for the call
        // that actually caused the transition (never on the already-ACTIVE no-op, never on the
        // losing side of a concurrent-activation race).
        if (justActivated) {
            trainingEnrollmentEventPublisher.publishTrainingEnrollmentActivated(
                    enrollment.getId(), enrollment.getStudentId(), enrollment.getTrainingId(),
                    training.getTitle(), enrollment.getActivatedAt());
        }

        return new ActivationOutcome(toResponse(enrollment, training.getTitle(), student), created);
    }

    public TrainingEnrollmentResponse revoke(String id, AuthenticatedUser admin, String jwtToken) {
        TrainingEnrollment enrollment = trainingEnrollmentRepository.findById(id)
                .orElseThrow(() -> new TrainingEnrollmentNotFoundException("Enrollment not found: " + id));

        if (enrollment.getStatus() != TrainingEnrollmentStatus.REVOKED) {
            LocalDateTime now = appClock.now();
            enrollment.setStatus(TrainingEnrollmentStatus.REVOKED);
            enrollment.setRevokedAt(now);
            enrollment.setUpdatedAt(now);
            enrollment = trainingEnrollmentRepository.save(enrollment);
        }
        // else: already REVOKED — idempotent, just return it as-is.

        String trainingTitle = trainingRepository.findById(enrollment.getTrainingId())
                .map(Training::getTitle)
                .orElse(null);
        return toResponse(enrollment, trainingTitle, jwtToken);
    }

    public PageResponse<StudentEnrollmentResponse> myEnrollments(AuthenticatedUser user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "activatedAt"));
        var result = trainingEnrollmentRepository
                .findByStudentIdAndStatus(user.userId(), TrainingEnrollmentStatus.ACTIVE, pageable);

        Set<String> trainingIds = result.getContent().stream()
                .map(TrainingEnrollment::getTrainingId)
                .collect(Collectors.toSet());
        Map<String, Training> trainingsById = trainingRepository.findAllById(trainingIds).stream()
                .collect(Collectors.toMap(Training::getId, t -> t));

        List<StudentEnrollmentResponse> content = result.getContent().stream()
                .map(e -> {
                    Training training = trainingsById.get(e.getTrainingId());
                    return StudentEnrollmentResponse.builder()
                            .enrollmentId(e.getId())
                            .trainingId(e.getTrainingId())
                            .trainingTitle(training != null ? training.getTitle() : null)
                            .trainingImage(training != null ? training.getImage() : null)
                            .amountAtEnrollment(e.getAmountAtEnrollment())
                            .status(e.getStatus())
                            .activatedAt(e.getActivatedAt())
                            .startDate(training != null ? training.getStartDate() : null)
                            .endDate(training != null ? training.getEndDate() : null)
                            .build();
                })
                .toList();

        return PageResponse.<StudentEnrollmentResponse>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /**
     * Admin search over up to 3 independent optional filters (trainingId/studentId/status).
     * Built directly with MongoTemplate/Criteria rather than a combinatorial set of derived
     * repository methods — the standard way to handle a variable filter set in Spring Data
     * MongoDB. Still fully DB-level: filtering, sorting and pagination all happen in Mongo.
     */
    public PageResponse<TrainingEnrollmentResponse> adminList(String trainingId, String studentId,
                                                              TrainingEnrollmentStatus status,
                                                              int page, int size, String jwtToken) {
        List<Criteria> criteria = new ArrayList<>();
        if (trainingId != null && !trainingId.isBlank()) {
            criteria.add(Criteria.where("trainingId").is(trainingId));
        }
        if (studentId != null && !studentId.isBlank()) {
            criteria.add(Criteria.where("studentId").is(studentId));
        }
        if (status != null) {
            criteria.add(Criteria.where("status").is(status));
        }
        Criteria combined = criteria.isEmpty()
                ? new Criteria()
                : new Criteria().andOperator(criteria.toArray(new Criteria[0]));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Query pagedQuery = Query.query(combined).with(pageable);
        Query countQuery = Query.query(combined);

        List<TrainingEnrollment> results = mongoTemplate.find(pagedQuery, TrainingEnrollment.class);
        long totalElements = mongoTemplate.count(countQuery, TrainingEnrollment.class);

        Set<String> trainingIds = results.stream().map(TrainingEnrollment::getTrainingId).collect(Collectors.toSet());
        Map<String, String> titlesById = trainingRepository.findAllById(trainingIds).stream()
                .collect(Collectors.toMap(Training::getId, Training::getTitle));

        List<TrainingEnrollmentResponse> content = results.stream()
                .map(e -> toResponse(e, titlesById.get(e.getTrainingId()), jwtToken))
                .toList();

        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        return PageResponse.<TrainingEnrollmentResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    public TrainingAccessResponse getAccess(String trainingId, AuthenticatedUser user) {
        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new TrainingNotFoundException("Training not found: " + trainingId));

        if (user.isAdmin()) {
            return TrainingAccessResponse.builder().trainingId(trainingId).enrolled(true).status(null).build();
        }

        if (user.isFormateur()) {
            boolean owns = training.getInstructorId().equals(user.userId());
            return TrainingAccessResponse.builder().trainingId(trainingId).enrolled(owns).status(null).build();
        }

        // ETUDIANT
        return trainingEnrollmentRepository.findByStudentIdAndTrainingId(user.userId(), trainingId)
                .filter(e -> e.getStatus() == TrainingEnrollmentStatus.ACTIVE)
                .map(e -> TrainingAccessResponse.builder().trainingId(trainingId).enrolled(true).status(e.getStatus()).build())
                .orElseGet(() -> TrainingAccessResponse.builder().trainingId(trainingId).enrolled(false).status(null).build());
    }

    // --- Reusable access check, now consumed by LiveSessionService (meetingUrl visibility),
    // RecordingService (full recording access gate) and CalendarService (meetingUrl visibility)
    // — see hasTrainingContentAccess()/assertTrainingContentAccess() below for the full
    // ADMIN/owning-FORMATEUR/ACTIVE-enrolled-ETUDIANT rule built on top of these two. ---

    public boolean hasActiveTrainingEnrollment(String studentId, String trainingId) {
        return trainingEnrollmentRepository
                .existsByStudentIdAndTrainingIdAndStatus(studentId, trainingId, TrainingEnrollmentStatus.ACTIVE);
    }

    public void assertActiveTrainingEnrollment(String studentId, String trainingId) {
        if (!hasActiveTrainingEnrollment(studentId, trainingId)) {
            throw new TrainingAccessDeniedException("You do not have an active enrollment for this training");
        }
    }

    /**
     * The single access rule for private Training content (LiveSession.meetingUrl, Recording
     * video) — mirrors getAccess()'s ADMIN/owning-FORMATEUR/ACTIVE-enrolled-ETUDIANT logic, but
     * takes an already-loaded Training (the caller already has it, e.g. from findTrainingOrThrow())
     * instead of a trainingId, and returns a plain boolean instead of a response DTO. Built on
     * top of hasActiveTrainingEnrollment() above rather than duplicating the enrollment check.
     */
    public boolean hasTrainingContentAccess(Training training, AuthenticatedUser user) {
        if (user.isAdmin()) {
            return true;
        }
        if (user.isFormateur()) {
            return training.getInstructorId().equals(user.userId());
        }
        // ETUDIANT
        return hasActiveTrainingEnrollment(user.userId(), training.getId());
    }

    /**
     * Same rule as hasTrainingContentAccess(), throwing instead of returning false — used where
     * lacking content access must block the whole response (RecordingService.getBySession()),
     * rather than just hiding one field.
     */
    public void assertTrainingContentAccess(Training training, AuthenticatedUser user) {
        if (!hasTrainingContentAccess(training, user)) {
            throw new TrainingAccessDeniedException("You are not enrolled in this training.");
        }
    }

    // --- Helpers ---

    /** PUBLISHED/IN_PROGRESS only — computed status, not the raw stored field (see report). */
    private void assertEligibleForEnrollment(Training training) {
        TrainingStatus effective = training.effectiveStatus(LocalDate.now(appClock.zone()));
        if (effective != TrainingStatus.PUBLISHED && effective != TrainingStatus.IN_PROGRESS) {
            throw new TrainingNotEligibleForEnrollmentException(
                    "Training is not eligible for enrollment (status: " + effective + ")");
        }
    }

    /** Reuses an already-fetched student (e.g. from activate()'s strict lookup) — no extra call. */
    private TrainingEnrollmentResponse toResponse(TrainingEnrollment enrollment, String trainingTitle, UserBasicInfoResponse student) {
        TrainingEnrollmentResponse.TrainingEnrollmentResponseBuilder builder = TrainingEnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudentId())
                .trainingId(enrollment.getTrainingId())
                .trainingTitle(trainingTitle)
                .amountAtEnrollment(enrollment.getAmountAtEnrollment())
                .status(enrollment.getStatus())
                .enrolledAt(enrollment.getEnrolledAt())
                .activatedAt(enrollment.getActivatedAt())
                .revokedAt(enrollment.getRevokedAt())
                .createdAt(enrollment.getCreatedAt())
                .updatedAt(enrollment.getUpdatedAt());

        if (student != null) {
            builder.studentName((student.getFirstName() + " " + student.getLastName()).trim());
            builder.studentEmail(student.getEmail());
        }

        return builder.build();
    }

    /** Fail-soft lookup variant — for revoke()/adminList(), which don't already have the
     * student object in hand (see UserServiceClient.getBasicInfoOrNull). */
    private TrainingEnrollmentResponse toResponse(TrainingEnrollment enrollment, String trainingTitle, String jwtToken) {
        UserBasicInfoResponse student = userServiceClient.getBasicInfoOrNull(enrollment.getStudentId(), jwtToken);
        return toResponse(enrollment, trainingTitle, student);
    }

    /**
     * Total revenue/count + last-7-days breakdown, for the admin dashboard "Revenue" stat card
     * and its charts. Uses appClock (not raw LocalDate.now()) for consistency with the rest of
     * this service. REVOKED enrollments still count — same convention as course-service.
     */
    public TrainingEnrollmentStatsResponse getEnrollmentStats() {
        Aggregation totalAgg = Aggregation.newAggregation(
                Aggregation.group()
                        .sum("amountAtEnrollment").as("totalRevenue")
                        .count().as("totalCount")
        );
        Document totalDoc = mongoTemplate.aggregate(totalAgg, TrainingEnrollment.class, Document.class).getUniqueMappedResult();

        BigDecimal totalRevenue = totalDoc != null ? toBigDecimal(totalDoc.get("totalRevenue")) : BigDecimal.ZERO;
        long totalCount = totalDoc != null ? toLong(totalDoc.get("totalCount")) : 0L;

        LocalDate today = LocalDate.now(appClock.zone());
        LocalDate sevenDaysAgo = today.minusDays(6);
        LocalDateTime windowStart = sevenDaysAgo.atStartOfDay();

        Aggregation dailyAgg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("createdAt").gte(windowStart)),
                Aggregation.project()
                        .and(DateOperators.DateToString.dateOf("createdAt").toString("%Y-%m-%d")).as("day")
                        .and("amountAtEnrollment").as("amountAtEnrollment"),
                Aggregation.group("day")
                        .sum("amountAtEnrollment").as("revenue")
                        .count().as("count")
        );
        List<Document> dailyResults = mongoTemplate.aggregate(dailyAgg, TrainingEnrollment.class, Document.class).getMappedResults();

        Map<String, Document> byDate = new HashMap<>();
        for (Document doc : dailyResults) {
            byDate.put(doc.getString("_id"), doc);
        }

        List<TrainingEnrollmentDailyStat> dailyStats = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = sevenDaysAgo.plusDays(i);
            String key = day.toString();
            Document doc = byDate.get(key);
            dailyStats.add(TrainingEnrollmentDailyStat.builder()
                    .date(key)
                    .revenue(doc != null ? toBigDecimal(doc.get("revenue")) : BigDecimal.ZERO)
                    .count(doc != null ? toLong(doc.get("count")) : 0L)
                    .build());
        }

        return TrainingEnrollmentStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalCount(totalCount)
                .dailyStats(dailyStats)
                .build();
    }

    /**
     * Top trainings ranked by revenue or enrollment count — ADMIN only, includes revenue.
     * Unlike course-service's Enrollment, TrainingEnrollment does NOT denormalize the training
     * title onto the document — only trainingId is stored — so after grouping in Mongo, titles
     * are batch-fetched from TrainingRepository, same pattern as adminList()/myEnrollments().
     */
    public List<TopTrainingResponse> getTopTrainings(int limit, String sortBy) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        String sortField = "count".equalsIgnoreCase(sortBy) ? "enrollmentCount" : "revenue";

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group("trainingId")
                        .sum("amountAtEnrollment").as("revenue")
                        .count().as("enrollmentCount"),
                Aggregation.sort(Sort.Direction.DESC, sortField),
                Aggregation.limit(safeLimit)
        );

        List<Document> results = mongoTemplate.aggregate(aggregation, TrainingEnrollment.class, Document.class).getMappedResults();

        List<String> trainingIds = results.stream().map(doc -> doc.getString("_id")).toList();
        Map<String, String> titlesById = trainingRepository.findAllById(trainingIds).stream()
                .collect(Collectors.toMap(Training::getId, Training::getTitle));

        return results.stream()
                .map(doc -> {
                    String trainingId = doc.getString("_id");
                    return TopTrainingResponse.builder()
                            .trainingId(trainingId)
                            .trainingTitle(titlesById.get(trainingId))
                            .enrollmentCount(toLong(doc.get("enrollmentCount")))
                            .revenue(toBigDecimal(doc.get("revenue")))
                            .build();
                })
                .toList();
    }

    /**
     * Public-facing popularity ranking (any authenticated role) for the student "Recommended
     * Trainings" widget — same $group-by-trainingId + count as getTopTrainings(), trimmed to
     * id/title/count only. No revenue, no admin restriction. REVOKED still counts, same rule
     * as everywhere else this service counts enrollments.
     */
    public List<PopularTrainingResponse> getPopularTrainings(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group("trainingId")
                        .count().as("enrollmentCount"),
                Aggregation.sort(Sort.Direction.DESC, "enrollmentCount"),
                Aggregation.limit(safeLimit)
        );

        List<Document> results = mongoTemplate.aggregate(aggregation, TrainingEnrollment.class, Document.class).getMappedResults();

        List<String> trainingIds = results.stream().map(doc -> doc.getString("_id")).toList();
        Map<String, String> titlesById = trainingRepository.findAllById(trainingIds).stream()
                .collect(Collectors.toMap(Training::getId, Training::getTitle));

        return results.stream()
                .map(doc -> {
                    String trainingId = doc.getString("_id");
                    return PopularTrainingResponse.builder()
                            .trainingId(trainingId)
                            .trainingTitle(titlesById.get(trainingId))
                            .enrollmentCount(toLong(doc.get("enrollmentCount")))
                            .build();
                })
                .toList();
    }

    /**
     * "My trainings" performance summary for a FORMATEUR — same $group-by-trainingId
     * aggregation as getTopTrainings(), pre-filtered to trainings this FORMATEUR owns
     * (TrainingRepository.findByInstructorId already exists for this).
     */
    public MyTrainingStatsResponse getMyTrainingStats(String instructorId) {
        List<String> ownedTrainingIds = trainingRepository.findByInstructorId(instructorId).stream()
                .map(Training::getId)
                .toList();

        if (ownedTrainingIds.isEmpty()) {
            return MyTrainingStatsResponse.builder()
                    .totalRevenue(BigDecimal.ZERO)
                    .totalCount(0L)
                    .trainings(List.of())
                    .build();
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("trainingId").in(ownedTrainingIds)),
                Aggregation.group("trainingId")
                        .sum("amountAtEnrollment").as("revenue")
                        .count().as("enrollmentCount"),
                Aggregation.sort(Sort.Direction.DESC, "revenue")
        );

        List<Document> results = mongoTemplate.aggregate(aggregation, TrainingEnrollment.class, Document.class).getMappedResults();

        Map<String, String> titlesById = trainingRepository.findAllById(
                results.stream().map(doc -> doc.getString("_id")).toList()
        ).stream().collect(Collectors.toMap(Training::getId, Training::getTitle));

        List<TopTrainingResponse> trainings = results.stream()
                .map(doc -> {
                    String trainingId = doc.getString("_id");
                    return TopTrainingResponse.builder()
                            .trainingId(trainingId)
                            .trainingTitle(titlesById.get(trainingId))
                            .enrollmentCount(toLong(doc.get("enrollmentCount")))
                            .revenue(toBigDecimal(doc.get("revenue")))
                            .build();
                })
                .toList();

        BigDecimal totalRevenue = trainings.stream().map(TopTrainingResponse::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalCount = trainings.stream().mapToLong(TopTrainingResponse::getEnrollmentCount).sum();

        return MyTrainingStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalCount(totalCount)
                .trainings(trainings)
                .build();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof Decimal128 d) return d.bigDecimalValue();
        if (value instanceof BigDecimal b) return b;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }
}