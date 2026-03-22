# Session WebSocket Protocol

Endpoint:
- `ws://<host>/ws/room/{roomId}?token=<join_token>`

Client -> server:
- `SUBMIT_WORD` payload `{ "word": "string" }`
- `PING` payload `{}`

Server -> client:
- `ROOM_STATE`
- `PLAYER_JOINED`
- `PLAYER_DISCONNECTED`
- `TURN_STARTED`
- `WORD_ACCEPTED`
- `WORD_REJECTED`
- `TURN_TIMEOUT`
- `PLAYER_ELIMINATED`
- `MATCH_ENDED`
- `ERROR`

Envelope format:
```json
{
  "type": "TURN_STARTED",
  "payload": {}
}
```
