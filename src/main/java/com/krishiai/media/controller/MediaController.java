package com.krishiai.media.controller;

import com.krishiai.common.exception.BadRequestException;
import com.krishiai.common.exception.ForbiddenException;
import com.krishiai.common.response.ApiResponse;
import com.krishiai.media.constant.CloudinaryFolder;
import com.krishiai.media.dto.MediaResponse;
import com.krishiai.media.service.CloudinaryService;
import com.krishiai.security.userdetails.CustomUserDetails;
import com.krishiai.user.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/media")
@Validated
@RequiredArgsConstructor
public class MediaController {

    private final CloudinaryService cloudinaryService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MediaResponse>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false, defaultValue = "GENERAL")
            @Size(max = 50, message = "Folder name must not exceed 50 characters")
            String folderName,
            @RequestParam(value = "resourceType", required = false, defaultValue = "image")
            @Pattern(regexp = "image|video|document", message = "Resource type must be image, video, or document")
            String resourceType,
            @AuthenticationPrincipal CustomUserDetails principal) {

        CloudinaryFolder folder = parseFolder(folderName);
        enforceFolderAccess(folder, principal);
        MediaResponse response = cloudinaryService.uploadMedia(
                file, folder, principal.getUserId(), resourceType);

        return ResponseEntity.ok(ApiResponse.success("Media uploaded successfully to cloud storage", response));
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @RequestParam("publicId") @NotBlank @Size(max = 255) String publicId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(UserRole.ROLE_ADMIN.name()));

        cloudinaryService.deleteMedia(publicId, principal.getUserId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Media deleted successfully from cloud storage", null));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MediaResponse>> getMedia(
            @RequestParam("publicId") @NotBlank @Size(max = 255) String publicId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(UserRole.ROLE_ADMIN.name()));

        MediaResponse response = cloudinaryService.getMediaByPublicId(publicId, principal.getUserId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Media retrieved successfully", response));
    }

    private CloudinaryFolder parseFolder(String folderName) {
        for (CloudinaryFolder folder : CloudinaryFolder.values()) {
            if (folder.name().equalsIgnoreCase(folderName) || folder.getPath().equalsIgnoreCase(folderName)) {
                return folder;
            }
        }
        throw new BadRequestException("Unsupported media folder: " + folderName);
    }

    private void enforceFolderAccess(CloudinaryFolder folder, CustomUserDetails principal) {
        boolean isAdmin = hasRole(principal, UserRole.ROLE_ADMIN);
        boolean isExpert = hasRole(principal, UserRole.ROLE_EXPERT);
        boolean isFarmer = hasRole(principal, UserRole.ROLE_FARMER);

        boolean allowed = switch (folder) {
            case GENERAL, USER_PROFILES -> true;
            case EXPERT_DOCUMENTS -> isExpert || isAdmin;
            case FARMER_IMAGES, DISEASE_IMAGES, AI_ANALYSIS -> isFarmer || isAdmin;
            case CROP_IMAGES -> isAdmin;
        };

        if (!allowed) {
            throw new ForbiddenException("You do not have permission to upload media to " + folder.name());
        }
    }

    private boolean hasRole(CustomUserDetails principal, UserRole role) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role.name()));
    }
}
