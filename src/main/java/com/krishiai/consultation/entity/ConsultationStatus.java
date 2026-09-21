package com.krishiai.consultation.entity;

public enum ConsultationStatus {
    REQUESTED,
    PENDING, // Legacy alias for REQUESTED
    ACCEPTED,
    PAYMENT_PENDING,
    PAID,
    ACTIVE,
    COMPLETED,
    RESOLVED, // Legacy alias for COMPLETED
    EXPIRED,
    REJECTED,
    CANCELLED,
    REFUND_PENDING,
    REFUNDED;

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isClosed() {
        return this == COMPLETED || this == RESOLVED || this == REJECTED || this == CANCELLED || this == EXPIRED || this == REFUNDED;
    }
}

