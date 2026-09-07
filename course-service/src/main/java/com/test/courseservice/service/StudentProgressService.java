package com.test.courseservice.service;

import com.test.courseservice.dto.StudentProgressResponse;
import com.test.courseservice.dto.VideoProgressResponse;
import com.test.courseservice.exception.*;
import com.test.courseservice.model.Chapter;
import com.test.courseservice.model.Course;
import com.test.courseservice.model.StudentProgress;
import com.test.courseservice.model.Video;
import com.test.courseservice.repository.ChapterRepository;
import com.test.courseservice.repository.CourseRepository;
import com.test.courseservice.repository.StudentProgressRepository;
import com.test.courseservice.repository.VideoRepository;
import com.test.courseservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentProgressService {

    private final StudentProgressRepository progressRepository;
    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final VideoRepository videoRepository;
    private final EnrollmentService enrollmentService;

    public StudentProgressResponse markVideoComplete(String courseId, String videoId, AuthenticatedUser currentUser) {

        Course course = resolveCourse(courseId);
        assertStudentCourseAccess(course, currentUser);

        Video video = resolveVideo(videoId);
        assertVideoBelongsToCourse(video, courseId);

        StudentProgress progress = findOrCreateProgress(currentUser.userId(), courseId);

        if (!progress.getCompletedVideoIds().contains(videoId)) {
            progress.getCompletedVideoIds().add(videoId);
        }

        return saveAndRespond(progress, courseId);
    }

    public StudentProgressResponse getCourseProgress(String courseId, AuthenticatedUser currentUser) {

        Course course = resolveCourse(courseId);
        assertStudentCourseAccess(course, currentUser);

        StudentProgress progress = progressRepository.findByStudentIdAndCourseId(currentUser.userId(), courseId)
                .orElseGet(() -> newEmptyProgress(currentUser.userId(), courseId));

        return buildResponse(progress, courseId);
    }

    public VideoProgressResponse getVideoProgress(String courseId, String videoId, AuthenticatedUser currentUser) {

        Course course = resolveCourse(courseId);
        assertStudentCourseAccess(course, currentUser);

        Video video = resolveVideo(videoId);
        assertVideoBelongsToCourse(video, courseId);

        boolean completed = progressRepository.findByStudentIdAndCourseId(currentUser.userId(), courseId)
                .map(p -> p.getCompletedVideoIds().contains(videoId))
                .orElse(false);

        return new VideoProgressResponse(videoId, completed);
    }

    private StudentProgress findOrCreateProgress(String studentId, String courseId) {
        return progressRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseGet(() -> {
                    try {
                        return progressRepository.save(newEmptyProgress(studentId, courseId));
                    } catch (DuplicateKeyException e) {
                        // Une requête concurrente a créé le document entre-temps — on récupère celui-ci plutôt que d'échouer.
                        return progressRepository.findByStudentIdAndCourseId(studentId, courseId)
                                .orElseThrow(() -> e);
                    }
                });
    }

    private StudentProgress newEmptyProgress(String studentId, String courseId) {
        LocalDateTime now = LocalDateTime.now();
        return StudentProgress.builder()
                .studentId(studentId)
                .courseId(courseId)
                .completedVideoIds(new ArrayList<>())
                .progressPercentage(0.0)
                .completed(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private StudentProgressResponse saveAndRespond(StudentProgress progress, String courseId) {

        Set<String> validVideoIds = resolveValidVideoIds(courseId);
        long totalVideos = validVideoIds.size();
        long completedCount = progress.getCompletedVideoIds().stream()
                .filter(validVideoIds::contains)
                .count();

        double percentage = computePercentage(completedCount, totalVideos);
        boolean isCompleted = totalVideos > 0 && completedCount == totalVideos;

        progress.setProgressPercentage(percentage);
        progress.setCompleted(isCompleted);
        progress.setUpdatedAt(LocalDateTime.now());

        StudentProgress saved = progressRepository.save(progress);

        return toResponse(saved, completedCount, totalVideos, percentage, isCompleted);
    }

    private StudentProgressResponse buildResponse(StudentProgress progress, String courseId) {

        Set<String> validVideoIds = resolveValidVideoIds(courseId);
        long totalVideos = validVideoIds.size();
        long completedCount = progress.getCompletedVideoIds().stream()
                .filter(validVideoIds::contains)
                .count();

        double percentage = computePercentage(completedCount, totalVideos);
        boolean isCompleted = totalVideos > 0 && completedCount == totalVideos;

        return toResponse(progress, completedCount, totalVideos, percentage, isCompleted);
    }

    /** Ne renvoie que les ids de vidéos réellement existantes aujourd'hui — filtre naturellement les vidéos supprimées entre-temps. */
    private Set<String> resolveValidVideoIds(String courseId) {
        List<String> chapterIds = chapterRepository.findByCourseIdOrderByOrderAsc(courseId).stream()
                .map(Chapter::getId)
                .toList();

        if (chapterIds.isEmpty()) {
            return Set.of();
        }

        return videoRepository.findByChapterIdIn(chapterIds).stream()
                .map(Video::getId)
                .collect(Collectors.toSet());
    }

    private double computePercentage(long completedCount, long totalVideos) {
        if (totalVideos == 0) return 0.0;
        double raw = (completedCount * 100.0) / totalVideos;
        return Math.round(raw * 100.0) / 100.0;
    }

    private Course resolveCourse(String courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
    }

    private Video resolveVideo(String videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException("Video not found with id: " + videoId));
    }

    private void assertVideoBelongsToCourse(Video video, String courseId) {
        Chapter chapter = chapterRepository.findById(video.getChapterId())
                .orElseThrow(() -> new ChapterNotFoundException("Chapter not found with id: " + video.getChapterId()));

        if (!chapter.getCourseId().equals(courseId)) {
            throw new VideoNotBelongToCourseException("This video does not belong to the specified course");
        }
    }

    private void assertStudentCourseAccess(Course course, AuthenticatedUser currentUser) {
        if (!Boolean.TRUE.equals(course.getPublished())) {
            throw new CourseAccessDeniedException("This course is not available");
        }
        enrollmentService.assertActiveEnrollment(currentUser.userId(), course.getId());
    }

    private StudentProgressResponse toResponse(StudentProgress progress, long completedCount, long totalVideos,
                                               double percentage, boolean completed) {
        return StudentProgressResponse.builder()
                .id(progress.getId())
                .courseId(progress.getCourseId())
                .completedVideoIds(progress.getCompletedVideoIds())
                .completedVideosCount((int) completedCount)
                .totalVideosCount((int) totalVideos)
                .progressPercentage(percentage)
                .completed(completed)
                .createdAt(progress.getCreatedAt())
                .updatedAt(progress.getUpdatedAt())
                .build();
    }
}