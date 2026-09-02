# AI Enterprise Assistant — Cognitive Search & RAG Platform

An enterprise-grade, multi-tenant Cognitive Search and Retrieval-Augmented Generation (RAG) platform built with Java 21, Spring Boot 3.3.2, PostgreSQL 16 + pgvector 0.8.6, Redis, Google Gemini Embedding 2, and Groq LLM (`openai/gpt-oss-20b`).

---

## 🏗 Architecture Overview

```
                      [ Client Requests ]
                               │
                      ( CorrelationIdFilter )
                               │
                   ( JwtAuthenticationFilter )
                               │
                      ( RateLimitFilter )
                               │
                 [ Controller Layer / OpenAPI ]
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
   [ Semantic Search & RAG ]            [ User & IAM Services ]
            │                                     │
   ( Gemini Embedding 2 )                         │
   ( 768-dim Vectors )                            │
            │                                     │
            ▼                                     ▼
[ PostgreSQL 16 + pgvector 0.8.6 ]    [ Redis Optimization Layer ]
( HNSW Cosine Distance <=> )          ( 2m - 15m Domain TTLs )
( 4-Tier Security SQL Filter )        ( Eventual Fallback )
            │
            ▼
     ( Groq LLM )
 ( Citations & Context )
```

### Key Highlights:
1. **Multi-Tenant IAM & RBAC**: Tenant isolation enforced at database query level and token principal level.
2. **4-Tier Security Model**: Document retrieval filters by Tenant ID, Security Classification (`PUBLIC`..`TOP_SECRET`), Allowed Roles (`String[]`), and Allowed Department Code directly inside native pgvector SQL queries.
3. **Authorized Semantic Search & RAG**: Semantic vector search with HNSW cosine distance (`<=>`), backend-authoritative citations, and prompt injection defense (`<documents>`, `<user_question>`).
4. **Conversation Memory**: Tenant-isolated chat session and turn management.
5. **Redis Caching & Rate Limiting**: Per-domain TTLs with graceful degradation fallback to PostgreSQL. Endpoint rate limiting (30 rpm) on expensive AI operations.
6. **Observability**: `X-Correlation-ID` header injection, MDC correlation logging, Actuator health endpoints (`/actuator/health`).

---

## 🛠 Tech Stack

- **Language & JDK**: Java 21 LTS
- **Framework**: Spring Boot 3.3.2 (Web, Security, Data JPA, Actuator)
- **Database**: PostgreSQL 16.14 with `pgvector 0.8.6`
- **Cache & Rate Limiting**: Redis 7.x (Spring Data Redis / Lettuce)
- **Embedding Provider**: Google Gemini Embedding 2 (`models/gemini-embedding-2`, 768 dimensions)
- **LLM Provider**: Groq LLM API (`openai/gpt-oss-20b`)
- **Document Parser**: Apache Tika 2.9.2
- **Documentation**: SpringDoc OpenAPI 3.0 / Swagger UI

---

## 📋 Environment Variables Reference

| Variable | Description | Default / Example |
|----------|-------------|-------------------|
| `SERVER_PORT` | HTTP Listening Port | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active Spring Profile (`dev`, `prod`, `h2`) | `dev` |
| `POSTGRES_HOST` | PostgreSQL Database Host | `localhost` |
| `POSTGRES_PORT` | PostgreSQL Database Port | `5432` |
| `POSTGRES_DB` | PostgreSQL Database Name | `ai_enterprise_assistant` |
| `POSTGRES_USER` | PostgreSQL Username | `postgres` |
| `POSTGRES_PASSWORD` | PostgreSQL Password | `postgres` |
| `REDIS_HOST` | Redis Cache Host | `localhost` |
| `REDIS_PORT` | Redis Cache Port | `6379` |
| `REDIS_PASSWORD` | Redis Password | `` |
| `GEMINI_API_KEY` | Google Gemini Embeddings API Key | `AIzaSy...` |
| `GROQ_API_KEY` | Groq LLM API Key | `gsk_...` |
| `JWT_SECRET` | 256-bit HS256 JWT Secret | Configured via env |
| `RATE_LIMIT_ENABLED` | Enable Redis Fixed-Window Rate Limiter | `true` |
| `RATE_LIMIT_RPM` | Rate limit requests per minute | `30` |

---

## 🚀 Setup & Installation Instructions

### 1. Database Setup (PostgreSQL + pgvector)
Connect to PostgreSQL 16 as superuser and initialize the database and vector extension:
```sql
CREATE DATABASE ai_enterprise_assistant;
\c ai_enterprise_assistant;
CREATE EXTENSION IF NOT EXISTS vector;
```

### 2. Configure Environment Variables
Set the API keys in your terminal environment:
```powershell
$env:GEMINI_API_KEY="AIzaSy..."
$env:GROQ_API_KEY="gsk_..."
```

### 3. Run Tests
Execute the full Maven test suite (125 tests):
```powershell
.\mvnw.cmd clean test
```

### 4. Run Locally
Launch the application:
```powershell
.\mvnw.cmd spring-boot:run
```

---

## 📚 API Endpoints Overview

| Category | Endpoint | Method | Security | Description |
|----------|----------|--------|----------|-------------|
| Auth | `/api/v1/auth/register` | POST | Public | Register initial tenant & admin |
| Auth | `/api/v1/auth/login` | POST | Public | Authenticate user & issue JWT |
| Documents | `/api/v1/documents` | POST | `ROLE_ADMIN` | Upload & extract document |
| Search | `/api/v1/search/semantic` | POST | Authenticated | Perform authorized 4-tier vector search |
| RAG AI | `/api/v1/ai/rag/chat` | POST | Authenticated | Execute RAG prompt & retrieve citations |
| Chat | `/api/v1/chat/sessions` | POST/GET | Authenticated | Manage chat sessions & turns |
| Dashboard | `/api/v1/dashboard/tenant` | GET | `ROLE_ADMIN` | Retrieve tenant aggregate analytics |
| Platform | `/api/v1/admin/dashboard/platform` | GET | `ROLE_SUPER_ADMIN` | Retrieve platform global metrics |
| Actuator | `/actuator/health` | GET | Public | Probe system & dependency health |

---

## 🔒 Security Policy

- All database queries filter strictly by `tenant_id`.
- Document chunks inherit clearance levels (`PUBLIC`..`TOP_SECRET`), allowed roles, and allowed departments enforced inside native PostgreSQL `vector` distance queries (`<=>`).
- Secrets (JWT secrets, API keys, passwords) are never logged, stored in Git, or exposed in error responses.
