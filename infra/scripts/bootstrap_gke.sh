#!/usr/bin/env bash
set -euo pipefail

PROJECT_ID="${PROJECT_ID:-wordfleet-487310}"
REGION="${REGION:-us-central1}"
CLUSTER_NAME="${CLUSTER_NAME:-wordfleet-beta}"
SA_KEY_PATH="${SA_KEY_PATH:-/home/wenhe/.gcp/wordfleet-deployer.json}"

command -v gcloud >/dev/null || { echo "gcloud not found"; exit 1; }
command -v kubectl >/dev/null || { echo "kubectl not found"; exit 1; }

if [[ ! -f "$SA_KEY_PATH" ]]; then
  echo "Service-account key not found at $SA_KEY_PATH"
  exit 1
fi

gcloud auth activate-service-account --key-file "$SA_KEY_PATH"
gcloud config set project "$PROJECT_ID"
gcloud services enable container.googleapis.com artifactregistry.googleapis.com compute.googleapis.com iam.googleapis.com

gcloud container clusters create-auto "$CLUSTER_NAME" \
  --region "$REGION" \
  --project "$PROJECT_ID" \
  --release-channel regular

gcloud container clusters get-credentials "$CLUSTER_NAME" --region "$REGION" --project "$PROJECT_ID"

kubectl create namespace wordfleet --dry-run=client -o yaml | kubectl apply -f -

echo "GKE bootstrap complete for project=$PROJECT_ID cluster=$CLUSTER_NAME region=$REGION"
