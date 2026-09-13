package com.krishiai.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.krishiai.common.exception.ForbiddenException;
import com.krishiai.common.exception.MediaUploadException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.media.constant.CloudinaryFolder;
import com.krishiai.media.dto.MediaResponse;
import com.krishiai.media.entity.MediaFile;
import com.krishiai.media.repository.MediaFileRepository;
import com.krishiai.user.entity.User;
import com.krishiai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;
    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024; // 10 MB
    private static final long MAX_VIDEO_SIZE = 50L * 1024 * 1024; // 50 MB
    private static final long MAX_DOCUMENT_SIZE = 25L * 1024 * 1024; // 25 MB

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/quicktime", "video/webm", "video/x-matroska", "video/mpeg"
    );

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/webp"
    );

    @Override
    @Transactional
    public MediaResponse uploadImage(MultipartFile file, CloudinaryFolder folder, Long userId) {
        validateFile(file, ALLOWED_IMAGE_TYPES, MAX_IMAGE_SIZE, "image");
        return uploadToCloudinary(file, folder, userId, "image");
    }

    @Override
    @Transactional
    public MediaResponse uploadVideo(MultipartFile file, CloudinaryFolder folder, Long userId) {
        validateFile(file, ALLOWED_VIDEO_TYPES, MAX_VIDEO_SIZE, "video");
        return uploadToCloudinary(file, folder, userId, "video");
    }

    @Override
    @Transactional
    public MediaResponse uploadDocument(MultipartFile file, CloudinaryFolder folder, Long userId) {
        validateFile(file, ALLOWED_DOCUMENT_TYPES, MAX_DOCUMENT_SIZE, "document");
        String resourceType = file.getContentType() != null && file.getContentType().startsWith("image/") ? "image" : "auto";
        return uploadToCloudinary(file, folder, userId, resourceType);
    }

    @Override
    @Transactional
    public MediaResponse uploadMedia(MultipartFile file, CloudinaryFolder folder, Long userId, String resourceType) {
        String normalizedResourceType = resourceType == null || resourceType.isBlank()
                ? "image"
                : resourceType.trim().toLowerCase(Locale.ROOT);

        return switch (normalizedResourceType) {
            case "image" -> uploadImage(file, folder, userId);
            case "video" -> uploadVideo(file, folder, userId);
            case "document" -> uploadDocument(file, folder, userId);
            default -> throw new MediaUploadException("Unsupported media resource type: " + resourceType);
        };
    }

    @Override
    @Transactional
    public void deleteMedia(String publicId, Long requesterUserId, boolean isAdmin) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        MediaFile mediaFile = mediaFileRepository.findByPublicId(publicId).orElse(null);

        if (mediaFile == null && !isAdmin) {
            throw new ResourceNotFoundException("Media not found for public ID: " + publicId);
        }

        if (mediaFile != null && !isAdmin && requesterUserId != null) {
            if (mediaFile.getUploadedBy() != null && !mediaFile.getUploadedBy().getId().equals(requesterUserId)) {
                throw new ForbiddenException("You do not have permission to delete this media file.");
            }
        }

        try {
            String resourceType = mediaFile != null ? mediaFile.getResourceType() : "image";
            Map<?, ?> destroyOptions = ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "invalidate", true
            );
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, destroyOptions);
            log.info("Cloudinary destroy result for publicId {}: {}", publicId, result);

            if (mediaFile != null) {
                mediaFileRepository.delete(mediaFile);
            }
        } catch (IOException e) {
            log.error("Failed to delete media from Cloudinary for publicId {}: {}", publicId, e.getMessage(), e);
            throw new MediaUploadException("Failed to delete media from cloud storage: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MediaResponse getMediaByPublicId(String publicId, Long requesterUserId, boolean isAdmin) {
        MediaFile mediaFile = getMediaEntityByPublicId(publicId);
        if (!isAdmin && requesterUserId != null && mediaFile.getUploadedBy() != null) {
            if (!mediaFile.getUploadedBy().getId().equals(requesterUserId)) {
                throw new ForbiddenException("You do not have permission to view this media file metadata.");
            }
        }
        return MediaResponse.from(mediaFile);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaFile getMediaEntityByPublicId(String publicId) {
        return mediaFileRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found for public ID: " + publicId));
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file, Set<String> allowedTypes, long maxSize, String categoryName) {
        if (file == null || file.isEmpty()) {
            throw new MediaUploadException("Please select a valid, non-empty file to upload.");
        }

        if (file.getSize() > maxSize) {
            long maxMb = maxSize / (1024 * 1024);
            throw new MediaUploadException(String.format("File size exceeds the maximum limit of %d MB for %ss.", maxMb, categoryName));
        }

        String contentType = file.getContentType();
        String normalizedContentType = contentType != null ? contentType.toLowerCase(Locale.ROOT) : null;
        if (normalizedContentType == null || !allowedTypes.contains(normalizedContentType)) {
            throw new MediaUploadException(String.format("Invalid file format (%s). Allowed formats: %s",
                    contentType != null ? contentType : "unknown", String.join(", ", allowedTypes)));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.length() > 255) {
            throw new MediaUploadException("File name is too long.");
        }
    }

    private MediaResponse uploadToCloudinary(MultipartFile file, CloudinaryFolder folder, Long userId, String resourceType) {
        try {
            User user = null;
            if (userId != null) {
                user = userRepository.findById(userId).orElse(null);
            }

            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", folder.getPath(),
                    "resource_type", resourceType != null ? resourceType : "auto",
                    "use_filename", true,
                    "unique_filename", true,
                    "overwrite", false
            );

            log.info("Uploading media ({} bytes, type '{}') to Cloudinary folder '{}'",
                    file.getSize(), file.getContentType(), folder.getPath());

            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

            String publicId = (String) uploadResult.get("public_id");
            String secureUrl = (String) uploadResult.get("secure_url");
            String format = (String) uploadResult.get("format");
            Object resTypeObj = uploadResult.get("resource_type");
            String resType = resTypeObj != null ? resTypeObj.toString() : resourceType;
            Number bytesNumber = (Number) uploadResult.get("bytes");
            Long bytes = bytesNumber != null ? bytesNumber.longValue() : file.getSize();

            Integer width = uploadResult.get("width") instanceof Number n ? n.intValue() : null;
            Integer height = uploadResult.get("height") instanceof Number n ? n.intValue() : null;

            MediaFile mediaFile = new MediaFile(
                    publicId,
                    secureUrl,
                    file.getOriginalFilename(),
                    format,
                    resType,
                    bytes,
                    width,
                    height,
                    folder.getPath(),
                    user
            );

            MediaFile saved = mediaFileRepository.save(mediaFile);
            log.info("Successfully uploaded media to Cloudinary. Public ID: {}", publicId);
            return MediaResponse.from(saved);

        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage(), e);
            throw new MediaUploadException("Failed to upload file to cloud storage: " + e.getMessage(), e);
        }
    }
}
