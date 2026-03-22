# Wordfleet

Wordfleet is a real-time, server-authoritative multiplayer word game. Beta uses a Java Spring Boot control plane, Java session servers on Agones, Redis state/leaderboards, and a React web client.

## Core beta features
- Turn-order elimination gameplay with **2-12 players per room**.
- WebSocket real-time game sessions.
- Guest auth + email magic-link auth (SendGrid integration).
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
Prerequisites: Java 21, Maven, Node 20+, Docker.

1. Start Redis:
```bash
docker compose up -d redis
```

2. Start control plane:
```bash
cd services/control-plane
mvn spring-boot:run
```

3. Start session server:
```bash
cd services/session-server
mvn spring-boot:run
```

4. Start web app:
```bash
cd apps/web
npm install
npm run dev
```

5. Open `http://localhost:5173`.

## Deploy to GKE
Use `docs/DEPLOY_GKE.md` and scripts in `infra/scripts`.

## Security note
Never commit credential files. Metadata-only account logs are kept in `docs/account-setup-log.md`.
