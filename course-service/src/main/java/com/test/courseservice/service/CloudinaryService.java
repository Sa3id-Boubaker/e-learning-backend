package com.test.courseservice.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.test.courseservice.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    // -- Images --
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB
    private static final String UPLOAD_FOLDER = "omarise/course-images";

    // -- Vidéos --
    private static final Set<String> ALLOWED_VIDEO_CONTENT_TYPES = Set.of(
            "video/mp4", "video/webm", "video/quicktime" // .mov
    );
    private static final long MAX_VIDEO_SIZE = 500L * 1024 * 1024; // 500 MB
    private static final String VIDEO_UPLOAD_FOLDER = "omarise/course-videos";

    private final Cloudinary cloudinary;

    /** duration reste null pour un upload d'image — utilisé uniquement pour les vidéos. */
    public record UploadResult(String url, String publicId, Integer duration) {}

    public UploadResult uploadCourseImage(MultipartFile file, String courseId) {

        if (file == null || file.isEmpty()) {
            throw new FileStorageException("No file provided");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("File size exceeds the 10MB limit");
        }

        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new FileStorageException("Only image files are allowed (jpg, jpeg, png, gif, webp)");
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", UPLOAD_FOLDER,
                    "public_id", courseId + "-" + System.currentTimeMillis(),
                    "overwrite", true,
                    "resource_type", "image"
            ));

            return new UploadResult((String) result.get("secure_url"), (String) result.get("public_id"), null);

        } catch (IOException e) {
            throw new FileStorageException("Failed to upload the image to Cloudinary");
        }
    }

    public void deleteIfExists(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException ignored) {
        }
    }

    public UploadResult uploadVideo(MultipartFile file, String referenceId) {

        if (file == null || file.isEmpty()) {
            throw new FileStorageException("No file provided");
        }

        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new FileStorageException("File size exceeds the 500MB limit");
        }

        if (file.getContentType() == null || !ALLOWED_VIDEO_CONTENT_TYPES.contains(file.getContentType())) {
            throw new FileStorageException("Only video files are allowed (mp4, webm, mov)");
        }

        File tempFile = null;
        try {
            tempFile = File.createTempFile("video-upload-", ".tmp");
            file.transferTo(tempFile);

            Map<?, ?> result = cloudinary.uploader().uploadLarge(tempFile, ObjectUtils.asMap(
                    "folder", VIDEO_UPLOAD_FOLDER,
                    "public_id", referenceId + "-" + System.currentTimeMillis(),
                    "resource_type", "video"
            ));

            Object durationValue = result.get("duration");
            Integer duration = durationValue != null
                    ? (int) Math.round(((Number) durationValue).doubleValue())
                    : null;

            return new UploadResult((String) result.get("secure_url"), (String) result.get("public_id"), duration);

        } catch (IOException e) {
            throw new FileStorageException("Failed to upload the video to Cloudinary");
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    public void deleteVideoIfExists(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "video"));
        } catch (IOException ignored) {
        }
    }
}