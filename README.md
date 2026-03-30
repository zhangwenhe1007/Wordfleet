# Wordfleet

Wordfleet is a real-time, server-authoritative multiplayer word game. Beta uses a Java Spring Boot control plane, Java session servers on Agones, Redis state/leaderboards, and a React web client.

## Core beta features
- Turn-order elimination gameplay with **2-12 players per room**.
- WebSocket real-time game sessions.
- Guest auth for local development. Email magic-link scaffolding exists in the codebase but is not required for local run.
- Redis-backed room state with TTL and global leaderboards.
- Agones Fleet/FleetAutoscaler deployment manifests for GKE.

## Repository layout
- `services/control-plane` REST APIs for auth, rooms, leaderboards, and match ingestion.
- `services/session-server` authoritative game loop and WebSocket protocol.
- `libs/protocol` shared join-token and envelope models.
- `apps/web` browser client.
- `infra/k8s` and `infra/agones` deployment assets.
- `infra/scripts` GKE/bootstrap/build/deploy scripts.
- `tests/load` k6 scenarios.
- `docs` API/protocol/deploy/runbooks/account setup log.

## Local development

These are the full local run steps that match the current repo and the manual startup flow used during development.

### Prerequisites
- Java 21
- Maven
- Node.js 20+
- npm
- Redis

Docker is optional for local run. If Redis is already installed locally, you do not need Docker.

### Default local ports
- Control plane: `http://localhost:8080`
- Session server: `http://localhost:8090`
- Web app: `http://localhost:5173`
- Redis: `localhost:6379`

### 1. Start Redis

Option A: local Redis service
```bash
sudo service redis-server start
```

Option B: Docker Compose
```bash
cd ~/Wordfleet
docker compose up -d redis
```

### 2. Bootstrap local Maven dependencies once

Run these once before starting the Java services:

```bash
cd ~/Wordfleet
mvn -N install
mvn -pl libs/protocol install -DskipTests
```

Why this is needed:
- `services/control-plane` and `services/session-server` depend on the local `libs/protocol` module
- the root parent POM also needs to be installed in your local Maven cache

### 3. Start the control plane

Open a new terminal:

```bash
cd ~/Wordfleet/services/control-plane
mvn spring-boot:run
```

### 4. Start the session server

Open another terminal:

```bash
cd ~/Wordfleet/services/session-server
mvn spring-boot:run
```

### 5. Start the web app

Open a third terminal:

```bash
cd ~/Wordfleet/apps/web
npm install
npm run dev
```

### 6. Open the app

Open:

```text
http://localhost:5173
```

### 7. How to use it locally
- Use **guest login only**
- Do not use the email magic-link flow for local run
- A room stays in `WAITING` until at least 2 players join
- To test multiplayer locally, open the app in two browser windows or one normal window plus one incognito window

### 8. Local runtime defaults

The local setup already has fallback defaults in the service configs:
- control plane talks to Redis at `localhost:6379`
- session server talks to Redis at `localhost:6379`
- control plane falls back to session websocket URL `ws://localhost:8090/ws/room`
- local secrets default to development values, so no extra env vars are required for a basic local run

Relevant config files:
- `services/control-plane/src/main/resources/application.yml`
- `services/session-server/src/main/resources/application.yml`

### 9. Quick health checks

Once both Java services are up, you can verify them with:

```bash
curl http://localhost:8080/health
curl http://localhost:8090/health
```

### 10. Common local issues

If Maven cannot resolve local artifacts:

```bash
cd ~/Wordfleet
mvn -N install
mvn -pl libs/protocol install -DskipTests
```

If you see:

```text
No plugin found for prefix 'spring-boot'
```

start the service from its own module directory instead of from the repo root:

```bash
cd ~/Wordfleet/services/control-plane
mvn spring-boot:run
```

```bash
cd ~/Wordfleet/services/session-server
mvn spring-boot:run
```

If the frontend does not pick up a recent UI change, restart Vite:

```bash
cd ~/Wordfleet/apps/web
npm run dev
```

If gameplay/backend changes were made, restart the session server:

```bash
cd ~/Wordfleet/services/session-server
mvn spring-boot:run
```

## Deploy to GKE
Use `docs/DEPLOY_GKE.md` and scripts in `infra/scripts`.

## Security note
Never commit credential files. Metadata-only account logs are kept in `docs/account-setup-log.md`.
