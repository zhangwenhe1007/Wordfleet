# Wordfleet Beta Implementation Status

## Date
- 2026-02-13

## Completed implementation
- Monorepo structure for `control-plane`, `session-server`, `protocol`, `web`, `infra`, `tests`, `docs`.
- Control-plane Spring Boot API:
  - guest auth
  - magic-link request/verify flow
  - room create/join/get
  - daily/all-time leaderboard APIs
  - signed match completion callback ingestion
- Session-server Spring Boot WebSocket service:
  - join-token verified room connections
  - authoritative turn-order game loop
  - minimum 2 players to start
  - elimination mode, scoring, penalties, events, timers
  - Redis room snapshot persistence
  - control-plane match summary callback
  - Agones SDK ready/health integration hooks
- Shared protocol module:
  - HMAC join token issue/verify
  - websocket envelope model
- Web client (React + Vite):
  - guest auth
  - room create/join
  - websocket gameplay console
  - score/lives/turn state rendering
- Infra and operations:
  - Dockerfiles
  - K8s manifests for namespace, redis, control-plane, session service, ingress, HPA
  - Agones fleet and fleet autoscaler manifests
  - bootstrap/build/deploy scripts for GKE
  - CI workflow
  - k6 load scenario
  - API/protocol/deploy docs and runbooks
  - account metadata log (`docs/account-setup-log.md`)

## Environment blockers (not fixable automatically here)
- `java`, `mvn`, `gcloud`, and `kubectl` are not installed on this machine.
- npm dependency fetch stalled in this environment, so web build could not be executed.
- SendGrid account/API key creation requires external manual signup.

## Manual steps required to run/deploy
1. Install prerequisites: Java 21, Maven, gcloud CLI, kubectl.
2. Install web deps:
   - `cd /home/wenhe/Wordfleet/apps/web && npm install`
3. Run backend tests/build:
   - `cd /home/wenhe/Wordfleet && mvn -pl libs/protocol,services/control-plane,services/session-server -am test`
4. Provision/deploy on GCP:
   - follow `docs/DEPLOY_GKE.md`
5. Configure secret values:
   - `JOIN_TOKEN_SECRET`, `SESSION_CALLBACK_SECRET`, `SENDGRID_API_KEY`

## Notes
- Minimum players is implemented as 2 across backend validation, game runtime, and web client defaults.
- No secret values were logged; metadata only.
