# AI Enterprise Assistant

> A full-stack, multi-tenant enterprise AI platform combining secure knowledge management, Retrieval-Augmented Generation (RAG), meeting intelligence, and AI-powered productivity utilities — containerized with Docker.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Setup & Installation](#setup--installation)
- [Environment Variables](#environment-variables)
- [RAG Pipeline](#rag-pipeline)
- [Meeting Intelligence](#meeting-intelligence)
- [AI Utilities](#ai-utilities)
- [Security](#security)
- [API Reference](#api-reference)
- [Docker Services](#docker-services)
- [Flyway Database Migrations](#flyway-database-migrations)
- [Future Improvements](#future-improvements)
- [Disclaimer](#disclaimer)

---

## Overview

The **AI Enterprise Assistant** is an enterprise-grade AI platform designed to augment organisational knowledge workflows. It allows employees to upload documents, query their enterprise knowledge base via a conversational AI interface grounded strictly in uploaded documents, analyse meeting transcripts, and leverage AI-powered productivity tools — all within a secure, multi-tenant, role-based architecture.

The system enforces strict enterprise grounding: the AI will only answer questions when relevant information is found in uploaded documents. When no relevant context exists, it safely declines to answer rather than hallucinating.

---

## Key Features

| Feature | Description |
|---|---|
| **Secure Authentication** | JWT-based registration and login with silent token refresh and multi-tenant isolation |
| **Knowledge Base Management** | Upload, index, search, download, and delete enterprise documents (PDF, DOCX, TXT, MD) |
| **Document Ingestion Pipeline** | Automatic text extraction, semantic chunking, embedding, and pgvector storage on upload |
| **RAG AI Chat Assistant** | Retrieval-Augmented Generation with document-grounded answers and numbered source citations like `[S1]` |
| **Follow-up Question Handling** | LLM-powered query normalization rewrites conversational follow-ups into standalone semantic queries before retrieval |
| **Enterprise Grounding Safety** | Zero-context safety: if no relevant documents are found, the system returns a safe refusal rather than hallucinating |
| **Intent Classification** | Intelligently routes queries between document-listing requests and knowledge retrieval using an LLM-based intent classifier |
| **Meeting Intelligence** | Upload meeting transcripts and receive AI-generated summaries, decisions, action items, risks, and participant extractions |
| **AI Utilities** | Built-in Email Generator, SQL Query Builder, and Code Generator powered by Groq LLM |
| **Dashboard** | Tenant-scoped analytics dashboard with document counts, chat statistics, and usage metrics |
| **Tenant Isolation** | All data operations are strictly scoped by Tenant ID at the database query level |
| **RBAC** | Role-based access control (`ROLE_ADMIN`, `ROLE_EMPLOYEE`) with fine-grained permission authorities |
| **Redis Caching** | Multi-domain TTL caching for document metadata, permissions, sessions, and dashboard data |
| **Rate Limiting** | Sliding-window rate limiting (30 rpm default) on expensive AI endpoints via Redis |
| **Dockerized** | Full production deployment via Docker Compose with health checks and dependency ordering |
| **Flyway Migrations** | Versioned SQL schema management with automatic migration on startup |

---

## System Architecture

```mermaid
flowchart TD
    User([User Browser]) --> FE[React Frontend\nTypeScript + Vite]
    FE --> Nginx[Nginx Reverse Proxy]
    Nginx --> API[Spring Boot REST API\nJava 21]

    API --> Auth[JWT Auth Filter\nTenant Context]
    Auth --> RBAC[RBAC Permission Check]
    RBAC --> Services[Application Services]

    Services --> PG[(PostgreSQL 16\n+ pgvector)]
    Services --> Redis[(Redis 7\nCache + Rate Limit)]
    Services --> Groq[Groq LLM API\nChat + Intent + Utilities]
    Services --> Gemini[Google Gemini\nEmbedding API]

    subgraph RAG Pipeline
        direction LR
        Upload[Document Upload] --> Extract[Text Extraction\nApache Tika]
        Extract --> Chunk[Semantic Chunking]
        Chunk --> Embed[Gemini Embedding\n768-dim]
        Embed --> Store[pgvector Storage\nHNSW Index]
        Query[User Query] --> Normalize[Query Normalization\nLLM Rewrite]
        Normalize --> VecSearch[Cosine Similarity Search\nTop-K=8, threshold=0.55]
        Store --> VecSearch
        VecSearch --> Context[Retrieved Context]
        Context --> LLM[Groq LLM\nGrounded Answer]
        LLM --> Citation[Cited Response\n[S1] [S2] ...]
    end
```

### Document Upload → RAG Flow

```
Document Upload (PDF / DOCX / TXT / MD)
    ↓
Text Extraction (Apache Tika)
    ↓
Semantic Chunking (configurable size + overlap)
    ↓
Google Gemini Embedding (768-dimensional vectors)
    ↓
pgvector Storage (HNSW Cosine Similarity Index)
    ↓
[On Query] → Query Normalization (LLM rewrites follow-ups)
    ↓
Cosine Similarity Search (Top-K=8, threshold=0.55)
    ↓
Retrieved Context → Groq LLM (enterprise-grounded prompt)
    ↓
Cited Answer with [S1], [S2] source markers
```

---

## Technology Stack

### Backend
| Component | Technology |
|---|---|
| Language | Java 21 LTS |
| Framework | Spring Boot 3.3.2 |
| Security | Spring Security 6 + JWT (JJWT) |
| Database ORM | Spring Data JPA / Hibernate 6 |
| Database | PostgreSQL 16 + pgvector 0.8.6 |
| Cache | Redis 7.x (Spring Data Redis / Lettuce) |
| LLM Provider | Groq API (`openai/gpt-oss-20b`) |
| Embedding Provider | Google Gemini Embedding 2 (`models/gemini-embedding-2`, 768 dims) |
| Document Parsing | Apache Tika 2.9.2 |
| Migrations | Flyway |
| API Documentation | SpringDoc OpenAPI 3 / Swagger UI |
| HTTP Client | Spring WebFlux WebClient |
| Build | Apache Maven 3.9 |

### Frontend
| Component | Technology |
|---|---|
| Framework | React 18 |
| Language | TypeScript |
| Build Tool | Vite 5 |
| Styling | Tailwind CSS 3 |
| HTTP Client | Axios |
| State Management | Zustand |
| Routing | React Router DOM 6 |
| Icons | Lucide React |
| Web Server | Nginx 1.25 |

### Infrastructure
| Component | Technology |
|---|---|
| Containerization | Docker + Docker Compose |
| Database | pgvector/pgvector:pg16 |
| Reverse Proxy | Nginx (fronting React + proxying API) |

---

## Project Structure

```
ai-enterprise-assistant/
├── .env.example                    # Environment variable template (safe placeholders)
├── .gitignore                      # Git ignore rules
├── docker-compose.yml              # Multi-service Docker orchestration
├── README.md                       # This file
│
├── frontend/                       # React + TypeScript frontend
│   ├── Dockerfile                  # Multi-stage Node → Nginx build
│   ├── nginx.conf                  # Nginx reverse proxy config
│   ├── package.json
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   └── src/
│       ├── components/             # Shared UI components
│       ├── pages/                  # Route-level pages (Chat, Knowledge, Meetings, etc.)
│       ├── services/               # Axios API service layer
│       ├── store/                  # Zustand state stores
│       ├── hooks/                  # Custom React hooks
│       ├── types/                  # TypeScript type definitions
│       └── routes/                 # React Router route configuration
│
└── backend/                        # Spring Boot backend
    ├── Dockerfile                  # Multi-stage Maven → JRE build
    ├── pom.xml                     # Maven dependencies
    └── src/main/
        ├── java/com/enterprise/assistant/
        │   ├── ai/                 # LLM and embedding service clients
        │   ├── config/             # Spring configuration beans
        │   ├── controller/         # REST controllers
        │   ├── domain/             # JPA entities
        │   ├── dto/                # Request/Response DTOs
        │   ├── exception/          # Global exception handling
        │   ├── repository/         # Spring Data JPA repositories
        │   ├── security/           # JWT filters, UserPrincipal, RBAC
        │   ├── service/            # Business logic services
        │   │   ├── chat/           # Chat, intent classification, RAG
        │   │   ├── document/       # Document upload, extraction, chunking
        │   │   ├── meeting/        # Meeting transcript processing
        │   │   └── utility/        # Email, SQL, Code generators
        │   └── util/               # Shared utilities
        └── resources/
            ├── application.yml             # Base Spring configuration
            ├── application-dev.yml         # Development profile
            ├── application-prod.yml        # Production profile
            └── db/migration/               # Flyway SQL migrations
                ├── V1__init_schema.sql     # Full schema + pgvector
                ├── V2__seed_data.sql       # Default tenant, roles, sample data
                ├── V3__add_user_sso_fields.sql
                └── V4__add_meetings_table.sql
```

---

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop) (Docker Engine 24+ and Docker Compose v2)
- A **Groq API key** — obtain free at [console.groq.com/keys](https://console.groq.com/keys)
- A **Google Gemini API key** — obtain free at [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)

No local Java, Node.js, or PostgreSQL installation is required. Everything runs inside Docker.

---

## Setup & Installation

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/ai-enterprise-assistant.git
cd ai-enterprise-assistant
```

### 2. Create your environment file

```bash
cp .env.example .env
```

Open `.env` and fill in your actual API keys and credentials:

```env
POSTGRES_PASSWORD=choose_a_strong_database_password
REDIS_PASSWORD=choose_a_strong_redis_password
JWT_SECRET=generate_with_openssl_rand_hex_32
GEMINI_API_KEY=your_actual_gemini_api_key
GROQ_API_KEY=your_actual_groq_api_key
```

> **Security Note:** Never commit your `.env` file. It is already listed in `.gitignore`.

### 3. Build and start all services

```bash
docker compose up -d --build
```

This builds the frontend and backend Docker images and starts PostgreSQL, Redis, the Spring Boot backend, and the Nginx-served React frontend.

### 4. Verify all services are healthy

```bash
docker compose ps
```

You should see all four services running:

```
NAME                  STATUS           PORTS
assistant-postgres    Up (healthy)     0.0.0.0:5432->5432/tcp
assistant-redis       Up (healthy)     0.0.0.0:6379->6379/tcp
assistant-backend     Up (healthy)     0.0.0.0:8080->8080/tcp
assistant-frontend    Up               0.0.0.0:80->80/tcp
```

### 5. Open the application

Navigate to **http://localhost** in your browser.

### 6. Register an account

Use the **Register** page to create your first account. The default tenant slug is **`acme-corp`** (seeded by Flyway migration V2).

> **Default test credentials** seeded by V2 migration:
> - Admin: `admin@acme.com` / `Password123!`
> - Employee: `sarah.connor@acme.com` / `Password123!`

---

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `POSTGRES_DB` | Yes | PostgreSQL database name |
| `POSTGRES_USER` | Yes | PostgreSQL username |
| `POSTGRES_PASSWORD` | Yes | PostgreSQL password |
| `REDIS_PASSWORD` | Yes | Redis password |
| `JWT_SECRET` | Yes | HS256 JWT signing secret (minimum 64 hex chars / 256 bits) |
| `JWT_EXPIRATION_MS` | No | Access token lifetime in ms (default: `900000` = 15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | No | Refresh token lifetime in ms (default: `604800000` = 7 days) |
| `GEMINI_API_KEY` | Yes | Google Gemini API key for document embeddings |
| `GROQ_API_KEY` | Yes | Groq API key for LLM chat, intent classification, and utilities |
| `GROQ_MODEL` | No | Groq model name (default: `openai/gpt-oss-20b`) |
| `RAG_TOP_K` | No | Maximum chunks retrieved per query (default: `8`) |
| `RAG_SIMILARITY_THRESHOLD` | No | Minimum cosine similarity score for retrieval (default: `0.55`) |
| `RATE_LIMIT_ENABLED` | No | Enable API rate limiting (default: `true`) |
| `RATE_LIMIT_RPM` | No | Requests per minute per user (default: `30`) |

---

## RAG Pipeline

The Retrieval-Augmented Generation pipeline ensures that all AI answers are grounded strictly in uploaded enterprise documents.

### Document Ingestion (Upload → Index)

1. **Upload**: User uploads a document (PDF, DOCX, TXT, or Markdown).
2. **Text Extraction**: Apache Tika extracts plain text from all supported formats.
3. **Chunking**: The extracted text is split into semantic overlapping chunks.
4. **Embedding**: Each chunk is embedded using the Google Gemini Embedding 2 model (768 dimensions).
5. **Storage**: Embeddings are stored in PostgreSQL via the `pgvector` extension with an HNSW cosine similarity index for fast retrieval.

### Query → Retrieval → Generation

1. **Query Normalization**: If the user asks a follow-up question (e.g., "Can you explain that?"), an LLM first rewrites it into a standalone, semantically meaningful query (e.g., "Explain the workstation locking policy in detail").
2. **Intent Classification**: An LLM classifies the query intent — `ENTERPRISE_KNOWLEDGE` (RAG), `DOCUMENT_LIST`, `CASUAL_CHAT`, or `MEETING_QUERY` — to route correctly.
3. **Cosine Similarity Search**: The normalized query is embedded and searched against the pgvector index. Top-K=8 chunks above the 0.55 similarity threshold are retrieved.
4. **Zero-Context Safety**: If zero relevant chunks are found, the system returns a safe refusal message and does NOT allow the LLM to answer from general knowledge.
5. **Grounded Generation**: Retrieved chunks are injected into a strictly scoped system prompt. The LLM answers only from the provided context.
6. **Source Citation**: Every cited chunk is referenced with a numbered marker (e.g., `[S1]`, `[S2]`) in the response.

---

## Meeting Intelligence

Users can upload plain-text meeting transcripts for AI-powered analysis.

1. **Upload**: Upload a `.txt` or `.md` transcript file with a meeting title and date.
2. **Processing**: The backend sends the transcript to the Groq LLM for analysis.
3. **Extraction**: The AI extracts:
   - **Summary**: Concise meeting overview
   - **Decisions**: Key decisions made
   - **Action Items**: Tasks assigned with owners and deadlines
   - **Risks**: Identified risks and blockers
   - **Participants**: People mentioned in the transcript

Results are stored in PostgreSQL and displayed in the Meetings dashboard.

---

## AI Utilities

Three standalone AI-powered productivity tools are available under **AI Utilities**:

| Tool | Description |
|---|---|
| **Email Generator** | Generate professional business emails from a brief description and tone selection |
| **SQL Query Builder** | Describe a database query in plain English and receive a formatted SQL query with explanation |
| **Code Generator** | Describe a function or feature and receive generated code in a chosen programming language |

All utilities use the Groq LLM. They are independent of the RAG knowledge base.

---

## Security

### Authentication
- JWT-based authentication with access tokens (15-minute default lifetime) and refresh tokens (7-day default lifetime).
- Silent automatic token refresh on 401 responses in the frontend.

### Multi-Tenant Isolation
- Every database query is strictly filtered by `tenant_id`.
- A user from Tenant A can never access data belonging to Tenant B.

### Role-Based Access Control (RBAC)
- Fine-grained permission authorities (`DOCUMENT_UPLOAD`, `DOCUMENT_READ`, `DOCUMENT_DELETE`, `AI_QUERY`, etc.).
- `ROLE_ADMIN` users have full permissions. `ROLE_EMPLOYEE` users have read, upload, download, and AI query permissions.

### Enterprise Document Grounding
- The AI is prohibited from answering from general knowledge.
- System prompts explicitly instruct the LLM to use only the retrieved enterprise context.

### Secret Management
- All secrets (API keys, database passwords, JWT secrets) are supplied via environment variables.
- No secrets are hardcoded in source code or committed to Git.
- `.env` is excluded from version control by `.gitignore`.

---

## API Reference

The full interactive API documentation is available at:

```
http://localhost:8080/swagger-ui.html
```

Key endpoint groups:

| Group | Base Path |
|---|---|
| Authentication | `POST /api/v1/auth/register`, `POST /api/v1/auth/login` |
| Knowledge Documents | `GET/POST/DELETE /api/v1/documents` |
| AI Chat | `POST /api/v1/chat/sessions`, `POST /api/v1/chat/sessions/{id}/messages` |
| Meetings | `GET/POST /api/v1/meetings` |
| AI Utilities | `POST /api/v1/utilities/email`, `/sql`, `/code` |
| Dashboard | `GET /api/v1/dashboard/tenant` |
| Health | `GET /actuator/health` |

---

## Docker Services

```yaml
# docker-compose.yml services
postgres    # pgvector/pgvector:pg16  — vector-enabled PostgreSQL
redis       # redis:7.2-alpine        — caching and rate limiting
backend     # Spring Boot JAR built from ./backend/Dockerfile
frontend    # Nginx + React bundle built from ./frontend/Dockerfile
```

### Common Commands

```bash
# Start all services (build if needed)
docker compose up -d --build

# Check service status
docker compose ps

# View backend logs
docker compose logs -f backend

# Stop all services
docker compose down

# Stop and remove all volumes (wipes database)
docker compose down -v
```

---

## Flyway Database Migrations

Database schema is managed automatically by Flyway on backend startup.

| Migration | Description |
|---|---|
| `V1__init_schema.sql` | Full schema: tenants, users, roles, documents, pgvector chunks, chat, audit logs |
| `V2__seed_data.sql` | Default tenant (`acme-corp`), roles, permissions, and sample users |
| `V3__add_user_sso_fields.sql` | Optional SSO/external identity fields on the users table |
| `V4__add_meetings_table.sql` | Meetings table for transcript processing and AI analysis results |

Migrations run in version order on every startup. Flyway uses `baseline-on-migrate: true` for compatibility with existing databases.

---

## Future Improvements

- **Expanded document formats**: PowerPoint, Excel, and scanned image OCR support
- **Conversational memory**: Longer session history and cross-session memory
- **Admin audit dashboard**: Visual audit log viewer for compliance tracking
- **Expanded RBAC**: Department-level access control and custom role creation via UI
- **Evaluation metrics**: Automated RAG faithfulness and relevance scoring
- **SSO integration**: OAuth2/OIDC integration (Google, Microsoft Entra ID)
- **Production deployment**: Kubernetes manifests and cloud deployment guides (AWS/GCP/Azure)
- **Document versioning**: Full version history and comparison for updated documents

---

## Disclaimer

This application uses Large Language Models (LLMs) to generate responses. While the RAG pipeline is designed to ground answers strictly in uploaded enterprise documents, AI-generated content should always be reviewed by a qualified human before use in important business decisions. The system may still occasionally provide incomplete or inaccurate information. It is not certified for regulated industries such as healthcare, legal, or financial advice without additional validation.

---

## Author

Developed as a Final Year Project demonstrating enterprise AI system design, RAG architecture, multi-tenant security, and full-stack containerized deployment.
