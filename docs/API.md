# Control Plane API

## Auth
- `POST /v1/auth/guest`
- `POST /v1/auth/magic-link/request`
- `POST /v1/auth/magic-link/verify`
- `GET /v1/auth/me`

## Rooms
- `POST /v1/rooms` (minPlayers range 2-12)
- `POST /v1/rooms/{roomId}/join`
- `GET /v1/rooms/{roomId}`

## Leaderboards
- `GET /v1/leaderboards/daily?limit=50`
- `GET /v1/leaderboards/alltime?limit=50`

## Match callback
- `POST /v1/matches/{roomId}/complete`
- `GET /v1/matches/{roomId}`

Session callback signature header:
- `X-Session-Signature = Base64(HMAC_SHA256(roomId|winnerUserId|endedAtEpochMillis, SESSION_CALLBACK_SECRET))`
