-- V4__consultation_messaging_system.sql
-- Production database migration for Real-Time Messaging and Consultation System

-- 1. Upgrade consultations table with lifecycle timestamps & subject
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS subject VARCHAR(255);
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP;
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP;
ALTER TABLE consultations ALTER COLUMN status TYPE VARCHAR(30);

-- Populate subject from title if subject is null
UPDATE consultations SET subject = title WHERE subject IS NULL AND title IS NOT NULL;

-- 2. Conversations table
CREATE TABLE IF NOT EXISTS conversations (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(30) NOT NULL DEFAULT 'CONSULTATION',
    consultation_id BIGINT UNIQUE,
    title VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conversations_consultation FOREIGN KEY (consultation_id) REFERENCES consultations(id) ON DELETE SET NULL
);

-- 3. Conversation members table
CREATE TABLE IF NOT EXISTS conversation_members (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_read_message_id BIGINT,
    CONSTRAINT fk_cm_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_cm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_conversation_member UNIQUE (conversation_id, user_id)
);

-- 4. Messages table with Cloudinary attachment support
CREATE TABLE IF NOT EXISTS messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    attachment_url VARCHAR(500),
    attachment_public_id VARCHAR(255),
    attachment_mime_type VARCHAR(100),
    attachment_file_size BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 5. Message reports for moderation
CREATE TABLE IF NOT EXISTS message_reports (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    reported_by BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by BIGINT,
    CONSTRAINT fk_mr_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_mr_reporter FOREIGN KEY (reported_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_mr_resolver FOREIGN KEY (resolved_by) REFERENCES users(id) ON DELETE SET NULL
);

-- 6. Announcements table
CREATE TABLE IF NOT EXISTS announcements (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_by BIGINT NOT NULL,
    target_role VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    CONSTRAINT fk_announcements_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

-- 7. Optimized indexes for fast conversation lookups, membership checks, and message pagination
CREATE INDEX IF NOT EXISTS idx_messages_conv_created ON messages(conversation_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_conv_id ON messages(conversation_id, id DESC);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_cm_user ON conversation_members(user_id);
CREATE INDEX IF NOT EXISTS idx_cm_conv ON conversation_members(conversation_id);
CREATE INDEX IF NOT EXISTS idx_consultations_farmer_status ON consultations(farmer_id, status);
CREATE INDEX IF NOT EXISTS idx_consultations_expert_status ON consultations(expert_id, status);
CREATE INDEX IF NOT EXISTS idx_announcements_target_role ON announcements(target_role);
