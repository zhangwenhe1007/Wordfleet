#!/usr/bin/env bash
set -euo pipefail

PROJECT_ID="${PROJECT_ID:-wordfleet-487310}"
REGION="${REGION:-us-central1}"
REPO="${REPO:-wordfleet}"
TAG="${TAG:-beta}"

command -v gcloud >/dev/null || { echo "gcloud not found"; exit 1; }
command -v docker >/dev/null || { echo "docker not found"; exit 1; }
command -v mvn >/dev/null || { echo "mvn not found"; exit 1; }

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

mvn -q -pl services/control-plane,services/session-server -am package -DskipTests

gcloud artifacts repositories create "$REPO" \
  --repository-format=docker \
  --location="$REGION" \
  --description="Wordfleet container repo" || true

gcloud auth configure-docker "$REGION-docker.pkg.dev" --quiet

CONTROL_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$REPO/control-plane:$TAG"
SESSION_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$REPO/session-server:$TAG"

docker build -t "$CONTROL_IMAGE" services/control-plane
docker build -t "$SESSION_IMAGE" services/session-server

docker push "$CONTROL_IMAGE"
docker push "$SESSION_IMAGE"

echo "Images pushed:"
echo "  $CONTROL_IMAGE"
echo "  $SESSION_IMAGE"
