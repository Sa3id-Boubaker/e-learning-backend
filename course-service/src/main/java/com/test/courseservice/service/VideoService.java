package com.test.courseservice.service;

import com.test.courseservice.dto.VideoCreateRequest;
import com.test.courseservice.dto.VideoResponse;
import com.test.courseservice.dto.VideoUpdateRequest;
import com.test.courseservice.exception.*;
import com.test.courseservice.model.Chapter;
import com.test.courseservice.model.Course;
import com.test.courseservice.model.Video;
import com.test.courseservice.repository.ChapterRepository;
import com.test.courseservice.repository.CourseRepository;
import com.test.courseservice.repository.VideoRepository;
import com.test.courseservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final ChapterRepository chapterRepository;
    private final CourseRepository courseRepository;
    private final CloudinaryService cloudinaryService;
    private final EnrollmentService enrollmentService;

    public VideoResponse createVideo(String chapterId, VideoCreateRequest request, AuthenticatedUser currentUser) {

        Chapter chapter = resolveChapter(chapterId);
        Course course = resolveCourse(chapter.getCourseId());
        assertManageAccess(course, currentUser);

        String uploadReference = UUID.randomUUID().toString();
        CloudinaryService.UploadResult uploadResult = cloudinaryService.uploadVideo(request.getVideo(), uploadReference);

        LocalDateTime now = LocalDateTime.now();

        Video video = Video.builder()
                .chapterId(chapterId)
                .title(request.getTitle())
                .description(request.getDescription())
                .videoUrl(uploadResult.url())
                .videoPublicId(uploadResult.publicId())
                .duration(uploadResult.duration())
                .order(request.getOrder())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Video saved = videoRepository.save(video);
        return toResponse(saved, false); // le créateur (FORMATEUR/ADMIN) voit toujours l'URL réelle
    }

    public List<VideoResponse> getVideosByChapter(String chapterId, AuthenticatedUser currentUser) {

        Chapter chapter = resolveChapter(chapterId);
        Course course = resolveCourse(chapter.getCourseId());
        assertViewAccess(course, currentUser);

        boolean maskUrl = shouldMaskVideoUrl(course, currentUser);

        return videoRepository.findByChapterIdOrderByOrderAsc(chapterId).stream()
                .map(v -> toResponse(v, maskUrl))
                .toList();
    }

    public VideoResponse getVideoById(String id, AuthenticatedUser currentUser) {

        Video video = resolveVideo(id);
        Chapter chapter = resolveChapter(video.getChapterId());
        Course course = resolveCourse(chapter.getCourseId());
        assertViewAccess(course, currentUser);

        boolean maskUrl = shouldMaskVideoUrl(course, currentUser);

        return toResponse(video, maskUrl);
    }

    public VideoResponse updateVideo(String id, VideoUpdateRequest request, AuthenticatedUser currentUser) {

        Video video = resolveVideo(id);
        Chapter chapter = resolveChapter(video.getChapterId());
        Course course = resolveCourse(chapter.getCourseId());
        assertManageAccess(course, currentUser);

        if (request.getTitle() != null) video.setTitle(request.getTitle());
        if (request.getDescription() != null) video.setDescription(request.getDescription());
        if (request.getOrder() != null) video.setOrder(request.getOrder());

        video.setUpdatedAt(LocalDateTime.now());

        Video saved = videoRepository.save(video);
        return toResponse(saved, false);
    }

    public void deleteVideo(String id, AuthenticatedUser currentUser) {

        Video video = resolveVideo(id);
        Chapter chapter = resolveChapter(video.getChapterId());
        Course course = resolveCourse(chapter.getCourseId());
        assertManageAccess(course, currentUser);

        cloudinaryService.deleteVideoIfExists(video.getVideoPublicId());
        videoRepository.delete(video);
    }

    private Video resolveVideo(String id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new VideoNotFoundException("Video not found with id: " + id));
    }

    private Chapter resolveChapter(String chapterId) {
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ChapterNotFoundException("Chapter not found with id: " + chapterId));
    }

    private Course resolveCourse(String courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
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
                // Plus de blocage ici — un étudiant non inscrit peut consulter la liste des
                // vidéos ; seul videoUrl est masqué, voir shouldMaskVideoUrl()/toResponse().
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

    private boolean shouldMaskVideoUrl(Course course, AuthenticatedUser currentUser) {
        if (!"ETUDIANT".equals(currentUser.role())) {
            return false; // FORMATEUR/ADMIN voient toujours l'URL réelle
        }
        return !enrollmentService.hasActiveEnrollment(currentUser.userId(), course.getId());
    }

    private VideoResponse toResponse(Video video, boolean maskUrl) {
        return VideoResponse.builder()
                .id(video.getId())
                .chapterId(video.getChapterId())
                .title(video.getTitle())
                .description(video.getDescription())
                .videoUrl(maskUrl ? null : video.getVideoUrl())
                .duration(video.getDuration())
                .order(video.getOrder())
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .build();
    }
}