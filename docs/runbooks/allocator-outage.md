# Runbook: Agones Allocation Outage

## Symptoms
- `POST /v1/rooms` succeeds but returns fallback local endpoint unexpectedly.
- Room creation latency spikes.

## Checks
1. Verify Agones allocator service health.
2. Confirm `AGONES_ALLOCATOR_URL` in `control-plane-config`.
3. Inspect control-plane logs for allocation fallback events.

## Mitigation
1. Scale fleet buffer up.
2. Temporarily route to fallback session endpoint only for staging.
3. Restart control-plane deployment after allocator recovery.
