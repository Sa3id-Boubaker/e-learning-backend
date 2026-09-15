package com.test.trainingservice.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.test.trainingservice.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import java.io.File;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
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

    // --- Clés de réponse Cloudinary partagées entre upload image et upload vidéo ---
    private static final String PUBLIC_ID_KEY = "public_id";
    private static final String RESOURCE_TYPE_KEY = "resource_type";

    @Value("${training.recording.max-file-size-bytes}")
    private long maxRecordingFileSize;

    @Value("${training.recording.folder}")
    private String recordingFolder;

    /**
     * Disabled by default on purpose: Cloudinary's Java SDK makes uploadLarge() throw a
     * RuntimeException (not just skip the add-on) when the "Google AI Video Transcription"
     * add-on isn't actively subscribed on the account, which would break recording uploads
     * entirely. Only flip to true once the add-on is confirmed active (dashboard → Add-ons) —
     * same feature flag and same reasoning as course-service's CloudinaryService.
     */
    @Value("${cloudinary.video-transcription-enabled:false}")
    private boolean videoTranscriptionEnabled;

    private final Cloudinary cloudinary;

    /** URL publique à afficher, et public_id à conserver pour permettre une suppression future. */
    public record UploadResult(String url, String publicId) {}

    /**
     * Même idée que UploadResult, avec en plus la durée (en secondes) renvoyée par Cloudinary
     * et subtitleUrl — l'URL du fichier .vtt généré par l'add-on de transcription, null si
     * videoTranscriptionEnabled est false (comportement par défaut).
     */
    public record VideoUploadResult(String url, String publicId, Long durationSeconds, String subtitleUrl) {}

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
                    PUBLIC_ID_KEY, trainingKey + "-" + System.currentTimeMillis(),
                    "overwrite", true,
                    RESOURCE_TYPE_KEY, "image"
            ));

            return new UploadResult((String) result.get("secure_url"), (String) result.get(PUBLIC_ID_KEY));

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
            Map<String, Object> uploadOptions = new HashMap<>(ObjectUtils.asMap(
                    "folder", recordingFolder,
                    PUBLIC_ID_KEY, sessionKey + "-" + System.currentTimeMillis(),
                    RESOURCE_TYPE_KEY, "video",
                    "chunk_size", UPLOAD_CHUNK_SIZE
            ));
            if (videoTranscriptionEnabled) {
                uploadOptions.put("raw_convert", "google_speech");
            }

            Map<?, ?> result = cloudinary.uploader().uploadLarge(tempFile, uploadOptions);

            String url = (String) result.get("secure_url");
            String publicId = (String) result.get(PUBLIC_ID_KEY);
            Long durationSeconds = extractDurationSeconds(result);
            String subtitleUrl = videoTranscriptionEnabled ? buildSubtitleUrl(publicId) : null;

            return new VideoUploadResult(url, publicId, durationSeconds, subtitleUrl);

        } catch (IOException e) {
            throw new FileStorageException("Failed to upload the video to Cloudinary");
        } finally {
            try {
                Files.delete(tempFile.toPath());
            } catch (IOException e) {
                tempFile.deleteOnExit();
                log.warn("Failed to delete temporary recording file: {}", tempFile.getAbsolutePath(), e);
            }
        }
    }

    public void deleteVideoIfExists(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(RESOURCE_TYPE_KEY, "video"));
        } catch (IOException ignored) {
            // suppression "best-effort" : ne fait jamais échouer la requête principale
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

    /**
     * Builds the public URL of the .vtt transcript Cloudinary's "Google AI Video Transcription"
     * add-on generates asynchronously as <public_id>.transcript, exposed as .vtt. Only called
     * when videoTranscriptionEnabled is true — never used to guess a URL the add-on won't produce.
     */
    private String buildSubtitleUrl(String publicId) {
        return "https://res.cloudinary.com/" + cloudinary.config.cloudName + "/raw/upload/" + publicId + ".transcript.vtt";
    }
}