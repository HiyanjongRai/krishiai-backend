package com.krishiai.media.dto;

import com.krishiai.media.entity.MediaFile;

import java.time.LocalDateTime;

public record MediaResponse(
        Long id,
        String publicId,
        String secureUrl,
        String originalFilename,
        String format,
        String resourceType,
        Long bytes,
        Integer width,
        Integer height,
        String folder,
        LocalDateTime createdAt
) {
    public static MediaResponse from(MediaFile mf) {
        if (mf == null) return null;
        return new MediaResponse(
                mf.getId(),
                mf.getPublicId(),
                mf.getSecureUrl(),
                mf.getOriginalFilename(),
                mf.getFormat(),
                mf.getResourceType(),
                mf.getBytes(),
                mf.getWidth(),
                mf.getHeight(),
                mf.getFolder(),
                mf.getCreatedAt()
        );
    }
}
