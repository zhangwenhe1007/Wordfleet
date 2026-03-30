import { useEffect, useMemo, useRef, useState } from 'react'

type AuthState = {
  userId: string
  displayName: string
  sessionToken: string
}

type JoinRoomResponse = {
  roomId: string
  userId: string
  wsEndpoint: string
  joinToken: string
  expiresAtEpochSeconds: number
}

type RoomStatePayload = {
  roomId: string
  status: string
  minPlayers: number
  maxPlayers: number
  players: string[]
  playerNames: Record<string, string>
  alive: string[]
  scores: Record<string, number>
  lives: Record<string, number>
  turnPlayerId: string | null
  substring: string
  turnEndsAt: number
  eventType: string
  tier: number
  winnerUserId?: string | null
}

const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

export default function App() {
  const [displayName, setDisplayName] = useState('')
  const [auth, setAuth] = useState<AuthState | null>(null)
  const [roomIdInput, setRoomIdInput] = useState('')
  const [roomId, setRoomId] = useState<string | null>(null)
  const [word, setWord] = useState('')
  const [logs, setLogs] = useState<string[]>([])
  const [roomState, setRoomState] = useState<RoomStatePayload | null>(null)
  const [error, setError] = useState<string>('')
  const [now, setNow] = useState(() => Date.now())
  const wsRef = useRef<WebSocket | null>(null)

  useEffect(() => {
    const timerId = window.setInterval(() => setNow(Date.now()), 200)
    return () => window.clearInterval(timerId)
  }, [])

  const isMyTurn = useMemo(() => {
    if (!auth || !roomState) return false
    return roomState.turnPlayerId === auth.userId
  }, [auth, roomState])

  const playerLabel = (playerId: string) => roomState?.playerNames?.[playerId] ?? playerId

  const turnCountdownSeconds = useMemo(() => {
    if (!roomState || roomState.status !== 'ACTIVE' || !roomState.turnEndsAt) {
      return null
    }
    return Math.max(0, (roomState.turnEndsAt - now) / 1000)
  }, [now, roomState])

  const turnTimerLabel = turnCountdownSeconds === null ? 'Waiting' : `${turnCountdownSeconds.toFixed(1)}s`
  const turnPlayerLabel = roomState?.turnPlayerId ? playerLabel(roomState.turnPlayerId) : 'N/A'

  async function authGuest() {
    setError('')
    const res = await fetch(`${API_BASE}/v1/auth/guest`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ displayName })
    })
    if (!res.ok) {
      setError(`Guest auth failed (${res.status})`)
      return
    }
    const data = (await res.json()) as AuthState
    setAuth(data)
  }

  async function createRoom() {
    if (!auth) return
    setError('')
    const res = await fetch(`${API_BASE}/v1/rooms`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${auth.sessionToken}`
      },
      body: JSON.stringify({ minPlayers: 2, maxPlayers: 8 })
    })

    if (!res.ok) {
      setError(`Create room failed (${res.status})`)
      return
    }

    const created = await res.json()
    setRoomIdInput(created.roomId)
    await joinRoom(created.roomId)
  }

  async function joinRoom(explicitRoomId?: string) {
    if (!auth) return
    const targetRoomId = explicitRoomId ?? roomIdInput.trim()
    if (!targetRoomId) return

    setError('')
    const res = await fetch(`${API_BASE}/v1/rooms/${targetRoomId}/join`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${auth.sessionToken}`
      }
    })

    if (!res.ok) {
      setError(`Join room failed (${res.status})`)
      return
    }

    const data = (await res.json()) as JoinRoomResponse
    setRoomId(data.roomId)
    connectWs(data)
  }

  function connectWs(data: JoinRoomResponse) {
    wsRef.current?.close()
    const ws = new WebSocket(`${data.wsEndpoint}?token=${data.joinToken}`)
    wsRef.current = ws

    ws.onopen = () => setLogs(prev => [`Connected to room ${data.roomId}`, ...prev].slice(0, 30))
    ws.onclose = () => setLogs(prev => ['Socket disconnected', ...prev].slice(0, 30))
    ws.onerror = () => setLogs(prev => ['Socket error', ...prev].slice(0, 30))
    ws.onmessage = event => {
      try {
        const msg = JSON.parse(event.data)
        if (msg.type === 'ROOM_STATE') {
          setRoomState(msg.payload)
        }
        setLogs(prev => [`${msg.type}: ${JSON.stringify(msg.payload)}`, ...prev].slice(0, 40))
      } catch {
        setLogs(prev => [`RAW: ${event.data}`, ...prev].slice(0, 40))
      }
    }
  }

  function submitWord() {
    if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN || !word.trim()) {
      return
    }
    wsRef.current.send(JSON.stringify({ type: 'SUBMIT_WORD', payload: { word } }))
    setWord('')
  }

  function handleWordSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    submitWord()
  }

  function startNewGame() {
    if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN || roomState?.status !== 'ENDED') {
      return
    }
    wsRef.current.send(JSON.stringify({ type: 'START_NEW_GAME', payload: {} }))
  }

  function playerMatchStatus(playerId: string) {
    if (!roomState) {
      return 'Waiting'
    }

    if (roomState.status === 'ENDED') {
      return roomState.winnerUserId === playerId ? 'Winner' : 'Eliminated'
    }

    if (roomState.turnPlayerId === playerId && roomState.status === 'ACTIVE') {
      return `Now • ${turnTimerLabel}`
    }

    return roomState.alive.includes(playerId) ? 'Waiting' : 'Eliminated'
  }

  return (
    <main className="app-shell">
      <section className="panel hero">
        <h1>Wordfleet Beta Console</h1>
        <p>Turn-order survival word game. Minimum 2 players per room.</p>
      </section>

      {!auth && (
        <section className="panel auth-panel">
          <h2>Authenticate</h2>
          <div className="row">
            <input
              value={displayName}
              placeholder="Display name"
              onChange={e => setDisplayName(e.target.value)}
            />
            <button onClick={authGuest}>Continue as Guest</button>
          </div>
        </section>
      )}

      {auth && (
        <>
          <section className="panel">
            <h2>Player</h2>
            <p>{auth.displayName} ({auth.userId})</p>
          </section>

          <section className="panel room-panel">
            <h2>Room</h2>
            <div className="row">
              <button onClick={createRoom}>Create Room</button>
              <input
                value={roomIdInput}
                placeholder="Room ID"
                onChange={e => setRoomIdInput(e.target.value)}
              />
              <button onClick={() => joinRoom()}>Join</button>
            </div>
            {roomId && <p>Active Room: {roomId}</p>}
          </section>

          <section className="panel gameplay">
            <h2>Gameplay</h2>
            <div className="stats">
              <div>Status: {roomState?.status ?? 'N/A'}</div>
              <div>Turn: {turnPlayerLabel}</div>
              <div>Timer: {turnTimerLabel}</div>
              <div>Substring: {roomState?.substring ?? 'N/A'}</div>
              <div>Event: {roomState?.eventType ?? 'NONE'}</div>
            </div>

            <form className="row submit-row" onSubmit={handleWordSubmit}>
              <input
                value={word}
                placeholder={isMyTurn ? 'Your word' : 'Wait for your turn'}
                onChange={e => setWord(e.target.value)}
                disabled={!isMyTurn}
              />
              <button type="submit" disabled={!isMyTurn}>Submit</button>
            </form>

            {roomState && (
              <>
                <table>
                  <thead>
                    <tr>
                      <th>Player</th>
                      <th>Score</th>
                      <th>Lives</th>
                      <th>Turn</th>
                    </tr>
                  </thead>
                  <tbody>
                    {roomState.players.map(playerId => (
                      <tr
                        key={playerId}
                        className={
                          roomState.status === 'ENDED'
                            ? roomState.winnerUserId === playerId
                              ? 'current-turn'
                              : undefined
                            : roomState.turnPlayerId === playerId
                              ? 'current-turn'
                              : undefined
                        }
                      >
                        <td>{playerLabel(playerId)}</td>
                        <td>{roomState.scores[playerId] ?? 0}</td>
                        <td>{roomState.lives[playerId] ?? 0}</td>
                        <td>{playerMatchStatus(playerId)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                {roomState.status === 'ENDED' && (
                  <div className="row post-game-actions">
                    <button onClick={startNewGame}>New Game</button>
                  </div>
                )}
              </>
            )}
          </section>

          <section className="panel logs">
            <h2>Event Log</h2>
            <pre>{logs.join('\n')}</pre>
          </section>
        </>
      )}

      {error && <section className="panel error">{error}</section>}
    </main>
  )
}
