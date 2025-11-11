# Windows Setup and Run Guide

This guide targets Windows 10/11 users running the project locally. All commands below assume Windows PowerShell in the repository root (`Kennenlernen Contargo`).

## 1. Prerequisites

- **Docker Desktop** with WSL 2 integration enabled (required for PostgreSQL and LocalStack containers).
- **Node.js 20 LTS** (Node ≥ 18.0.0 works) and npm. Verify with:
  ```powershell
  node -v
  npm -v
  ```
- **Git** (for cloning) and **PowerShell 7+** recommended.

> The backend uses the Gradle wrapper with Java toolchains. You do **not** need to install a JDK manually; the wrapper downloads a JDK 21 runtime on first use.

## 2. Start Local Infrastructure (PostgreSQL + LocalStack)

From the repository root:

```powershell
docker compose up -d
```

Services exposed locally:

- PostgreSQL: `localhost:5432` (`postgres/password`, database `s3sync`)
- LocalStack S3 endpoint: `http://localhost:4566`

Useful follow-up commands:

```powershell
docker compose ps               # verify containers are healthy
docker compose logs -f postgres # tail database logs
docker compose down             # stop and remove containers
```

## 3. Run the Backend (Spring Boot)

```powershell
cd backend
.\gradlew.bat bootRun
```

The first run downloads Gradle, dependencies, and a JDK toolchain. The API listens on `http://localhost:8080` with the LocalStack and PostgreSQL settings defined in `application.yml`.

### Environment Overrides

Set these before starting the app if you need custom endpoints:

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `AWS_S3_ENDPOINT`, `AWS_S3_BUCKET_NAME`, `AWS_S3_REGION`, `AWS_S3_FORCE_PATH_STYLE`
- `SYNC_SCHEDULE_CRON` *or* `SYNC_SCHEDULE_INTERVAL`, `SYNC_SCHEDULER_ENABLED`

Example (PowerShell):

```powershell
$env:AWS_S3_BUCKET_NAME = "my-sync-bucket"
.\gradlew.bat bootRun
```

### Backend Utilities

```powershell
.\gradlew.bat test   # run unit + integration tests (requires Docker for Testcontainers)
.\gradlew.bat build  # produce a runnable jar in build/libs
```

## 4. Run the Frontend (Vue 3 + Vite)

In a new PowerShell window:

```powershell
cd frontend
npm install
npm run dev
```

- Vite serves the SPA on `http://localhost:5173`.
- The dev server proxies `/api` requests to `http://localhost:8080` (configurable in `vite.config.ts`).
- To target a different backend when deploying a static build, set `VITE_API_BASE_URL` in an `.env` file before `npm run build`.

### Additional Frontend Commands

```powershell
npm run build    # create production bundle in dist/
npm run preview  # preview the built app locally
```

## 5. Shut Down

Stop the backend and frontend processes (Ctrl+C in each terminal) and tear down infrastructure when finished:

```powershell
docker compose down
```

This removes the LocalStack and PostgreSQL containers but keeps persistent volumes (customer/order data seeded by Flyway).

