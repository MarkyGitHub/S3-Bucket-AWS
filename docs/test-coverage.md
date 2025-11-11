# Test Coverage Summary

## How to Run

Execute all automated backend tests (unit + integration) from PowerShell:

```powershell
cd backend
.\gradlew.bat test
```

- Requires Docker Desktop (Testcontainers starts PostgreSQL and LocalStack automatically).
- A detailed HTML report is generated at `backend/build/reports/tests/test/index.html`.

## Backend Test Suite

| Area | Test Class | Scope |
| --- | --- | --- |
| Configuration validation | `com.contargo.s3sync.config.ConfigurationValidationTest` | Asserts that required `aws.s3.*` properties and positive sync intervals are enforced. |
| Application bootstrap | `com.contargo.s3sync.S3SyncApplicationTests` | Verifies that the Spring context loads with default configuration. |
| Sync orchestration | `com.contargo.s3sync.sync.SyncServiceTest` | Covers incremental vs full exports, country grouping, CSV layout, sync state persistence, and empty-change handling. |
| S3 storage adapter | `com.contargo.s3sync.sync.S3StorageServiceTest` | Checks bucket/key naming, retry logic, and logging for upload failures. |
| Scheduling | `com.contargo.s3sync.sync.SyncSchedulerTest` | Ensures scheduled runs are created at the configured interval and recover after failures. |
| Monitoring API | `com.contargo.s3sync.sync.SyncMonitoringServiceTest` | Validates DTO mapping for run/state responses. |
| End-to-end export | `com.contargo.s3sync.sync.SyncServiceIntegrationTest` | Uses Testcontainers (PostgreSQL + LocalStack) to assert that a real sync run uploads files to S3 and reports success. |

## Frontend

- No automated front-end tests are present. Manual verification is currently required for Vue components and routing.

## Coverage Gaps & Suggestions

- **Frontend** – consider adding component tests (e.g., Vitest + Vue Test Utils) for critical views and store logic.
- **API contracts** – consumer-driven tests or schema validation could guard against regressions in the REST responses consumed by the frontend.

