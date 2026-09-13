package com.krishiai.user.service;

import com.krishiai.common.exception.ConflictException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.user.dto.UserResponse;
import com.krishiai.user.dto.UserUpdateRequest;
import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import com.krishiai.user.entity.UserStatus;
import com.krishiai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final com.krishiai.media.service.CloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        User user = getUserEntity(userId);
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UserUpdateRequest request) {
        User user = getUserEntity(userId);

        user.setFullName(request.fullName().strip());

        if (request.profileImage() != null) {
            user.setProfileImage(request.profileImage().strip());
        }

        if (request.phone() != null && !request.phone().isBlank()) {
            String cleanPhone = request.phone().strip();
            if (!cleanPhone.equals(user.getPhone()) && userRepository.existsByPhone(cleanPhone)) {
                throw new ConflictException("Phone number is already in use by another account");
            }
            user.setPhone(cleanPhone);
        } else {
            user.setPhone(null);
        }

        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByRole(UserRole role, Pageable pageable) {
        return userRepository.findByRole(role, pageable)
                .map(UserResponse::from);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long userId, UserStatus status) {
        User user = getUserEntity(userId);
        user.setStatus(status);
        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    @Override
    @Transactional
    public UserResponse uploadProfileImage(Long userId, org.springframework.web.multipart.MultipartFile file) {
        User user = getUserEntity(userId);

        // If user already has an existing Cloudinary image, clean it up
        if (user.getProfileImagePublicId() != null && !user.getProfileImagePublicId().isBlank()) {
            try {
                cloudinaryService.deleteMedia(user.getProfileImagePublicId(), userId, true);
            } catch (Exception ex) {
                // Log and continue with new upload so user is not blocked
                org.slf4j.LoggerFactory.getLogger(UserServiceImpl.class)
                        .warn("Failed to remove old profile image for user {}: {}", userId, ex.getMessage());
            }
        }

        com.krishiai.media.dto.MediaResponse mediaResponse =
                cloudinaryService.uploadImage(file, com.krishiai.media.constant.CloudinaryFolder.USER_PROFILES, userId);

        user.setProfileImage(mediaResponse.secureUrl());
        user.setProfileImagePublicId(mediaResponse.publicId());
        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    @Override
    @Transactional
    public UserResponse removeProfileImage(Long userId) {
        User user = getUserEntity(userId);

        if (user.getProfileImagePublicId() != null && !user.getProfileImagePublicId().isBlank()) {
            cloudinaryService.deleteMedia(user.getProfileImagePublicId(), userId, true);
        }

        user.setProfileImage(null);
        user.setProfileImagePublicId(null);
        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }
}
