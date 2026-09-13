package com.krishiai.media.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.media.constant.CloudinaryFolder;
import com.krishiai.media.dto.MediaResponse;
import com.krishiai.media.service.CloudinaryService;
import com.krishiai.security.userdetails.CustomUserDetails;
import com.krishiai.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final CloudinaryService cloudinaryService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MediaResponse>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false, defaultValue = "GENERAL") String folderName,
            @RequestParam(value = "resourceType", required = false, defaultValue = "image") String resourceType,
            @AuthenticationPrincipal CustomUserDetails principal) {

        CloudinaryFolder folder = CloudinaryFolder.fromString(folderName);
        MediaResponse response = cloudinaryService.uploadMedia(
                file, folder, principal.getUserId(), resourceType);

        return ResponseEntity.ok(ApiResponse.success("Media uploaded successfully to cloud storage", response));
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @RequestParam("publicId") String publicId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(UserRole.ROLE_ADMIN.name()));

        cloudinaryService.deleteMedia(publicId, principal.getUserId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Media deleted successfully from cloud storage", null));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MediaResponse>> getMedia(
            @RequestParam("publicId") String publicId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(UserRole.ROLE_ADMIN.name()));

        MediaResponse response = cloudinaryService.getMediaByPublicId(publicId, principal.getUserId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Media retrieved successfully", response));
    }
}
