package com.test.courseservice.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.test.courseservice.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.nio.file.Files;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private static final String PUBLIC_ID_KEY = "public_id";
    private static final String RESOURCE_TYPE_KEY = "resource_type";

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

    // Desactive par defaut : l'add-on Cloudinary "Google AI Video Transcription" doit d'abord
    // etre active/souscrit sur le compte, sinon Cloudinary fait echouer l'upload ENTIER de la
    // video (pas juste la transcription) avec "You don't have an active subscription for Google
    // AI Video Transcription" -- confirme en test reel. Ne mettre a true qu'une fois l'add-on
    // actif cote dashboard Cloudinary.
    @Value("${cloudinary.video-transcription-enabled:false}")
    private boolean videoTranscriptionEnabled;

    /** duration et subtitleUrl restent null pour un upload d'image — utilisés uniquement pour les vidéos. */
    public record UploadResult(String url, String publicId, Integer duration, String subtitleUrl) {}

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
                    PUBLIC_ID_KEY, courseId + "-" + System.currentTimeMillis(),
                    "overwrite", true,
                    RESOURCE_TYPE_KEY, "image"
            ));

            return new UploadResult((String) result.get("secure_url"), (String) result.get(PUBLIC_ID_KEY), null, null);

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
        } catch (IOException e) {
            // Best-effort cleanup: si la suppression échoue côté Cloudinary, on ne bloque pas l'appelant.
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

            Map<String, Object> uploadOptions = new java.util.HashMap<>(ObjectUtils.asMap(
                    "folder", VIDEO_UPLOAD_FOLDER,
                    PUBLIC_ID_KEY, referenceId + "-" + System.currentTimeMillis(),
                    RESOURCE_TYPE_KEY, "video"
            ));
            if (videoTranscriptionEnabled) {
                // Declenche l'add-on "Google AI Video Transcription" cote Cloudinary. NE PAS activer
                // avant que l'add-on soit reellement souscrit sur le compte Cloudinary : sinon
                // Cloudinary rejette l'upload entier de la video (confirme en test reel).
                uploadOptions.put("raw_convert", "google_speech");
            }

            Map<?, ?> result = cloudinary.uploader().uploadLarge(tempFile, uploadOptions);

            Object durationValue = result.get("duration");
            Integer duration = durationValue != null
                    ? (int) Math.round(((Number) durationValue).doubleValue())
                    : null;

            String publicId = (String) result.get(PUBLIC_ID_KEY);
            // Construit l'URL du .vtt genere par l'add-on de transcription, seulement si on l'a
            // reellement demande cette fois-ci (sinon le fichier n'existera jamais -> null plutot
            // qu'une URL garantie en 404). Le fichier est produit de maniere asynchrone apres
            // l'upload : cette URL peut donc renvoyer 404 pendant un court moment le temps que la
            // transcription se termine -- un <track> HTML5 en 404 echoue silencieusement sans
            // jamais casser la lecture video (comportement standard du tag).
            String subtitleUrl = videoTranscriptionEnabled
                    ? "https://res.cloudinary.com/" + cloudinary.config.cloudName + "/raw/upload/" + publicId + ".transcript.vtt"
                    : null;

            return new UploadResult((String) result.get("secure_url"), publicId, duration, subtitleUrl);

        } catch (IOException e) {
            throw new FileStorageException("Failed to upload the video to Cloudinary");
        } finally {
            if (tempFile != null) {
                try {
                    Files.delete(tempFile.toPath());
                } catch (IOException e) {
                    log.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath(), e);
                }
            }
        }
    }

    public void deleteVideoIfExists(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(RESOURCE_TYPE_KEY, "video"));
        } catch (IOException e) {
            // Best-effort cleanup: si la suppression échoue côté Cloudinary, on ne bloque pas l'appelant.
        }
    }
}