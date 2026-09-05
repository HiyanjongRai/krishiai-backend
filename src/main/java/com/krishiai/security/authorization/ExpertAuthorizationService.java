package com.krishiai.security.authorization;

import com.krishiai.common.exception.ForbiddenException;
import com.krishiai.expert.entity.ExpertApplicationStatus;
import com.krishiai.expert.entity.ExpertProfile;
import com.krishiai.expert.entity.ExpertVerificationStatus;
import com.krishiai.expert.repository.ExpertProfileRepository;
import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import com.krishiai.user.entity.UserStatus;
import com.krishiai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Centralized authorization service for expert operations and verification policies.
 *
 * <p>Enforces the rule:
 * Frontend restrictions are UX only. Spring Boot service/security layer must enforce
 * all verified-only permissions and resource ownership.
 *
 * <p>Can be referenced in SpEL: {@code @PreAuthorize("@expertAuth.isVerifiedExpert(principal.userId)")}
 */
@Slf4j
@Component("expertAuth")
@RequiredArgsConstructor
public class ExpertAuthorizationService {

    private final UserRepository userRepository;
    private final ExpertProfileRepository expertProfileRepository;

    /**
     * Checks if the authenticated user is an ACTIVE, verified expert (or an ADMIN).
     *
     * <p>Both User.status == ACTIVE and ExpertProfile.verificationStatus == VERIFIED are required.
     */
    public boolean isVerifiedExpert(Long userId) {
        if (userId == null) {
            return false;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Access denied: User {} is not active (status={})", userId, user != null ? user.getStatus() : "null");
            return false;
        }

        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return true;
        }

        if (user.getRole() != UserRole.ROLE_EXPERT) {
            return false;
        }

        ExpertProfile profile = expertProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) {
            log.warn("Access denied: No expert profile for userId={}", userId);
            return false;
        }

        boolean verified = (profile.getVerificationStatus() == ExpertVerificationStatus.VERIFIED
                || profile.isVerifiedExpert())
                && profile.getApplicationStatus() == ExpertApplicationStatus.APPROVED;

        if (!verified) {
            log.warn("Access denied: Expert {} is not verified (verificationStatus={}, applicationStatus={})",
                    userId, profile.getVerificationStatus(), profile.getApplicationStatus());
        }

        return verified;
    }

    /**
     * Assert that the user is verified, or throw ForbiddenException with a clean message.
     */
    public void requireVerifiedExpert(Long userId) {
        if (!isVerifiedExpert(userId)) {
            throw new ForbiddenException(
                    "Verification Required: Your expert account has not been approved yet. " +
                    "Complete your application and wait for admin verification to access verified expert features."
            );
        }
    }

    /**
     * Checks if the expert owns the specified profile ID.
     */
    public boolean isProfileOwner(Long userId, Long profileId) {
        if (userId == null || profileId == null) {
            return false;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getRole() == UserRole.ROLE_ADMIN) {
            return true;
        }

        return expertProfileRepository.findById(profileId)
                .map(ep -> ep.getUser() != null && ep.getUser().getId().equals(userId))
                .orElse(false);
    }

    /**
     * Checks if the expert can edit/modify the application.
     * Allowed only in DRAFT, REJECTED, or ADDITIONAL_INFORMATION_REQUIRED states.
     */
    public boolean canModifyApplication(Long userId) {
        if (userId == null) return false;
        ExpertProfile profile = expertProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) return false;

        ExpertApplicationStatus status = profile.getApplicationStatus();
        return status == ExpertApplicationStatus.DRAFT
                || status == ExpertApplicationStatus.REJECTED
                || status == ExpertApplicationStatus.ADDITIONAL_INFORMATION_REQUIRED;
    }
}
