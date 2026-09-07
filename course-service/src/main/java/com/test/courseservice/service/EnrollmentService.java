package com.test.courseservice.service;

import com.test.courseservice.client.UserServiceClient;
import com.test.courseservice.dto.*;
import com.test.courseservice.event.EnrollmentEventPublisher;
import com.test.courseservice.exception.EnrollmentNotFoundException;
import com.test.courseservice.exception.EnrollmentRequiredException;
import com.test.courseservice.exception.InvalidEnrollmentTargetException;
import com.test.courseservice.exception.ResourceNotFoundException;
import com.test.courseservice.model.Course;
import com.test.courseservice.model.Enrollment;
import com.test.courseservice.model.EnrollmentStatus;
import com.test.courseservice.model.StudentProgress;
import com.test.courseservice.repository.*;
import com.test.courseservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.DateOperators;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final UserServiceClient userServiceClient;
    private final EnrollmentEventPublisher eventPublisher;
    private final ChapterRepository chapterRepository;
    private final VideoRepository videoRepository;
    private final StudentProgressRepository studentProgressRepository;
    private final MongoTemplate mongoTemplate;

    public EnrollmentResponse createEnrollment(EnrollmentCreateRequest request, AuthenticatedUser currentUser) {

        Course course = resolveCourse(request.getCourseId());

        UserServiceClient.UserBasicInfo studentInfo = userServiceClient.fetchBasicInfo(request.getStudentId(), currentUser.token());
        if (studentInfo == null) {
            throw new ResourceNotFoundException("Student not found.");
        }
        if (!"ETUDIANT".equals(studentInfo.role())) {
            throw new InvalidEnrollmentTargetException("Only students can be enrolled in courses.");
        }

        Optional<Enrollment> existing = enrollmentRepository.findByStudentIdAndCourseId(request.getStudentId(), request.getCourseId());

        if (existing.isPresent()) {
            Enrollment enrollment = existing.get();

            if (enrollment.getEnrollmentStatus() == EnrollmentStatus.REVOKED) {
                enrollment.setEnrollmentStatus(EnrollmentStatus.ACTIVE);
                enrollment.setActivatedAt(LocalDateTime.now());
                enrollment.setActivatedBy(currentUser.userId());
                enrollment.setUpdatedAt(LocalDateTime.now());
                Enrollment reactivated = enrollmentRepository.save(enrollment);

                eventPublisher.publishEnrollmentActivated(
                        reactivated.getStudentId(), reactivated.getCourseId(), reactivated.getCourseTitle(),
                        reactivated.getAmountAtEnrollment(), reactivated.getActivatedAt());

                return toResponse(reactivated, true, currentUser.token());
            }

            return toResponse(enrollment, true, currentUser.token());
        }

        BigDecimal discount = course.getDiscountPercentage() != null ? course.getDiscountPercentage() : BigDecimal.ZERO;
        BigDecimal finalPrice = courseService.calculateFinalPrice(course.getPrice(), discount);

        LocalDateTime now = LocalDateTime.now();

        Enrollment enrollment = Enrollment.builder()
                .studentId(request.getStudentId())
                .courseId(request.getCourseId())
                .courseTitle(course.getTitle())
                .amountAtEnrollment(finalPrice)
                .enrollmentStatus(EnrollmentStatus.ACTIVE)
                .enrolledAt(now)
                .activatedAt(now)
                .activatedBy(currentUser.userId())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Enrollment saved;
        try {
            saved = enrollmentRepository.save(enrollment);
        } catch (DuplicateKeyException e) {
            saved = enrollmentRepository.findByStudentIdAndCourseId(request.getStudentId(), request.getCourseId()).orElseThrow(() -> e);
        }

        eventPublisher.publishEnrollmentActivated(
                saved.getStudentId(), saved.getCourseId(), saved.getCourseTitle(),
                saved.getAmountAtEnrollment(), saved.getActivatedAt());

        return toResponse(saved, true, currentUser.token());
    }

    /**
     * Admin search over up to 3 independent optional filters (courseId/studentId/status).
     * Built with MongoTemplate/Criteria rather than a combinatorial set of derived repository
     * methods — same approach used for the equivalent Training enrollment admin listing.
     */
    public PageResponse<EnrollmentResponse> listAllEnrollments(String courseId, String studentId, EnrollmentStatus status,
                                                               int page, int size, AuthenticatedUser currentUser) {
        List<Criteria> criteria = new ArrayList<>();
        if (courseId != null && !courseId.isBlank()) {
            criteria.add(Criteria.where("courseId").is(courseId));
        }
        if (studentId != null && !studentId.isBlank()) {
            criteria.add(Criteria.where("studentId").is(studentId));
        }
        if (status != null) {
            criteria.add(Criteria.where("enrollmentStatus").is(status));
        }
        Criteria combined = criteria.isEmpty()
                ? new Criteria()
                : new Criteria().andOperator(criteria.toArray(new Criteria[0]));

        Pageable pageable = safePageable(page, size);
        Query pagedQuery = Query.query(combined).with(pageable);
        Query countQuery = Query.query(combined);

        List<Enrollment> results = mongoTemplate.find(pagedQuery, Enrollment.class);
        long totalElements = mongoTemplate.count(countQuery, Enrollment.class);

        List<EnrollmentResponse> content = results.stream()
                .map(e -> toResponse(e, true, currentUser.token()))
                .toList();

        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) totalElements / pageable.getPageSize());

        return PageResponse.<EnrollmentResponse>builder()
                .content(content)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(pageable.getPageNumber() == 0)
                .last(pageable.getPageNumber() >= totalPages - 1)
                .build();
    }

    public PageResponse<EnrollmentResponse> listCourseEnrollments(String courseId, int page, int size, AuthenticatedUser currentUser) {
        resolveCourse(courseId);
        Page<Enrollment> result = enrollmentRepository.findByCourseId(courseId, safePageable(page, size));
        return toPageResponse(result, true, currentUser.token());
    }

    public PageResponse<EnrollmentResponse> listMyEnrollments(int page, int size, AuthenticatedUser currentUser) {
        Page<Enrollment> result = enrollmentRepository.findByStudentId(currentUser.userId(), safePageable(page, size));
        return toPageResponse(result, false, currentUser.token());
    }

    public void revokeEnrollment(String enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException("Enrollment not found with id: " + enrollmentId));

        enrollment.setEnrollmentStatus(EnrollmentStatus.REVOKED);
        enrollment.setUpdatedAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);
    }

    public CourseAccessResponse checkAccess(String courseId, AuthenticatedUser currentUser) {
        resolveCourse(courseId);

        if ("ADMIN".equals(currentUser.role()) || "FORMATEUR".equals(currentUser.role())) {
            return CourseAccessResponse.builder().courseId(courseId).enrolled(true).status("ACTIVE").build();
        }

        Optional<Enrollment> enrollment = enrollmentRepository.findByStudentIdAndCourseId(currentUser.userId(), courseId);
        boolean enrolled = enrollment.map(e -> e.getEnrollmentStatus() == EnrollmentStatus.ACTIVE).orElse(false);
        String status = enrollment.map(e -> e.getEnrollmentStatus().name()).orElse(null);

        return CourseAccessResponse.builder().courseId(courseId).enrolled(enrolled).status(status).build();
    }

    boolean hasActiveEnrollment(String studentId, String courseId) {
        return enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .map(e -> e.getEnrollmentStatus() == EnrollmentStatus.ACTIVE)
                .orElse(false);
    }

    void assertActiveEnrollment(String studentId, String courseId) {
        if (!hasActiveEnrollment(studentId, courseId)) {
            throw new EnrollmentRequiredException("You are not enrolled in this course.");
        }
    }

    private Course resolveCourse(String courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
    }

    private Pageable safePageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private EnrollmentResponse toResponse(Enrollment enrollment, boolean includeActivatedBy, String jwtToken) {

        EnrollmentResponse.EnrollmentResponseBuilder builder = EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudentId())
                .courseId(enrollment.getCourseId())
                .courseTitle(enrollment.getCourseTitle())
                .amountAtEnrollment(enrollment.getAmountAtEnrollment())
                .enrollmentStatus(enrollment.getEnrollmentStatus())
                .enrolledAt(enrollment.getEnrolledAt())
                .activatedAt(enrollment.getActivatedAt());

        if (includeActivatedBy) {
            builder.activatedBy(enrollment.getActivatedBy());
        }

        UserServiceClient.UserBasicInfo studentInfo = userServiceClient.fetchBasicInfo(enrollment.getStudentId(), jwtToken);
        if (studentInfo != null) {
            builder.studentName(studentInfo.firstName() + " " + studentInfo.lastName());
            builder.studentEmail(studentInfo.email());
        }

        return builder.build();
    }

    private PageResponse<EnrollmentResponse> toPageResponse(Page<Enrollment> page, boolean includeActivatedBy, String jwtToken) {
        var content = page.getContent().stream()
                .map(e -> toResponse(e, includeActivatedBy, jwtToken))
                .toList();

        return PageResponse.<EnrollmentResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public PageResponse<MyCourseResponse> listMyCourses(int page, int size, AuthenticatedUser currentUser) {

        Page<Enrollment> result = enrollmentRepository.findByStudentIdAndEnrollmentStatus(
                currentUser.userId(), EnrollmentStatus.ACTIVE, safePageable(page, size));

        List<MyCourseResponse> content = result.getContent().stream()
                .map(this::toMyCourseResponse)
                .toList();

        return PageResponse.<MyCourseResponse>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    private MyCourseResponse toMyCourseResponse(Enrollment enrollment) {

        Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);

        StudentProgress progress = studentProgressRepository
                .findByStudentIdAndCourseId(enrollment.getStudentId(), enrollment.getCourseId())
                .orElse(null);

        List<String> completedVideoIds = progress != null ? progress.getCompletedVideoIds() : List.of();

        ProgressCalculator.Result calc = ProgressCalculator.compute(
                chapterRepository, videoRepository, enrollment.getCourseId(), completedVideoIds);

        return MyCourseResponse.builder()
                .courseId(enrollment.getCourseId())
                .courseTitle(enrollment.getCourseTitle())
                .category(course != null ? course.getCategory() : null)
                .image(course != null ? course.getImage() : null)
                .progressPercentage(calc.percentage())
                .completed(calc.completed())
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }

    /**
     * Total revenue/count + last-7-days breakdown, for the admin dashboard "Revenue" stat card
     * and its charts. REVOKED enrollments still count (a revoke is an access correction, not a
     * refund — same convention as everywhere else in this service).
     */
    public EnrollmentStatsResponse getEnrollmentStats() {
        Aggregation totalAgg = Aggregation.newAggregation(
                Aggregation.group()
                        .sum("amountAtEnrollment").as("totalRevenue")
                        .count().as("totalCount")
        );
        Document totalDoc = mongoTemplate.aggregate(totalAgg, Enrollment.class, Document.class).getUniqueMappedResult();

        BigDecimal totalRevenue = totalDoc != null ? toBigDecimal(totalDoc.get("totalRevenue")) : BigDecimal.ZERO;
        long totalCount = totalDoc != null ? toLong(totalDoc.get("totalCount")) : 0L;

        LocalDate today = LocalDate.now();
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
        List<Document> dailyResults = mongoTemplate.aggregate(dailyAgg, Enrollment.class, Document.class).getMappedResults();

        Map<String, Document> byDate = new HashMap<>();
        for (Document doc : dailyResults) {
            byDate.put(doc.getString("_id"), doc);
        }

        List<EnrollmentDailyStat> dailyStats = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = sevenDaysAgo.plusDays(i);
            String key = day.toString();
            Document doc = byDate.get(key);
            dailyStats.add(EnrollmentDailyStat.builder()
                    .date(key)
                    .revenue(doc != null ? toBigDecimal(doc.get("revenue")) : BigDecimal.ZERO)
                    .count(doc != null ? toLong(doc.get("count")) : 0L)
                    .build());
        }

        return EnrollmentStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalCount(totalCount)
                .dailyStats(dailyStats)
                .build();
    }

    /**
     * Top courses ranked by revenue or enrollment count. courseTitle is already denormalized
     * onto every Enrollment document, so a single $group with $first is enough — no join back
     * to the Course collection needed.
     */
    public List<TopCourseResponse> getTopCourses(int limit, String sortBy) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        String sortField = "count".equalsIgnoreCase(sortBy) ? "enrollmentCount" : "revenue";

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group("courseId")
                        .first("courseTitle").as("courseTitle")
                        .sum("amountAtEnrollment").as("revenue")
                        .count().as("enrollmentCount"),
                Aggregation.sort(Sort.Direction.DESC, sortField),
                Aggregation.limit(safeLimit)
        );

        List<Document> results = mongoTemplate.aggregate(aggregation, Enrollment.class, Document.class).getMappedResults();

        return results.stream()
                .map(doc -> TopCourseResponse.builder()
                        .courseId(doc.getString("_id"))
                        .courseTitle(doc.getString("courseTitle"))
                        .enrollmentCount(toLong(doc.get("enrollmentCount")))
                        .revenue(toBigDecimal(doc.get("revenue")))
                        .build())
                .toList();
    }

    /**
     * "My courses" performance summary for a FORMATEUR — same $group-by-courseId aggregation as
     * getTopCourses(), but pre-filtered with a $match to only the courses this FORMATEUR owns
     * (CourseRepository.findByInstructorId already exists for exactly this). No cross-instructor
     * data ever leaves this method. Totals are summed in Java from the per-course breakdown —
     * one aggregation round-trip, not two.
     */
    public MyCourseStatsResponse getMyCourseStats(String instructorId) {
        List<String> ownedCourseIds = courseRepository.findByInstructorId(instructorId, Pageable.unpaged())
                .map(Course::getId)
                .toList();

        if (ownedCourseIds.isEmpty()) {
            return MyCourseStatsResponse.builder()
                    .totalRevenue(BigDecimal.ZERO)
                    .totalCount(0L)
                    .courses(List.of())
                    .build();
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("courseId").in(ownedCourseIds)),
                Aggregation.group("courseId")
                        .first("courseTitle").as("courseTitle")
                        .sum("amountAtEnrollment").as("revenue")
                        .count().as("enrollmentCount"),
                Aggregation.sort(Sort.Direction.DESC, "revenue")
        );

        List<Document> results = mongoTemplate.aggregate(aggregation, Enrollment.class, Document.class).getMappedResults();

        List<TopCourseResponse> courses = results.stream()
                .map(doc -> TopCourseResponse.builder()
                        .courseId(doc.getString("_id"))
                        .courseTitle(doc.getString("courseTitle"))
                        .enrollmentCount(toLong(doc.get("enrollmentCount")))
                        .revenue(toBigDecimal(doc.get("revenue")))
                        .build())
                .toList();

        BigDecimal totalRevenue = courses.stream().map(TopCourseResponse::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalCount = courses.stream().mapToLong(TopCourseResponse::getEnrollmentCount).sum();

        return MyCourseStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalCount(totalCount)
                .courses(courses)
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