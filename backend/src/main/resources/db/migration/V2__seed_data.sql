-- =============================================================================
-- AI ENTERPRISE ASSISTANT — SAMPLE SEED DATA (V2)
-- Seeds initial default tenant, admin user, permissions, and sample document chunks.
-- =============================================================================

-- 1. Insert Default Tenant
INSERT INTO tenants (id, name, slug, status, settings, created_at, updated_at)
VALUES (
    'c71a39d8-2122-4411-9a7b-3b8c21a41234',
    'Acme Corporation',
    'acme-corp',
    'ACTIVE',
    '{"allowedModels": ["gpt-4o", "gemini-embedding-2"], "maxDocumentSizeMb": 50}'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (slug) DO NOTHING;

-- 2. Insert Departments using dynamic Tenant ID lookup
INSERT INTO departments (id, tenant_id, name, code, created_at, updated_at)
SELECT 
    'd1111111-1111-1111-1111-111111111111'::uuid, id, 'Executive Management', 'EXEC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM tenants WHERE slug = 'acme-corp'
UNION ALL
SELECT 
    'd2222222-2222-2222-2222-222222222222'::uuid, id, 'Software Engineering', 'ENG', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM tenants WHERE slug = 'acme-corp'
UNION ALL
SELECT 
    'd3333333-3333-3333-3333-333333333333'::uuid, id, 'Finance & Accounting', 'FIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM tenants WHERE slug = 'acme-corp'
ON CONFLICT (tenant_id, code) DO NOTHING;

-- 3. Insert Permissions Catalog
INSERT INTO permissions (id, code, category, description, created_at)
VALUES
    ('11111111-0000-0000-0000-000000000001', 'USER_READ', 'USER_MGMT', 'View user profiles', CURRENT_TIMESTAMP),
    ('11111111-0000-0000-0000-000000000002', 'USER_WRITE', 'USER_MGMT', 'Create or edit users', CURRENT_TIMESTAMP),
    ('11111111-0000-0000-0000-000000000003', 'DOCUMENT_READ', 'DOC_MGMT', 'Search and read knowledge documents', CURRENT_TIMESTAMP),
    ('11111111-0000-0000-0000-000000000004', 'DOCUMENT_UPLOAD', 'DOC_MGMT', 'Upload documents to storage', CURRENT_TIMESTAMP),
    ('11111111-0000-0000-0000-000000000005', 'DOCUMENT_DOWNLOAD', 'DOC_MGMT', 'Download raw document files', CURRENT_TIMESTAMP),
    ('11111111-0000-0000-0000-000000000006', 'DOCUMENT_UPDATE', 'DOC_MGMT', 'Update document metadata', CURRENT_TIMESTAMP),
    ('11111111-0000-0000-0000-000000000007', 'DOCUMENT_DELETE', 'DOC_MGMT', 'Delete knowledge documents', CURRENT_TIMESTAMP),
    ('11111111-0000-0000-0000-000000000008', 'AI_QUERY', 'AI_ENGINE', 'Execute RAG search queries', CURRENT_TIMESTAMP),
    ('11111111-0000-0000-0000-000000000009', 'AUDIT_READ', 'GOVERNANCE', 'View system audit logs', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- 4. Insert Roles
INSERT INTO roles (id, tenant_id, name, description, is_system_role, created_at)
SELECT 'a1111111-1111-1111-1111-111111111111'::uuid, id, 'ROLE_ADMIN', 'Tenant System Administrator', true, CURRENT_TIMESTAMP FROM tenants WHERE slug = 'acme-corp'
UNION ALL
SELECT 'b2222222-2222-2222-2222-222222222222'::uuid, id, 'ROLE_EMPLOYEE', 'Standard Knowledge Worker', true, CURRENT_TIMESTAMP FROM tenants WHERE slug = 'acme-corp'
ON CONFLICT (tenant_id, name) DO NOTHING;

-- 5. Map Permissions to Roles using dynamic Role lookups
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r 
JOIN tenants t ON r.tenant_id = t.id AND t.slug = 'acme-corp' AND r.name = 'ROLE_ADMIN'
CROSS JOIN permissions p
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r 
JOIN tenants t ON r.tenant_id = t.id AND t.slug = 'acme-corp' AND r.name = 'ROLE_EMPLOYEE'
JOIN permissions p ON p.code IN ('DOCUMENT_READ', 'DOCUMENT_UPLOAD', 'DOCUMENT_DOWNLOAD', 'AI_QUERY')
ON CONFLICT DO NOTHING;

-- 6. Insert Sample Users
-- Password hash for 'Password123!': $2a$12$E91.vL7B8l0a6cZk703c3.rD1p6mQ4Y7vF8tS1N3a4b5c6d7e8f9g
INSERT INTO users (id, tenant_id, department_id, email, password_hash, full_name, security_classification, is_active, created_at, updated_at)
SELECT 
    'e1111111-1111-1111-1111-111111111111'::uuid, t.id, d.id, 'admin@acme.com', '$2a$12$E91.vL7B8l0a6cZk703c3.rD1p6mQ4Y7vF8tS1N3a4b5c6d7e8f9g', 'System Admin', 'RESTRICTED', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t JOIN departments d ON d.tenant_id = t.id AND d.code = 'EXEC' WHERE t.slug = 'acme-corp'
UNION ALL
SELECT 
    'e2222222-2222-2222-2222-222222222222'::uuid, t.id, d.id, 'sarah.connor@acme.com', '$2a$12$E91.vL7B8l0a6cZk703c3.rD1p6mQ4Y7vF8tS1N3a4b5c6d7e8f9g', 'Sarah Connor', 'INTERNAL', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t JOIN departments d ON d.tenant_id = t.id AND d.code = 'ENG' WHERE t.slug = 'acme-corp'
ON CONFLICT (tenant_id, email) DO NOTHING;

-- 7. Assign User Roles dynamically
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN tenants t ON u.tenant_id = t.id AND t.slug = 'acme-corp'
JOIN roles r ON r.tenant_id = t.id AND r.name = 'ROLE_ADMIN'
WHERE u.email = 'admin@acme.com'
UNION ALL
SELECT u.id, r.id
FROM users u
JOIN tenants t ON u.tenant_id = t.id AND t.slug = 'acme-corp'
JOIN roles r ON r.tenant_id = t.id AND r.name = 'ROLE_EMPLOYEE'
WHERE u.email = 'sarah.connor@acme.com'
ON CONFLICT DO NOTHING;

-- 8. Insert Sample Document
INSERT INTO documents (id, tenant_id, department_id, uploader_id, title, category, source_type, source_uri, mime_type, file_size_bytes, checksum, security_classification, allowed_roles, allowed_departments, status, version, created_at, updated_at)
SELECT 
    'f1111111-1111-1111-1111-111111111111'::uuid, t.id, d.id, u.id, 'Q3 Software Architecture Guidelines.pdf', 'GENERAL', 'FILE_UPLOAD', 's3://acme-docs/q3-arch-guidelines.pdf', 'application/pdf', 2097152, 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 'INTERNAL', ARRAY['ROLE_EMPLOYEE', 'ROLE_ADMIN'], ARRAY['ENG'], 'READY', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t 
JOIN departments d ON d.tenant_id = t.id AND d.code = 'ENG'
JOIN users u ON u.tenant_id = t.id AND u.email = 'sarah.connor@acme.com'
WHERE t.slug = 'acme-corp'
ON CONFLICT (id) DO NOTHING;

-- 9. Insert Sample Document Chunk
INSERT INTO document_chunks (id, tenant_id, document_id, chunk_index, content, character_count, token_count, security_classification, allowed_roles, allowed_departments, created_at)
SELECT 
    'c1111111-1111-1111-1111-111111111111'::uuid, t.id, doc.id, 0, 'Section 1. Architecture Overview: All microservices must be developed in Java 21 using Spring Boot 3.3+. PostgreSQL with pgvector is mandatory for vector similarity search.', 165, 32, 'INTERNAL', ARRAY['ROLE_EMPLOYEE', 'ROLE_ADMIN'], ARRAY['ENG'], CURRENT_TIMESTAMP
FROM tenants t
JOIN documents doc ON doc.tenant_id = t.id AND doc.id = 'f1111111-1111-1111-1111-111111111111'::uuid
WHERE t.slug = 'acme-corp'
ON CONFLICT (id) DO NOTHING;

-- 10. Insert Sample Chat Session & Message
INSERT INTO chat_sessions (id, tenant_id, user_id, title, status, created_at, updated_at)
SELECT 
    '91111111-1111-1111-1111-111111111111'::uuid, t.id, u.id, 'Software Architecture Clarification', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t
JOIN users u ON u.tenant_id = t.id AND u.email = 'sarah.connor@acme.com'
WHERE t.slug = 'acme-corp'
ON CONFLICT (id) DO NOTHING;

INSERT INTO chat_messages (id, session_id, role, content, created_at)
VALUES 
    ('81111111-1111-1111-1111-111111111111', '91111111-1111-1111-1111-111111111111', 'USER', 'What framework and Java version are required for microservices?', CURRENT_TIMESTAMP),
    ('82222222-2222-2222-2222-222222222222', '91111111-1111-1111-1111-111111111111', 'ASSISTANT', 'According to Section 1 of the Q3 Software Architecture Guidelines [1], all microservices must be developed in Java 21 using Spring Boot 3.3+.', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 11. Insert Sample Audit Log
INSERT INTO audit_logs (id, tenant_id, actor_user_id, action, target_resource_type, target_resource_id, client_ip, status, created_at)
SELECT 
    '71111111-1111-1111-1111-111111111111'::uuid, t.id, u.id, 'DOCUMENT_UPLOAD', 'DOCUMENT', 'f1111111-1111-1111-1111-111111111111'::uuid, '127.0.0.1', 'SUCCESS', CURRENT_TIMESTAMP
FROM tenants t
JOIN users u ON u.tenant_id = t.id AND u.email = 'sarah.connor@acme.com'
WHERE t.slug = 'acme-corp'
ON CONFLICT (id) DO NOTHING;
