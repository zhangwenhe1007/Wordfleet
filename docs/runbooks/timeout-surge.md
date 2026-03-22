# Runbook: Timeout Surge

## Symptoms
- `TURN_TIMEOUT` events spike.
- Player drop-off increases.

## Checks
1. Verify websocket connection health and packet loss.
2. Inspect recent event mix for too many `SUDDEN_DEATH_3S` rounds.
3. Validate scheduler tick drift on session servers.

## Mitigation
1. Increase default turn timer from 8s to 10s via config rollout.
2. Reduce event probability from 0.2 to 0.1.
3. Scale session fleet to reduce noisy-neighbor latency.
