# Demo Runbook

This guide walks through a live demo of the S3 Sync proof of concept on Windows. It assumes prerequisites are installed, Docker Desktop is running, and the repository is checked out at `Kennenlernen Contargo`.

## 1. Prepare the Environment

1. Open **Windows Terminal** or **PowerShell**.
2. Navigate to the project root:
   ```powershell
   cd "C:\Users\mschl\VSCode Projekte\Contargo\Kennenlernen Contargo"
   ```
3. Start infrastructure (PostgreSQL + LocalStack) and give containers ~15 seconds to initialize:
   ```powershell
   docker compose up -d
   docker compose ps
   ```
   Confirm both services are `running`.

## 2. Launch the Backend (Spring Boot)

1. Open a new PowerShell tab:
   ```powershell
   cd "C:\Users\mschl\VSCode Projekte\Contargo\Kennenlernen Contargo\backend"
   .\gradlew.bat bootRun
   ```
2. Wait until the log shows `Started S3SyncApplication`. The backend now listens on `http://localhost:8080`.
3. Optional health check via browser or `curl`:
   ```powershell
   curl http://localhost:8080/actuator/health
   ```

## 3. Launch the Frontend (Vue Dashboard)

1. Open another PowerShell tab:
   ```powershell
   cd "C:\Users\mschl\VSCode Projekte\Contargo\Kennenlernen Contargo\frontend"
   npm install   # skip if already installed
   npm run dev
   ```
2. Vite serves the dashboard at `http://localhost:5173`. Open the URL in your browser.

## 4. Demo Flow (UI Walkthrough)

### 4.1 Synchronize View (default landing page)

1. Confirm “Sync Overview” shows last sync timestamps (initially blank until first run).
2. Click **Run Sync Now** to trigger a manual export.
   - Observe the button spinner and check backend logs for `Sync run X completed successfully`.
   - After completion, the table updates with timestamps for `kunde` and `auftraege`.
3. Scroll to **Recent Sync Runs** to highlight the new entry.
   - Point out per-country CSV batches and their object keys.

### 4.2 AWS Connect View

1. Navigate to **AWS Connect** in the sidebar.
2. Click **Refresh**.
3. Show how customer and order files are grouped, including counts and last modified timestamps.
4. Demonstrate a **Download** link (optional) to show CSV output.

### 4.3 Data View

1. Open **Data View**.
2. Explain the aggregated list: customers grouped by country with their orders (sorted by most recent change).
3. Note that this reflects the same data exported to S3, sourced directly from PostgreSQL via REST endpoints.

### 4.4 Settings View

1. Switch to **Settings**.
2. Highlight the scheduler frequency controls (default 3 hours). Change to e.g. “0 hours / 30 minutes”, click **Save Changes**, and mention the backend applies the new interval immediately.
3. Demonstrate the **Run Update Script** button; this calls `POST /api/orders/lastchange/touch` and returns the number of rows touched. Observe the toast summary and the `Last Update` badge in the sidebar (managed via Pinia store).

## 5. API Highlights (optional CLI segment)

Run from a separate PowerShell tab while the backend is active:

```powershell
# Trigger another sync
curl -X POST http://localhost:8080/api/sync/run

# Fetch sync state
curl http://localhost:8080/api/sync/state | ConvertFrom-Json

# List S3 objects
curl http://localhost:8080/api/s3/files | ConvertFrom-Json
```

Use these outputs to reinforce backend capabilities and REST contract.

## 6. Show Test Confidence (optional)

Demonstrate the automated tests before or after the live UI walkthrough:

```powershell
cd "C:\Users\mschl\VSCode Projekte\Contargo\Kennenlernen Contargo\backend"
.\gradlew.bat test
```

Mention that integration tests spin up PostgreSQL + LocalStack via Testcontainers, mirroring the demo setup.

## 7. Wrap Up and Tear Down

1. Stop the frontend (`Ctrl+C` in the Vite terminal).
2. Stop the backend (`Ctrl+C` in the Gradle terminal).
3. Shut down infrastructure:
   ```powershell
   cd "C:\Users\mschl\VSCode Projekte\Contargo\Kennenlernen Contargo"
   docker compose down
   ```
4. Remind the audience that database data persists in the Docker volume between runs.

## 8. Troubleshooting Checklist

- **Containers won’t start / port already in use**
  ```powershell
  docker compose down
  wsl -l -v                       # ensure Docker’s WSL distro is running
  netstat -ano | findstr 5432     # check for lingering PostgreSQL listeners
  ```
  Kill the blocking process (for example `Stop-Process -Id <PID>`).

- **Reset LocalStack state**
  ```powershell
  docker compose down
  Remove-Item -Recurse -Force .\.localstack
  docker compose up -d
  ```
  This clears any S3 buckets/objects created in previous runs.

- **Restart a single container**
  ```powershell
  docker compose restart localstack
  docker compose restart postgres
  ```

- **Check container logs for errors**
  ```powershell
  docker compose logs -f localstack
  docker compose logs -f postgres
  ```

- **Backend fails to connect to PostgreSQL**
  ```powershell
  docker compose ps            # confirm postgres is healthy
  Test-NetConnection localhost -Port 5432
  ```
  Verify credentials in `backend/src/main/resources/application.yml` match PostgreSQL env variables.

- **LocalStack S3 errors (bucket not found / connection refused)**
  ```powershell
  aws --endpoint-url http://localhost:4566 s3 ls          # requires AWS CLI
  docker compose restart localstack
  ```
  Ensure Docker Desktop is not paused and LocalStack’s exposed port `4566` is free.

- **Frontend cannot reach backend**
  - Confirm backend running on `http://localhost:8080`.
  - Check Vite proxy config (`frontend/vite.config.ts`).
  - Refresh browser with cache disabled (Ctrl+F5).

- **Clean Gradle cache if builds behave oddly**
  ```powershell
  cd backend
  .\gradlew.bat --stop
  Remove-Item -Recurse -Force .\.gradle
  .\gradlew.bat clean bootRun
  ```

- **General Docker restart**
  - From system tray, exit Docker Desktop, then relaunch it.
  - Alternatively:
    ```powershell
    Stop-Service com.docker.service
    Start-Service com.docker.service
    ```

Keep this section handy during the demo to quickly recover from common issues.