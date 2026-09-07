package projecteLearning.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import projecteLearning.exception.FileStorageException;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB
    private static final String UPLOAD_FOLDER = "omarise/profile-images";

    private final Cloudinary cloudinary;

    /** URL publique à afficher, et public_id à conserver pour permettre une suppression future. */
    public record UploadResult(String url, String publicId) {}

    public UploadResult uploadProfileImage(MultipartFile file, String userId) {

        if (file == null || file.isEmpty()) {
            throw new FileStorageException("No file provided");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("File size exceeds the 5MB limit");
        }

        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new FileStorageException("Only image files are allowed (jpg, jpeg, png, gif, webp)");
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", UPLOAD_FOLDER,
                    "public_id", userId + "-" + System.currentTimeMillis(),
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
}