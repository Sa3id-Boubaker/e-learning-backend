package com.test.courseservice.service;

import com.test.courseservice.model.Chapter;
import com.test.courseservice.model.Video;
import com.test.courseservice.repository.ChapterRepository;
import com.test.courseservice.repository.VideoRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Utilitaire partagé, sans état — évite une dépendance circulaire entre
 StudentProgressService et EnrollmentService, qui ont chacun besoin de ce calcul. */
final class ProgressCalculator {

    private ProgressCalculator() {}

    record Result(long completedCount, long totalVideos, double percentage, boolean completed) {}

    static Result compute(ChapterRepository chapterRepository, VideoRepository videoRepository,
                          String courseId, List<String> completedVideoIds) {

        List<String> chapterIds = chapterRepository.findByCourseIdOrderByOrderAsc(courseId).stream()
                .map(Chapter::getId)
                .toList();

        Set<String> validVideoIds = chapterIds.isEmpty()
                ? Set.of()
                : videoRepository.findByChapterIdIn(chapterIds).stream()
                .map(Video::getId)
                .collect(Collectors.toSet());

        long totalVideos = validVideoIds.size();
        long completedCount = completedVideoIds.stream().filter(validVideoIds::contains).count();

        double percentage = totalVideos == 0 ? 0.0 : Math.round((completedCount * 100.0 / totalVideos) * 100.0) / 100.0;
        boolean completed = totalVideos > 0 && completedCount == totalVideos;

        return new Result(completedCount, totalVideos, percentage, completed);
    }
}