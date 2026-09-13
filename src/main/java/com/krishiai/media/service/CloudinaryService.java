package com.krishiai.media.service;

import com.krishiai.media.constant.CloudinaryFolder;
import com.krishiai.media.dto.MediaResponse;
import com.krishiai.media.entity.MediaFile;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    MediaResponse uploadImage(MultipartFile file, CloudinaryFolder folder, Long userId);

    MediaResponse uploadVideo(MultipartFile file, CloudinaryFolder folder, Long userId);

    MediaResponse uploadDocument(MultipartFile file, CloudinaryFolder folder, Long userId);

    MediaResponse uploadMedia(MultipartFile file, CloudinaryFolder folder, Long userId, String resourceType);

    void deleteMedia(String publicId, Long requesterUserId, boolean isAdmin);

    MediaResponse getMediaByPublicId(String publicId, Long requesterUserId, boolean isAdmin);

    MediaFile getMediaEntityByPublicId(String publicId);
}
