package com.krishiai.expert.entity;

/**
 * Tracks the verification status of an individual crop or agricultural expertise claim.
 *
 * <p>Lifecycle:
 * <ul>
 *   <li>Expert claims expertise -> SELF_DECLARED (no verification completed)</li>
 *   <li>Expert uploads/submits supporting evidence -> EVIDENCE_SUBMITTED</li>
 *   <li>Admin approves/verifies -> VERIFIED</li>
 *   <li>Admin rejects with reason -> REJECTED</li>
 * </ul>
 */
public enum CropExpertiseVerificationStatus {

    /**
     * Legacy status retained for backward compatibility with existing databases/APIs.
     * Mapped to SELF_DECLARED in business logic.
     */
    @Deprecated
    PENDING,

    /**
     * Expert claims this expertise without formal verification.
     * Displayed as "Self-declared". Does NOT receive a "Verified Expertise" badge.
     */
    SELF_DECLARED,

    /**
     * Expert has provided supporting evidence (certificates, licenses, etc.).
     * Waiting for administrator inspection.
     */
    EVIDENCE_SUBMITTED,

    /**
     * Confirmed by admin with supporting credentials or review.
     * Receives the verified badge and priority in farmer search matching.
     */
    VERIFIED,

    /**
     * Rejected by admin due to insufficient credentials or evidence.
     * Requires a rejection reason.
     */
    REJECTED
}
