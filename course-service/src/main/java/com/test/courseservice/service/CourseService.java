package com.test.courseservice.service;

import com.test.courseservice.client.UserServiceClient;
import com.test.courseservice.dto.*;
import com.test.courseservice.exception.CourseAccessDeniedException;
import com.test.courseservice.exception.NotCourseOwnerException;
import com.test.courseservice.exception.ResourceNotFoundException;
import com.test.courseservice.model.Chapter;
import com.test.courseservice.model.Course;
import com.test.courseservice.model.Video;
import com.test.courseservice.repository.ChapterRepository;
import com.test.courseservice.repository.CourseRepository;
import com.test.courseservice.repository.EnrollmentRepository;
import com.test.courseservice.repository.QuestionRepository;
import com.test.courseservice.repository.QuizRepository;
import com.test.courseservice.repository.StudentProgressRepository;
import com.test.courseservice.repository.VideoRepository;
import com.test.courseservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CloudinaryService cloudinaryService;
    private final UserServiceClient userServiceClient;

    // Uniquement des repositories ici, jamais ChapterService/EnrollmentService/QuizService/StudentProgressService :
    // EnrollmentService dépend déjà de CourseService, donc CourseService ne doit jamais dépendre en retour
    // d'un service qui remonte vers lui (dépendance circulaire au démarrage de Spring sinon).
    private final ChapterRepository chapterRepository;
    private final VideoRepository videoRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentProgressRepository studentProgressRepository;

    public CourseResponse createCourse(CourseCreateRequest request, AuthenticatedUser currentUser) {

        LocalDateTime now = LocalDateTime.now();

        boolean isAdmin = "ADMIN".equals(currentUser.role());
        BigDecimal initialDiscount = (isAdmin && request.getDiscountPercentage() != null)
                ? request.getDiscountPercentage()
                : BigDecimal.ZERO;

        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .price(request.getPrice())
                .discountPercentage(initialDiscount)
                .instructorId(currentUser.userId())
                .published(request.getPublished() != null ? request.getPublished() : false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Course saved = courseRepository.save(course);

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            CloudinaryService.UploadResult uploadResult =
                    cloudinaryService.uploadCourseImage(request.getImage(), saved.getId());

            saved.setImage(uploadResult.url());
            saved.setImagePublicId(uploadResult.publicId());
            saved = courseRepository.save(saved);
        }

        return toResponse(saved);
    }

    public PageResponse<CourseResponse> getAllCourses(String search, int page, int size, AuthenticatedUser currentUser) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        boolean hasSearch = search != null && !search.isBlank();
        String safeSearch = hasSearch ? escapeRegex(search.trim()) : null;

        Page<Course> coursePage = switch (currentUser.role()) {
            case "ADMIN" -> hasSearch
                    ? courseRepository.findByTitleContainingIgnoreCase(safeSearch, pageable)
                    : courseRepository.findAll(pageable);
            case "FORMATEUR" -> hasSearch
                    ? courseRepository.findByInstructorIdAndTitleContainingIgnoreCase(currentUser.userId(), safeSearch, pageable)
                    : courseRepository.findByInstructorId(currentUser.userId(), pageable);
            default -> hasSearch
                    ? courseRepository.findByPublishedTrueAndTitleContainingIgnoreCase(safeSearch, pageable)
                    : courseRepository.findByPublishedTrue(pageable);
        };

        boolean enrich = "ADMIN".equals(currentUser.role());

        List<CourseResponse> content = coursePage.getContent().stream()
                .map(course -> enrich ? toEnrichedResponse(course, currentUser.token()) : toResponse(course))
                .toList();

        return PageResponse.<CourseResponse>builder()
                .content(content)
                .page(coursePage.getNumber())
                .size(coursePage.getSize())
                .totalElements(coursePage.getTotalElements())
                .totalPages(coursePage.getTotalPages())
                .first(coursePage.isFirst())
                .last(coursePage.isLast())
                .build();
    }


    /** Liste complète non paginée (id + title uniquement, pas d'enrichissement instructeur) —
     * utilisée pour peupler des filtres/dropdowns admin, pas pour l'affichage détaillé. */
    public List<CourseSummaryResponse> getAllCourseSummaries(AuthenticatedUser currentUser) {
        assertAdminOnly(currentUser);

        return courseRepository.findAll(Sort.by(Sort.Direction.ASC, "title")).stream()
                .map(course -> CourseSummaryResponse.builder()
                        .id(course.getId())
                        .title(course.getTitle())
                        .build())
                .toList();
    }

    public CourseResponse getCourseById(String id, AuthenticatedUser currentUser) {
        Course course = resolveCourse(id);
        assertViewAccess(course, currentUser);

        if ("ADMIN".equals(currentUser.role())) {
            return toEnrichedResponse(course, currentUser.token());
        }
        return toResponse(course);
    }

    public CourseResponse updateCourse(String id, CourseUpdateRequest request, AuthenticatedUser currentUser) {

        Course course = resolveCourse(id);
        assertOwnership(course, currentUser);

        if (request.getTitle() != null) course.setTitle(request.getTitle());
        if (request.getDescription() != null) course.setDescription(request.getDescription());
        if (request.getCategory() != null) course.setCategory(request.getCategory());
        if (request.getPrice() != null) course.setPrice(request.getPrice());
        if (request.getPublished() != null) course.setPublished(request.getPublished());

        // Un FORMATEUR propriétaire peut appeler cet endpoint, mais jamais modifier la remise par ce biais.
        if (request.getDiscountPercentage() != null && "ADMIN".equals(currentUser.role())) {
            course.setDiscountPercentage(request.getDiscountPercentage());
        }

        course.setUpdatedAt(LocalDateTime.now());

        Course saved = courseRepository.save(course);
        return toResponse(saved);
    }

    /**
     * Suppression en cascade — MongoDB n'a pas de relations/clés étrangères, donc tout ce qui
     * référence ce cours doit être supprimé explicitement ici, dans cet ordre :
     * 1) chapters + leurs videos (fichiers Cloudinary compris)
     * 2) quiz du cours + ses questions (même logique que QuizService.deleteQuiz())
     * 3) enrollments et progression des étudiants sur ce cours
     * 4) image du cours, puis le cours lui-même (toujours en dernier)
     *
     * Volontairement NON transactionnel (MongoTemplate/multi-document transactions ne sont pas
     * utilisées ailleurs dans ce service) — cohérent avec le reste du projet, où les suppressions
     * en plusieurs étapes (ex. QuizService.deleteQuiz()) sont déjà séquentielles sans transaction.
     *
     * QuizAttempt et Certificate ne sont PAS supprimés ici — décision explicite : on garde
     * l'historique des tentatives de quiz et les certificats déjà délivrés même après suppression
     * du cours (ils sont dénormalisés, courseTitle inclus, donc restent lisibles sans le cours).
     */
    public void deleteCourse(String id, AuthenticatedUser currentUser) {
        Course course = resolveCourse(id);
        assertOwnership(course, currentUser);

        List<Chapter> chapters = chapterRepository.findByCourseIdOrderByOrderAsc(id);
        if (!chapters.isEmpty()) {
            List<String> chapterIds = chapters.stream().map(Chapter::getId).toList();

            List<Video> videos = videoRepository.findByChapterIdIn(chapterIds);
            videos.forEach(video -> cloudinaryService.deleteVideoIfExists(video.getVideoPublicId()));
            videoRepository.deleteAll(videos);

            chapterRepository.deleteAll(chapters);
        }

        quizRepository.findByCourseId(id).ifPresent(quiz -> {
            questionRepository.deleteByQuizId(quiz.getId());
            quizRepository.delete(quiz);
        });

        enrollmentRepository.deleteByCourseId(id);
        studentProgressRepository.deleteByCourseId(id);

        cloudinaryService.deleteIfExists(course.getImagePublicId());
        courseRepository.delete(course);
    }

    public CourseResponse uploadCourseImage(String id, MultipartFile file, AuthenticatedUser currentUser) {

        Course course = resolveCourse(id);
        assertOwnership(course, currentUser);

        String previousPublicId = course.getImagePublicId();

        CloudinaryService.UploadResult uploadResult = cloudinaryService.uploadCourseImage(file, course.getId());

        course.setImage(uploadResult.url());
        course.setImagePublicId(uploadResult.publicId());
        course.setUpdatedAt(LocalDateTime.now());

        Course saved = courseRepository.save(course);

        cloudinaryService.deleteIfExists(previousPublicId);

        return toResponse(saved);
    }

    public CourseResponse applyDiscount(String id, DiscountRequest request, AuthenticatedUser currentUser) {
        assertAdminOnly(currentUser);
        Course course = resolveCourse(id);

        course.setDiscountPercentage(request.getDiscountPercentage());
        course.setUpdatedAt(LocalDateTime.now());

        Course saved = courseRepository.save(course);
        return toResponse(saved);
    }

    public CourseResponse removeDiscount(String id, AuthenticatedUser currentUser) {
        assertAdminOnly(currentUser);
        Course course = resolveCourse(id);

        course.setDiscountPercentage(BigDecimal.ZERO);
        course.setUpdatedAt(LocalDateTime.now());

        Course saved = courseRepository.save(course);
        return toResponse(saved);
    }

    private void assertAdminOnly(AuthenticatedUser currentUser) {
        if (!"ADMIN".equals(currentUser.role())) {
            throw new CourseAccessDeniedException("Only an administrator can manage course discounts");
        }
    }

    private Course resolveCourse(String id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    private void assertViewAccess(Course course, AuthenticatedUser currentUser) {
        switch (currentUser.role()) {
            case "ADMIN" -> { }
            case "FORMATEUR" -> {
                if (!course.getInstructorId().equals(currentUser.userId())) {
                    throw new CourseAccessDeniedException("This course does not belong to you");
                }
            }
            default -> {
                if (!Boolean.TRUE.equals(course.getPublished())) {
                    throw new CourseAccessDeniedException("This course is not available");
                }
            }
        }
    }

    private void assertOwnership(Course course, AuthenticatedUser currentUser) {
        boolean isAdmin = "ADMIN".equals(currentUser.role());
        boolean isOwner = course.getInstructorId().equals(currentUser.userId());

        if (!isAdmin && !isOwner) {
            throw new NotCourseOwnerException("You are not the owner of this course");
        }
    }

    private CourseResponse toEnrichedResponse(Course course, String jwtToken) {
        CourseResponse response = toResponse(course);

        UserServiceClient.UserBasicInfo instructor = userServiceClient.fetchBasicInfo(course.getInstructorId(), jwtToken);

        if (instructor != null) {
            response.setInstructorFirstName(instructor.firstName());
            response.setInstructorLastName(instructor.lastName());
            response.setInstructorEmail(instructor.email());
        }

        return response;
    }

    private CourseResponse toResponse(Course course) {
        BigDecimal discountPercentage = course.getDiscountPercentage() != null
                ? course.getDiscountPercentage()
                : BigDecimal.ZERO;

        BigDecimal finalPrice = calculateFinalPrice(course.getPrice(), discountPercentage);

        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .category(course.getCategory())
                .price(course.getPrice())
                .discountPercentage(discountPercentage)
                .finalPrice(finalPrice)
                .image(course.getImage())
                .instructorId(course.getInstructorId())
                .published(course.getPublished())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    /**
     * Anonymous-safe course list for the public landing page — no auth required. Same
     * PUBLISHED-only filter as the ETUDIANT branch of getAllCourses(), but trimmed to fields
     * safe to show a visitor (no instructor id, no description). finalPrice is computed the
     * same way toResponse() does — it is never a stored field on Course.
     */
    public List<PublicCourseResponse> getPublicCourses(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        Pageable pageable = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));

        return courseRepository.findByPublishedTrue(pageable).getContent().stream()
                .map(course -> {
                    BigDecimal discountPercentage = course.getDiscountPercentage() != null
                            ? course.getDiscountPercentage()
                            : BigDecimal.ZERO;

                    return PublicCourseResponse.builder()
                            .id(course.getId())
                            .title(course.getTitle())
                            .category(course.getCategory())
                            .image(course.getImage())
                            .price(course.getPrice())
                            .finalPrice(calculateFinalPrice(course.getPrice(), discountPercentage))
                            .build();
                })
                .toList();
    }

    BigDecimal calculateFinalPrice(BigDecimal price, BigDecimal discountPercentage) {
        BigDecimal discountAmount = price
                .multiply(discountPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return price.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
    }

    private String escapeRegex(String input) {
        return input.replaceAll("([.*+?^${}()|\\[\\]\\\\])", "\\\\$1");
    }
}