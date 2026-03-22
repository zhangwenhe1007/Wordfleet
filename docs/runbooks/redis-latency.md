# Runbook: Redis Latency / Saturation

## Symptoms
- Increased word submit latency.
- Missed turn transitions or delayed ROOM_STATE updates.

## Checks
1. `kubectl top pod -n wordfleet redis-0`
2. Redis INFO for memory and ops/sec.
3. Network latency from session pods to redis service.

## Mitigation
1. Increase redis pod resources.
2. Enable read replica for heavy read traffic (if needed).
3. Reduce state snapshot frequency temporarily.
