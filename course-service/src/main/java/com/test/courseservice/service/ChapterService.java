package com.test.courseservice.service;

import com.test.courseservice.dto.ChapterCreateRequest;
import com.test.courseservice.dto.ChapterResponse;
import com.test.courseservice.dto.ChapterUpdateRequest;
import com.test.courseservice.exception.ChapterNotFoundException;
import com.test.courseservice.exception.CourseAccessDeniedException;
import com.test.courseservice.exception.NotCourseOwnerException;
import com.test.courseservice.exception.ResourceNotFoundException;
import com.test.courseservice.model.Chapter;
import com.test.courseservice.model.Course;
import com.test.courseservice.repository.ChapterRepository;
import com.test.courseservice.repository.CourseRepository;
import com.test.courseservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentService enrollmentService;

    public ChapterResponse createChapter(String courseId, ChapterCreateRequest request, AuthenticatedUser currentUser) {

        Course course = resolveCourse(courseId);
        assertManageAccess(course, currentUser);

        LocalDateTime now = LocalDateTime.now();

        Chapter chapter = Chapter.builder()
                .courseId(courseId)
                .title(request.getTitle())
                .description(request.getDescription())
                .order(request.getOrder())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Chapter saved = chapterRepository.save(chapter);
        return toResponse(saved);
    }

    public List<ChapterResponse> getChaptersByCourse(String courseId, AuthenticatedUser currentUser) {

        Course course = resolveCourse(courseId);
        assertViewAccess(course, currentUser);

        return chapterRepository.findByCourseIdOrderByOrderAsc(courseId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ChapterResponse getChapterById(String id, AuthenticatedUser currentUser) {

        Chapter chapter = resolveChapter(id);
        Course course = resolveCourse(chapter.getCourseId());
        assertViewAccess(course, currentUser);

        return toResponse(chapter);
    }

    public ChapterResponse updateChapter(String id, ChapterUpdateRequest request, AuthenticatedUser currentUser) {

        Chapter chapter = resolveChapter(id);
        Course course = resolveCourse(chapter.getCourseId());
        assertManageAccess(course, currentUser);

        if (request.getTitle() != null) chapter.setTitle(request.getTitle());
        if (request.getDescription() != null) chapter.setDescription(request.getDescription());
        if (request.getOrder() != null) chapter.setOrder(request.getOrder());

        chapter.setUpdatedAt(LocalDateTime.now());

        Chapter saved = chapterRepository.save(chapter);
        return toResponse(saved);
    }

    public void deleteChapter(String id, AuthenticatedUser currentUser) {

        Chapter chapter = resolveChapter(id);
        Course course = resolveCourse(chapter.getCourseId());
        assertManageAccess(course, currentUser);

        chapterRepository.delete(chapter);
    }

    private Course resolveCourse(String courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
    }

    private Chapter resolveChapter(String id) {
        return chapterRepository.findById(id)
                .orElseThrow(() -> new ChapterNotFoundException("Chapter not found with id: " + id));
    }

    /** Même logique que CourseService.assertViewAccess — dupliquée volontairement, sans dépendance croisée entre les deux services internes. */
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

    private void assertManageAccess(Course course, AuthenticatedUser currentUser) {
        boolean isAdmin = "ADMIN".equals(currentUser.role());
        boolean isOwner = course.getInstructorId().equals(currentUser.userId());

        if (!isAdmin && !isOwner) {
            throw new NotCourseOwnerException("You are not the owner of this course");
        }
    }

    private ChapterResponse toResponse(Chapter chapter) {
        return ChapterResponse.builder()
                .id(chapter.getId())
                .courseId(chapter.getCourseId())
                .title(chapter.getTitle())
                .description(chapter.getDescription())
                .order(chapter.getOrder())
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .build();
    }
}