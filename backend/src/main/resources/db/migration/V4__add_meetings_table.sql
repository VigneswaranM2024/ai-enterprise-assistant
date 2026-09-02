-- =============================================================================
-- V2__add_meetings_table.sql
-- Creates the missing meetings table for the Meeting Intelligence feature.
-- =============================================================================

CREATE TABLE IF NOT EXISTS meetings (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    meeting_date DATE NOT NULL,
    participants TEXT,
    transcript_document_id UUID REFERENCES documents(id) ON DELETE SET NULL,
    summary TEXT,
    decisions TEXT,
    action_items TEXT,
    risks TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_meetings_tenant_id ON meetings(tenant_id);
CREATE INDEX IF NOT EXISTS idx_meetings_meeting_date ON meetings(meeting_date);
