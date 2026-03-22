# Deploy Wordfleet Beta to GKE

## Prerequisites
- `gcloud`, `kubectl`, `docker`, `mvn` installed.
- Service-account key with required IAM roles.
- Agones installed in cluster.

## 1) Authenticate and bootstrap cluster
```bash
cd /home/wenhe/Wordfleet
PROJECT_ID=wordfleet-487310 \
REGION=us-central1 \
SA_KEY_PATH=/home/wenhe/.gcp/wordfleet-deployer.json \
./infra/scripts/bootstrap_gke.sh
```

## 2) Build and push container images
```bash
PROJECT_ID=wordfleet-487310 \
REGION=us-central1 \
TAG=beta \
./infra/scripts/build_and_push.sh
```

## 3) Install Agones (if not already)
```bash
helm repo add agones https://agones.dev/chart/stable
helm repo update
helm upgrade --install agones agones/agones --namespace agones-system --create-namespace
```

## 4) Apply manifests
```bash
./infra/scripts/deploy_beta.sh
```

## 5) Configure secrets
```bash
kubectl -n wordfleet edit secret control-plane-secrets
```
Set:
- `JOIN_TOKEN_SECRET`
- `SESSION_CALLBACK_SECRET`
- `SENDGRID_API_KEY` (optional but required for real magic-link email delivery)

## 6) Get endpoints
```bash
kubectl -n wordfleet get ingress
kubectl -n wordfleet get svc control-plane
```

## 7) Smoke checks
```bash
curl http://<INGRESS_IP>/health
curl http://<INGRESS_IP>/v1/leaderboards/alltime
```
