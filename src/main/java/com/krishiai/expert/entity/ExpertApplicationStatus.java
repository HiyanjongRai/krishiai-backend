package com.krishiai.expert.entity;

/**
 * Tracks where an expert's verification application currently sits in the admin review workflow.
 *
 * <p>Valid state transitions:
 * <pre>
 *   DRAFT ──► SUBMITTED ──► UNDER_REVIEW ──► APPROVED
 *                                │   ▲       └──► REJECTED ──► SUBMITTED
 *                                ▼   │
 *                   ADDITIONAL_INFORMATION_REQUIRED
 * </pre>
 */
public enum ExpertApplicationStatus {

    /**
     * The expert has registered but has not yet submitted a verification application.
     * The expert can log in, edit their profile, select crops, and upload documents.
     */
    DRAFT,

    /**
     * The expert has submitted the application; waiting for an admin to begin review.
     */
    SUBMITTED,

    /**
     * An administrator is actively reviewing the expert's credentials and documents.
     */
    UNDER_REVIEW,

    /**
     * The admin team requires additional documents or corrections before deciding.
     * The expert can update profile/documents and re-submit.
     */
    ADDITIONAL_INFORMATION_REQUIRED,

    /**
     * Admin has approved the expert. The expert is now a verified professional.
     */
    APPROVED,

    /**
     * Admin has rejected the application. The expert may correct feedback and re-submit.
     */
    REJECTED
}
