<div align="center">

# HireTrack API

<p>
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white"/>
  <img src="https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white"/>
  <img src="https://img.shields.io/badge/Docker-ready-2496ED?style=for-the-badge&logo=docker&logoColor=white"/>
</p>

<p>
  <img src="https://img.shields.io/badge/JWT-Auth-black?style=for-the-badge&logo=jsonwebtokens"/>
  <img src="https://img.shields.io/badge/Flyway-Migrations-CC0200?style=for-the-badge&logo=flyway"/>
  <img src="https://img.shields.io/badge/Swagger-OpenAPI%203-85EA2D?style=for-the-badge&logo=swagger&logoColor=black"/>
  <img src="https://img.shields.io/github/actions/workflow/status/Sefirr-dot/hiretrack-api/ci.yml?style=for-the-badge&label=CI"/>
</p>

**A production-grade REST API for tracking job applications, interviews, and offers.**

*Built because I was tired of losing track of where I had applied.*

</div>

---

## What is this?

HireTrack is a backend API that lets job seekers manage their entire job search in one place: track every application, schedule interviews, get email reminders, and visualize their pipeline with real statistics.

Every feature comes from a real need — I built this while actively job hunting.

---

## Features

| Feature | Details |
|---|---|
| **JWT Auth** | Access + refresh token rotation, stored revocation, BCrypt passwords |
| **Application Tracking** | Full lifecycle: Applied → Phone Screen → Interview → Offer → Hired/Rejected |
| **Interview Scheduling** | 6 types (HR, Technical, System Design, Cultural, Final, Other) |
| **Email Reminders** | Quartz cron job sends Thymeleaf-templated HTML emails 24h before each interview |
| **Statistics Dashboard** | Response rate, interview conversion, weekly timeline, funnel by stage |
| **Document Storage** | Upload CVs/cover letters per application (PDF, DOCX, PNG, JPG — max 10MB) |
| **Redis Caching** | All read-heavy endpoints cached; evicted on any write |
| **Pagination & Filters** | Filter by status, company, remote, date range |
| **OpenAPI / Swagger UI** | Full API docs at `/swagger-ui.html` |
| **CI/CD** | GitHub Actions pipeline with PostgreSQL service container |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Spring Boot App                          │
│                                                                   │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐   │
│  │  Controllers │───▶│   Services   │───▶│  Repositories    │   │
│  │  (REST API)  │    │ (Biz Logic)  │    │  (Spring Data)   │   │
│  └──────────────┘    └──────┬───────┘    └────────┬─────────┘   │
│                             │                      │              │
│  ┌──────────────┐    ┌──────▼───────┐    ┌────────▼─────────┐   │
│  │  JWT Filter  │    │ Redis Cache  │    │   PostgreSQL 16   │   │
│  │  (Security)  │    │  (10 min TTL)│    │ (Flyway Migrations│   │
│  └──────────────┘    └──────────────┘    └──────────────────┘   │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Quartz Scheduler → EmailService → Thymeleaf Templates   │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## API Reference

### Authentication
```
POST   /api/v1/auth/register     Register a new user
POST   /api/v1/auth/login        Login, receive access + refresh token
POST   /api/v1/auth/refresh      Exchange refresh token for new access token
POST   /api/v1/auth/logout       Revoke refresh token
```

### Applications
```
GET    /api/v1/applications                          List (paginated, filterable)
POST   /api/v1/applications                          Create
GET    /api/v1/applications/{id}                     Get by ID
PUT    /api/v1/applications/{id}                     Update
PATCH  /api/v1/applications/{id}/status              Update status only
DELETE /api/v1/applications/{id}                     Delete
```

**Query params:** `?page=0&size=10&status=APPLIED&company=Google&remote=true&from=2025-01-01&to=2025-12-31`

### Interviews
```
GET    /api/v1/applications/{appId}/interviews       List
POST   /api/v1/applications/{appId}/interviews       Schedule
PUT    /api/v1/interviews/{id}                       Update
DELETE /api/v1/interviews/{id}                       Delete
```

### Statistics
```
GET    /api/v1/stats/summary      Total, by status, response rate, conversion rate
GET    /api/v1/stats/timeline     Weekly applications for last 12 weeks
GET    /api/v1/stats/funnel       Stage-by-stage conversion funnel
```

### Documents
```
POST   /api/v1/applications/{appId}/documents        Upload (multipart/form-data)
GET    /api/v1/applications/{appId}/documents        List
GET    /api/v1/documents/{id}/download               Download
DELETE /api/v1/documents/{id}                        Delete
```

---

## Application Status Flow

```
APPLIED ──▶ PHONE_SCREEN ──▶ TECHNICAL_TEST ──▶ INTERVIEW ──▶ FINAL_INTERVIEW ──▶ OFFER ──▶ HIRED
   │              │                 │               │                │               │
   └──────────────┴─────────────────┴───────────────┴────────────────┴───────────────┴──▶ REJECTED
                                                                                            GHOSTED
                                                                                            WITHDRAWN
```

---

## Getting Started

### Prerequisites
- Docker & Docker Compose
- A `.env` file (see `.env.example`)

### Run with Docker

```bash
git clone https://github.com/Sefirr-dot/hiretrack-api.git
cd hiretrack-api
cp .env.example .env
# Edit .env with your values
docker compose up -d
```

The API will be available at `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui.html`

### Run Locally

```bash
# Start only PostgreSQL and Redis
docker compose up -d postgres redis

# Set environment variables
export DB_URL=jdbc:postgresql://localhost:5432/hiretrack
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export JWT_SECRET=your_base64_secret
# ... (see .env.example for all vars)

./mvnw spring-boot:run
```

### Run Tests

```bash
./mvnw test            # Unit tests only
./mvnw verify          # All tests (requires Docker for Testcontainers)
```

---

## Project Structure

```
src/main/java/com/sefirr/hiretrack/
├── config/          SecurityConfig, RedisConfig, SwaggerConfig, QuartzConfig
├── controller/      AuthController, ApplicationController, InterviewController...
├── dto/             Request/Response DTOs with Bean Validation
├── entity/          User, Application, Interview, Document, RefreshToken
├── enums/           ApplicationStatus (10 states), InterviewType (6 types), Role
├── exception/       GlobalExceptionHandler + custom exceptions
├── repository/      Spring Data JPA repositories with custom JPQL queries
├── scheduler/       Quartz job for interview reminders (runs hourly)
├── security/        JwtService, JwtAuthFilter, UserDetailsServiceImpl
├── service/         Business logic layer (Auth, Application, Interview, Stats, Email)
└── storage/         StorageService interface + LocalStorageService implementation
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Security | Spring Security 6 + JWT (jjwt 0.12) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Cache | Redis 7 + Spring Cache |
| Scheduler | Quartz |
| Email | Spring Mail + Thymeleaf |
| Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Tests | JUnit 5 + Mockito + Testcontainers |
| Build | Maven |
| Infrastructure | Docker + Docker Compose |
| CI | GitHub Actions |

---

## Security Design

- Stateless JWT authentication (HS256)
- Short-lived access tokens (15 min) + long-lived refresh tokens (7 days)
- Refresh tokens stored in DB — real logout and revocation possible
- BCrypt password hashing
- Resource ownership enforced at the service layer on every request
- File path traversal prevention in storage service

---

## License

MIT — feel free to use this as a reference or starting point for your own projects.
