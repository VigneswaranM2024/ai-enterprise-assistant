-- =============================================================================
-- MIGRATION V3: ADD SSO AND USER PROFILE FIELDS TO USERS TABLE
-- =============================================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS external_sso_id VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS job_title VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;
