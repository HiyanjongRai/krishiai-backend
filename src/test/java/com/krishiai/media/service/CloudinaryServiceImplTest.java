package com.krishiai.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.krishiai.common.exception.ForbiddenException;
import com.krishiai.common.exception.MediaUploadException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.media.constant.CloudinaryFolder;
import com.krishiai.media.dto.MediaResponse;
import com.krishiai.media.entity.MediaFile;
import com.krishiai.media.repository.MediaFileRepository;
import com.krishiai.user.entity.User;
import com.krishiai.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceImplTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @Mock
    private MediaFileRepository mediaFileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CloudinaryServiceImpl cloudinaryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(101L);
        testUser.setEmail("farmer@example.com");
    }

    @Test
    @DisplayName("Should successfully upload image and return MediaResponse")
    void uploadImage_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "dummy-image-bytes".getBytes()
        );

        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("public_id", "krishiai/users/profile/abc123xyz");
        uploadResult.put("secure_url", "https://res.cloudinary.com/dzfbwtpy7/image/upload/v1/krishiai/users/profile/abc123xyz.png");
        uploadResult.put("format", "png");
        uploadResult.put("resource_type", "image");
        uploadResult.put("bytes", 1024L);
        uploadResult.put("width", 500);
        uploadResult.put("height", 500);

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);
        when(userRepository.findById(101L)).thenReturn(Optional.of(testUser));
        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(invocation -> {
            MediaFile mf = invocation.getArgument(0);
            mf.setId(1L);
            return mf;
        });

        MediaResponse response = cloudinaryService.uploadImage(file, CloudinaryFolder.USER_PROFILES, 101L);

        assertThat(response).isNotNull();
        assertThat(response.publicId()).isEqualTo("krishiai/users/profile/abc123xyz");
        assertThat(response.secureUrl()).contains("cloudinary.com");
        assertThat(response.format()).isEqualTo("png");
        assertThat(response.folder()).isEqualTo(CloudinaryFolder.USER_PROFILES.getPath());

        verify(mediaFileRepository).save(any(MediaFile.class));
    }

    @Test
    @DisplayName("Should reject invalid MIME type for image")
    void uploadImage_InvalidMimeType_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "pdf-bytes".getBytes()
        );

        assertThatThrownBy(() -> cloudinaryService.uploadImage(file, CloudinaryFolder.USER_PROFILES, 101L))
                .isInstanceOf(MediaUploadException.class)
                .hasMessageContaining("Invalid file format");

        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("Should reject empty file")
    void uploadImage_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]
        );

        assertThatThrownBy(() -> cloudinaryService.uploadImage(emptyFile, CloudinaryFolder.USER_PROFILES, 101L))
                .isInstanceOf(MediaUploadException.class)
                .hasMessageContaining("Please select a valid, non-empty file");

        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("Should allow owner to delete their media")
    void deleteMedia_Owner_Success() throws IOException {
        MediaFile mediaFile = new MediaFile(
                "krishiai/users/profile/abc", "https://url", "avatar.png",
                "png", "image", 100L, 50, 50, "krishiai/users/profile", testUser
        );

        when(mediaFileRepository.findByPublicId("krishiai/users/profile/abc")).thenReturn(Optional.of(mediaFile));
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("krishiai/users/profile/abc"), anyMap())).thenReturn(Map.of("result", "ok"));

        cloudinaryService.deleteMedia("krishiai/users/profile/abc", 101L, false);

        verify(uploader).destroy(eq("krishiai/users/profile/abc"), anyMap());
        verify(mediaFileRepository).delete(mediaFile);
    }

    @Test
    @DisplayName("Should forbid non-owner non-admin from deleting media")
    void deleteMedia_NotOwner_ThrowsForbidden() {
        User differentUser = new User();
        differentUser.setId(999L);

        MediaFile mediaFile = new MediaFile(
                "krishiai/users/profile/abc", "https://url", "avatar.png",
                "png", "image", 100L, 50, 50, "krishiai/users/profile", differentUser
        );

        when(mediaFileRepository.findByPublicId("krishiai/users/profile/abc")).thenReturn(Optional.of(mediaFile));

        assertThatThrownBy(() -> cloudinaryService.deleteMedia("krishiai/users/profile/abc", 101L, false))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(cloudinary);
        verify(mediaFileRepository, never()).delete(any(MediaFile.class));
    }

    @Test
    @DisplayName("Should reject non-admin delete when media record is missing")
    void deleteMedia_MissingRecordNonAdmin_ThrowsNotFound() {
        when(mediaFileRepository.findByPublicId("krishiai/users/profile/missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cloudinaryService.deleteMedia("krishiai/users/profile/missing", 101L, false))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(cloudinary);
        verify(mediaFileRepository, never()).delete(any(MediaFile.class));
    }

    @Test
    @DisplayName("Should allow admin to delete any user's media")
    void deleteMedia_Admin_Success() throws IOException {
        User differentUser = new User();
        differentUser.setId(999L);

        MediaFile mediaFile = new MediaFile(
                "krishiai/users/profile/abc", "https://url", "avatar.png",
                "png", "image", 100L, 50, 50, "krishiai/users/profile", differentUser
        );

        when(mediaFileRepository.findByPublicId("krishiai/users/profile/abc")).thenReturn(Optional.of(mediaFile));
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("krishiai/users/profile/abc"), anyMap())).thenReturn(Map.of("result", "ok"));

        cloudinaryService.deleteMedia("krishiai/users/profile/abc", 101L, true);

        verify(uploader).destroy(eq("krishiai/users/profile/abc"), anyMap());
        verify(mediaFileRepository).delete(mediaFile);
    }
}
