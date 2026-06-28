package com.legalaid.document;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    // ── Allowed MIME types ───────────────────────────────────
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            // Images
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            // PDFs
            "application/pdf",
            // Videos
            "video/mp4",
            "video/quicktime",
            "video/x-msvideo",  // .avi
            "video/webm"
    );

    // Max file size — 50MB
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    // ── Upload file to Cloudinary ────────────────────────────
    // Returns a map with: url, public_id, bytes, format, resource_type
    public Map<String, Object> upload(MultipartFile file,
                                      String folder,
                                      UUID ownerId) throws IOException {
        // Validate mime type
        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException(
                    "File type not allowed. Allowed: images, PDFs, videos.");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File too large. Maximum size is 50MB.");
        }

        // Build Cloudinary folder path: legalaid/{ownerId}/{folder}
        String cloudinaryFolder = "legalaid/" + ownerId
                + (folder != null ? "/" + folder : "");

        // Determine resource type based on mime type
        String resourceType = resolveResourceType(mimeType);

        Map<String, Object> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder",        cloudinaryFolder,
                        "resource_type", resourceType,
                        "use_filename",  true,
                        "unique_filename", true,
                        // Auto-generate thumbnail for videos and images
                        "eager", resourceType.equals("video")
                                ? List.of(ObjectUtils.asMap(
                                "width", 300, "height", 300,
                                "crop", "pad", "format", "jpg"))
                                : List.of()
                )
        );

        log.info("Uploaded file to Cloudinary: {} for user: {}",
                uploadResult.get("public_id"), ownerId);

        return uploadResult;
    }

    // ── Delete file from Cloudinary ──────────────────────────
    public void delete(String cloudinaryId, String mimeType) {
        try {
            String resourceType = resolveResourceType(mimeType);
            cloudinary.uploader().destroy(
                    cloudinaryId,
                    ObjectUtils.asMap("resource_type", resourceType)
            );
            log.info("Deleted from Cloudinary: {}", cloudinaryId);
        } catch (IOException e) {
            // Log but don't throw — DB record will still be soft deleted
            // Orphaned Cloudinary files can be cleaned up later
            log.error("Failed to delete from Cloudinary: {}", cloudinaryId, e);
        }
    }

    // ── Determine Cloudinary resource_type ───────────────────
    private String resolveResourceType(String mimeType) {
        if (mimeType == null) return "auto";
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        return "raw";  // PDFs and other files
    }
}