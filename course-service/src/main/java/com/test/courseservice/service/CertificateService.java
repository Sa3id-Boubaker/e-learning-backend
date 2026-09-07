package com.test.courseservice.service;

import com.test.courseservice.client.UserServiceClient;
import com.test.courseservice.dto.CertificateResponse;
import com.test.courseservice.dto.PageResponse;
import com.test.courseservice.dto.StudentProgressResponse;
import com.test.courseservice.exception.CertificateGenerationException;
import com.test.courseservice.exception.CertificateNotEligibleException;
import com.test.courseservice.exception.CertificateNotFoundException;
import com.test.courseservice.model.Certificate;
import com.test.courseservice.model.CertificateStatus;
import com.test.courseservice.model.Course;
import com.test.courseservice.model.Quiz;
import com.test.courseservice.model.QuizAttempt;
import com.test.courseservice.repository.CertificateRepository;
import com.test.courseservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final QuizService quizService;
    private final QuizAttemptService quizAttemptService;
    private final StudentProgressService studentProgressService;
    private final UserServiceClient userServiceClient;
    private final EnrollmentService enrollmentService;

    public record CertificateGenerationResult(CertificateResponse response, boolean created) {}

    public CertificateGenerationResult generateCertificate(String courseId, AuthenticatedUser currentUser) {

        Course course = quizService.resolveCourse(courseId);

        Optional<Certificate> existing = certificateRepository.findByStudentIdAndCourseId(currentUser.userId(), courseId);
        if (existing.isPresent()) {
            return new CertificateGenerationResult(toResponse(existing.get(), false), false);
        }

        enrollmentService.assertActiveEnrollment(currentUser.userId(), courseId); // <-- ajouté, avant tout le reste

        StudentProgressResponse progress = studentProgressService.getCourseProgress(courseId, currentUser);
        if (!Boolean.TRUE.equals(progress.getCompleted())) {
            throw new CertificateNotEligibleException("You must complete the course before obtaining a certificate.");
        }

        Quiz quiz = quizService.findQuizByCourseOptional(courseId)
                .orElseThrow(() -> new CertificateNotEligibleException(
                        "You must pass the quiz with a score of at least 70% before obtaining a certificate."));

        QuizAttempt passingAttempt = quizAttemptService.findPassingAttempt(quiz.getId(), currentUser.userId())
                .orElseThrow(() -> new CertificateNotEligibleException(
                        "You must pass the quiz with a score of at least 70% before obtaining a certificate."));

        UserServiceClient.UserBasicInfo studentInfo = userServiceClient.fetchBasicInfo(currentUser.userId(), currentUser.token());
        UserServiceClient.UserBasicInfo instructorInfo = userServiceClient.fetchBasicInfo(course.getInstructorId(), currentUser.token());

        if (studentInfo == null || instructorInfo == null) {
            throw new CertificateGenerationException("Failed to retrieve the required user information to generate the certificate");
        }

        Certificate certificate = Certificate.builder()
                .certificateNumber(generateCertificateNumber())
                .studentId(currentUser.userId())
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .studentName(fullName(studentInfo))
                .studentProfileImage(studentInfo.profileImage())
                .instructorId(course.getInstructorId())
                .instructorName(fullName(instructorInfo))
                .instructorProfileImage(instructorInfo.profileImage())
                .quizScore(passingAttempt.getScore())
                .issuedAt(LocalDateTime.now())
                .status(CertificateStatus.ISSUED)
                .build();

        Certificate saved;
        try {
            saved = certificateRepository.save(certificate);
        } catch (DuplicateKeyException e) {
            saved = certificateRepository.findByStudentIdAndCourseId(currentUser.userId(), courseId).orElseThrow(() -> e);
        }

        return new CertificateGenerationResult(toResponse(saved, false), true);
    }

    public CertificateResponse getMyCertificateForCourse(String courseId, AuthenticatedUser currentUser) {
        Certificate certificate = certificateRepository.findByStudentIdAndCourseId(currentUser.userId(), courseId)
                .orElseThrow(() -> new CertificateNotFoundException("No certificate found for this course"));
        return toResponse(certificate, false);
    }

    public PageResponse<CertificateResponse> getMyCertificates(int page, int size, AuthenticatedUser currentUser) {
        Page<Certificate> result = certificateRepository.findByStudentId(currentUser.userId(), safePageable(page, size));
        return toPageResponse(result, false);
    }

    public CertificateResponse verifyCertificate(String certificateNumber) {
        Certificate certificate = certificateRepository.findByCertificateNumber(certificateNumber)
                .orElseThrow(() -> new CertificateNotFoundException("No certificate found with number: " + certificateNumber));
        return toResponse(certificate, false);
    }

    public PageResponse<CertificateResponse> listAllCertificates(int page, int size) {
        Page<Certificate> result = certificateRepository.findAll(safePageable(page, size));
        return toPageResponse(result, true);
    }

    public PageResponse<CertificateResponse> listCertificatesByCourse(String courseId, int page, int size, AuthenticatedUser currentUser) {
        Course course = quizService.resolveCourse(courseId);
        quizService.assertManageAccess(course, currentUser);

        Page<Certificate> result = certificateRepository.findByCourseId(courseId, safePageable(page, size));
        return toPageResponse(result, true);
    }

    private Pageable safePageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "issuedAt"));
    }

    private String generateCertificateNumber() {
        int year = LocalDateTime.now().getYear();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "CERT-" + year + "-" + suffix;
    }

    private String fullName(UserServiceClient.UserBasicInfo info) {
        return info.firstName() + " " + info.lastName();
    }

    private CertificateResponse toResponse(Certificate certificate, boolean includeInternalIds) {
        CertificateResponse.CertificateResponseBuilder builder = CertificateResponse.builder()
                .id(certificate.getId())
                .certificateNumber(certificate.getCertificateNumber())
                .courseId(certificate.getCourseId())
                .courseTitle(certificate.getCourseTitle())
                .studentName(certificate.getStudentName())
                .studentProfileImage(certificate.getStudentProfileImage())
                .instructorName(certificate.getInstructorName())
                .instructorProfileImage(certificate.getInstructorProfileImage())
                .quizScore(certificate.getQuizScore())
                .issuedAt(certificate.getIssuedAt())
                .status(certificate.getStatus());

        if (includeInternalIds) {
            builder.studentId(certificate.getStudentId())
                    .instructorId(certificate.getInstructorId());
        }

        return builder.build();
    }

    private PageResponse<CertificateResponse> toPageResponse(Page<Certificate> page, boolean includeInternalIds) {
        var content = page.getContent().stream()
                .map(c -> toResponse(c, includeInternalIds))
                .toList();

        return PageResponse.<CertificateResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}