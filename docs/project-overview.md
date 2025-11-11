# Project Overview

## Purpose

The project is a proof of concept that exports customer and order data from PostgreSQL to an S3-compatible object store on a schedule. Exports are grouped by customer country and saved as CSV files without headers. A Vue-powered dashboard provides visibility into sync runs, S3 contents, and scheduler settings, and offers manual controls for operators.

## Backend (Spring Boot 3, Java 21)

- **Core flow** – `SyncService` coordinates incremental exports. It reads new/updated customers and orders, groups them by country, serializes CSV rows, and stores them through `S3StorageService`. Sync state is persisted via `SyncStateRepository`, enabling incremental runs.
- **Scheduling** – `SyncScheduler` schedules `SyncService` executions at a configurable interval (default: every 3 hours). Operators can update or disable the schedule through REST endpoints.
- **S3 integration** – `S3Service` abstracts the AWS SDK client. It ensures bucket existence, lists objects for the dashboard, serves downloads, and detects empty buckets to trigger full exports.
- **API surface**
  - `POST /api/sync/run` – trigger a sync immediately.
  - `GET /api/sync/state` – last successful sync timestamps per logical table (`kunde`, `auftraege`).
  - `GET /api/sync/runs` – recent runs with per-country batches and statuses.
  - `GET /api/sync/schedule`, `PUT /api/sync/schedule` – inspect or change the automatic schedule interval.
  - `GET /api/s3/files`, `GET /api/s3/files?key=…` – list and download S3 objects.
  - `GET /api/customers`, `GET /api/orders` – raw data views for debugging and for the dashboard’s “Data View”.
  - `POST /api/orders/lastchange/touch` – demo endpoint touching order timestamps to simulate fresh changes.
- **Persistence & data** – Spring Data JPA with Flyway migrations. Migrations create base tables and seed sample customers/orders and sync metadata.
- **Configuration** – `application.yml` defaults to Docker Compose services (`postgres:5432`, LocalStack `localhost:4566`). Properties under `aws.s3.*` and `sync.*` are validated via configuration tests.

## Frontend (Vue 3, TypeScript, Vite)

- **Routing & layout** – `router/index.ts` defines four main views rendered within `App.vue`:
  - **Synchronize** – shows last sync timestamps and recent runs (`SyncService` endpoints) and provides a “Run Sync Now” action.
  - **AWS Connect** – browses S3 bucket contents, splits files into customer/order/other groups, and enables CSV downloads via `s3Service`.
  - **Data View** – aggregates customers and their orders by country using `customerService` and `orderService`.
  - **Settings** – edits the scheduler interval, triggers the “touch last change” routine, and displays success/error feedback (state is shared through `statusStore`).
- **API client** – `services/api.ts` centralizes Axios configuration, endpoint paths, and TypeScript interfaces for responses.
- **Styling** – global styles in `style.css` with per-component scoped CSS for layout.
- **Dev experience** – Vite dev server on port 5173 proxies `/api` calls to the backend (`vite.config.ts`). Builds emit static assets in `frontend/dist`.

## Local Infrastructure

- `docker-compose.yml` spins up PostgreSQL 14 and LocalStack (S3 only). Volumes retain database state between restarts.
- Flyway migrations run automatically on backend startup to prepare schema and seed data.
- Default S3 bucket: `s3-sync-poc`, region `eu-central-1`, path-style addressing (compatible with LocalStack).
- The repo includes SQL scripts (`insert_kunde.sql`, `insert_auftraege.sql`, `update_kunde.sql`) for manual data tweaks if needed.

