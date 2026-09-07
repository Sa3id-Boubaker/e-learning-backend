package com.test.trainingservice.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.test.trainingservice.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import java.io.File;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB
    private static final String UPLOAD_FOLDER = "omarise/training-images";

    // --- Recording videos ---

    private static final Set<String> ALLOWED_VIDEO_CONTENT_TYPES = Set.of(
            "video/mp4", "video/webm", "video/quicktime", "video/x-msvideo", "video/x-matroska"
    );
    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of(
            "mp4", "webm", "mov", "avi", "mkv"
    );
    private static final long UPLOAD_CHUNK_SIZE = 20_000_000L; // 20 MB

    @Value("${training.recording.max-file-size-bytes}")
    private long maxRecordingFileSize;

    @Value("${training.recording.folder}")
    private String recordingFolder;

    private final Cloudinary cloudinary;

    /** URL publique à afficher, et public_id à conserver pour permettre une suppression future. */
    public record UploadResult(String url, String publicId) {}

    public record VideoUploadResult(String url, String publicId, Long durationSeconds) {}

    public UploadResult uploadTrainingImage(MultipartFile file, String trainingKey) {

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
                    "public_id", trainingKey + "-" + System.currentTimeMillis(),
                    "overwrite", true,
                    "resource_type", "image"
            ));

            return new UploadResult((String) result.get("secure_url"), (String) result.get("public_id"));

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
            // suppression "best-effort" : ne fait jamais échouer la requête principale
        }
    }

    public VideoUploadResult uploadRecordingVideo(MultipartFile file, String sessionKey) {

        validateVideoFile(file);

        File tempFile;
        try {
            tempFile = File.createTempFile("recording-" + sessionKey + "-", tempSuffix(file));
            file.transferTo(tempFile);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read the uploaded video file");
        }

        try {
            Map<?, ?> result = cloudinary.uploader().uploadLarge(tempFile, ObjectUtils.asMap(
                    "folder", recordingFolder,
                    "public_id", sessionKey + "-" + System.currentTimeMillis(),
                    "resource_type", "video",
                    "chunk_size", UPLOAD_CHUNK_SIZE
            ));

            String url = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            Long durationSeconds = extractDurationSeconds(result);

            return new VideoUploadResult(url, publicId, durationSeconds);

        } catch (IOException e) {
            throw new FileStorageException("Failed to upload the video to Cloudinary");
        } finally {
            if (!tempFile.delete()) {
                tempFile.deleteOnExit();
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

    private void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("No file provided");
        }

        if (file.getSize() > maxRecordingFileSize) {
            throw new FileStorageException("File size exceeds the configured maximum limit for recordings");
        }

        boolean validContentType = file.getContentType() != null
                && ALLOWED_VIDEO_CONTENT_TYPES.contains(file.getContentType());
        boolean validExtension = hasAllowedVideoExtension(file.getOriginalFilename());

        if (!validContentType && !validExtension) {
            throw new FileStorageException("Only video files are allowed (mp4, webm, mov, avi, mkv)");
        }
    }

    private boolean hasAllowedVideoExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_VIDEO_EXTENSIONS.contains(extension);
    }

    private String tempSuffix(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.'));
        }
        return ".tmp";
    }

    private Long extractDurationSeconds(Map<?, ?> result) {
        Object duration = result.get("duration");
        if (duration instanceof Number number) {
            return Math.round(number.doubleValue());
        }
        return null;
    }
}