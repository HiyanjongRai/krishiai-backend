-- V6__fix_consultation_status_check_constraint.sql
-- Drops the stale consultations_status_check constraint (only contained original values)
-- and recreates it with ALL current ConsultationStatus enum values:
-- REQUESTED, PENDING, ACCEPTED, PAYMENT_PENDING, PAID, ACTIVE,
-- COMPLETED, RESOLVED, EXPIRED, REJECTED, CANCELLED, REFUND_PENDING, REFUNDED

DO $$
BEGIN
    -- Drop existing check constraint if it exists (name from Hibernate or original migration)
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'consultations_status_check'
    ) THEN
        ALTER TABLE consultations DROP CONSTRAINT consultations_status_check;
    END IF;
END $$;

-- Recreate with all current status values
ALTER TABLE consultations
    ADD CONSTRAINT consultations_status_check
    CHECK (status IN (
        'REQUESTED',
        'PENDING',
        'ACCEPTED',
        'PAYMENT_PENDING',
        'PAID',
        'ACTIVE',
        'COMPLETED',
        'RESOLVED',
        'EXPIRED',
        'REJECTED',
        'CANCELLED',
        'REFUND_PENDING',
        'REFUNDED'
    ));
