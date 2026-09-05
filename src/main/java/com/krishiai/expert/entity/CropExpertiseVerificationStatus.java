package com.krishiai.expert.entity;

/**
 * Tracks the individual verification status of a crop assigned to an expert profile.
 *
 * <p>Lifecycle:
 * Expert selects crop ──► PENDING
 * Admin reviews ──► VERIFIED or REJECTED
 */
public enum CropExpertiseVerificationStatus {

    /**
     * Claimed by expert, awaiting administrative verification.
     */
    PENDING,

    /**
     * Confirmed by admin with supporting credentials or review.
     * Only crops with this status appear in verified farmer search matching.
     */
    VERIFIED,

    /**
     * Rejected by admin due to insufficient credentials or evidence.
     */
    REJECTED
}
