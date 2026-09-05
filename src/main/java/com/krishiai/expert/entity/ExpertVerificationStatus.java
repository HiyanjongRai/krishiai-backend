package com.krishiai.expert.entity;

/**
 * Represents the professional verification status of an ExpertProfile.
 *
 * <p>Separated from user account status (which is ACTIVE/SUSPENDED) and
 * application status (DRAFT/SUBMITTED/UNDER_REVIEW/etc.).
 */
public enum ExpertVerificationStatus {

    /**
     * Account is active, but the expert has not yet been approved by an administrator.
     * Can access onboarding, profile, and draft features, but not verified expert services.
     */
    UNVERIFIED,

    /**
     * Administrator has approved the expert. Can provide consultations, appear in searches,
     * and access verified advisory features.
     */
    VERIFIED,

    /**
     * Administrator rejected or revoked verification.
     */
    REJECTED
}
