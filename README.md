# S3 Sync Proof of Concept

This repository contains a full-stack proof of concept that synchronises customer and order data from a PostgreSQL database into S3-compatible object storage every three hours. The export is incremental, grouped by country, and delivered as CSV files without headers. A small Vue dashboard exposes the job history, current sync state, and allows manual trigger of a new run.

## Stack

- **Backend**: Java 21, Spring Boot 3, Spring Data JPA, Flyway, AWS SDK v2, Testcontainers
- **Database**: PostgreSQL 16 (DDL/data managed by Flyway)
- **Object storage**: AWS S3 (LocalStack in local/dev environments)
- **Frontend**: Vue 3, TypeScript, Vite, Axios, Day.js

## Getting Started

### 1. Infrastructure

Use Docker Compose to start PostgreSQL and LocalStack:

```bash
docker compose up -d
```

Services exposed locally:

- PostgreSQL: `jdbc:postgresql://localhost:5432/s3sync` (user/password `s3sync`)
- LocalStack S3 endpoint: `http://localhost:4566`

### 2. Backend

```bash
cd backend
./gradlew bootRun
```

Key endpoints:

- `POST /api/sync/run` — triggers a manual export
- `GET /api/sync/state` — latest successful timestamps per table
- `GET /api/sync/runs` — recent sync runs with per-country uploads
- `GET /api/customers`, `GET /api/orders` — raw table reads for debugging

Environment variables of interest:

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `AWS_S3_ENDPOINT` (override `aws.s3.endpoint`), `AWS_S3_BUCKET_NAME`, `AWS_S3_REGION`
- `SYNC_SCHEDULE_CRON` (override `sync.schedule-cron`), `SYNC_SCHEDULER_ENABLED`

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server runs on `http://localhost:5173` and proxies `/api` calls to the Spring Boot backend.

## Running Tests

```bash
cd backend
./gradlew test
```

The test suite includes:

- Unit tests for the sync service and monitoring service
- An integration test powered by Testcontainers (PostgreSQL + LocalStack) that exercises the full export flow end to end

Ensure Docker is available so Testcontainers can start required services.

## Project Layout

```
backend/   Spring Boot project (API, scheduler, persistence, Flyway migrations)
frontend/  Vue dashboard (monitoring UI + manual trigger)
docker-compose.yml  Local infra for PostgreSQL and LocalStack
```

## Next Steps

- Add auth on the monitoring endpoints or integrate with company SSO
- Push S3 object upload metrics to Prometheus/CloudWatch
- Improve retry handling & dead-letter tracking for failed uploads


