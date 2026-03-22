#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

command -v kubectl >/dev/null || { echo "kubectl not found"; exit 1; }

kubectl apply -k "$ROOT_DIR/infra/k8s/base"
kubectl apply -f "$ROOT_DIR/infra/agones/session-fleet.yaml"
kubectl apply -f "$ROOT_DIR/infra/agones/fleet-autoscaler.yaml"

echo "Deployment manifests applied."
