package com.wordfleet.session.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordfleet.session.config.WordfleetSessionProperties;
import com.wordfleet.session.game.DictionaryService;
import com.wordfleet.session.game.GameEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameRoomManager {
    private static final Logger log = LoggerFactory.getLogger(GameRoomManager.class);

    private final DictionaryService dictionaryService;
    private final RedisRoomStateRepository redisRoomStateRepository;
    private final ControlPlaneCallbackClient callbackClient;
    private final WordfleetSessionProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, RoomContext> rooms = new ConcurrentHashMap<>();

    public GameRoomManager(DictionaryService dictionaryService,
                           RedisRoomStateRepository redisRoomStateRepository,
                           ControlPlaneCallbackClient callbackClient,
                           WordfleetSessionProperties props) {
        this.dictionaryService = dictionaryService;
        this.redisRoomStateRepository = redisRoomStateRepository;
        this.callbackClient = callbackClient;
        this.props = props;
    }

    public void connect(String roomId, String userId, WebSocketSession session) {
        RoomContext room = rooms.computeIfAbsent(roomId, RoomContext::new);
        synchronized (room) {
            room.sessions.put(userId, session);
            if (!room.playerOrder.contains(userId)) {
                room.playerOrder.add(userId);
                room.alive.add(userId);
                room.lives.put(userId, 3);
                room.scores.put(userId, 0);
                room.streaks.put(userId, 0);
            }

            sendToUser(session, envelope("ROOM_STATE", statePayload(room)));

            if (room.status.equals("WAITING") && room.playerOrder.size() >= room.minPlayers) {
                room.status = "ACTIVE";
                room.startedAtEpochMillis = System.currentTimeMillis();
                room.turnPlayerId = null;
                room.turnIndex = -1;
                startNextTurnLocked(room, System.currentTimeMillis());
            } else {
                broadcast(room, envelope("PLAYER_JOINED", Map.of("userId", userId, "players", room.playerOrder.size())));
                broadcast(room, envelope("ROOM_STATE", statePayload(room)));
            }

            persist(room);
        }
    }

    public void disconnect(String roomId, String userId, String sessionId) {
        RoomContext room = rooms.get(roomId);
        if (room == null) {
            return;
        }

        synchronized (room) {
            WebSocketSession existing = room.sessions.get(userId);
            if (existing != null && existing.getId().equals(sessionId)) {
                room.sessions.remove(userId);
            }
            broadcast(room, envelope("PLAYER_DISCONNECTED", Map.of("userId", userId)));
            persist(room);
        }
    }

    public void submitWord(String roomId, String userId, String rawWord) {
        RoomContext room = rooms.get(roomId);
        if (room == null) {
            return;
        }

        synchronized (room) {
            if (!"ACTIVE".equals(room.status)) {
                return;
            }
            if (!userId.equals(room.turnPlayerId)) {
                broadcastToUser(room, userId, envelope("WORD_REJECTED", Map.of("reason", "Not your turn")));
                return;
            }

            long now = System.currentTimeMillis();
            if (now > room.turnEndsAtMillis) {
                handleTimeoutLocked(room, now);
                persist(room);
                return;
            }

            long lockout = room.lockoutUntilMillis.getOrDefault(userId, 0L);
            if (lockout > now) {
                broadcastToUser(room, userId, envelope("WORD_REJECTED", Map.of("reason", "Input lockout active")));
                return;
            }

            String word = DictionaryService.normalize(rawWord);
            if (word.length() < 4 || !dictionaryService.isKnownWord(word)) {
                applyInvalidPenalty(room, userId, now, "Not in dictionary");
                return;
            }

            if (!word.contains(room.substring)) {
                applyInvalidPenalty(room, userId, now, "Missing required substring");
                return;
            }

            if (room.eventType == GameEventType.HARD_MODE_DOUBLE_SUBSTRING
                    && dictionaryService.occurrences(word, room.substring) < 2) {
                applyInvalidPenalty(room, userId, now, "Hard mode requires substring twice");
                return;
            }

            if (room.eventType == GameEventType.NO_REPEAT_OVER_2 && repeatsAnyCharMoreThan(word, 2)) {
                applyInvalidPenalty(room, userId, now, "No character can repeat more than twice");
                return;
            }

            if (room.bannedWords.contains(word)) {
                room.scores.compute(userId, (k, v) -> (v == null ? 0 : v) - 2);
                room.streaks.put(userId, 0);
                room.lockoutUntilMillis.put(userId, now + 2000);
                broadcast(room, envelope("WORD_REJECTED", Map.of("reason", "Word already used", "penalty", -2)));
                persist(room);
                return;
            }

            int streak = room.streaks.getOrDefault(userId, 0) + 1;
            room.streaks.put(userId, streak);
            int points = calculatePoints(word.length(), room.tier, streak);
            if (room.eventType == GameEventType.DOUBLE_POINTS) {
                points = points * 2;
            }

            room.scores.compute(userId, (k, v) -> (v == null ? 0 : v) + points);
            room.bannedWords.add(word);
            room.recentSolveMillis.addLast(Math.max(0, now - room.turnStartedAtMillis));
            trimDeque(room.recentSolveMillis, 5);
            room.recentTimeouts.addLast(false);
            trimDeque(room.recentTimeouts, 3);

            broadcast(room, envelope("WORD_ACCEPTED", Map.of(
                    "userId", userId,
                    "word", word,
                    "points", points,
                    "newScore", room.scores.getOrDefault(userId, 0))));

            startNextTurnLocked(room, now);
            persist(room);
        }
    }

    @Scheduled(fixedDelay = 250)
    public void tickTimeouts() {
        long now = System.currentTimeMillis();
        for (RoomContext room : rooms.values()) {
            synchronized (room) {
                if ("ACTIVE".equals(room.status) && room.turnEndsAtMillis > 0 && now > room.turnEndsAtMillis) {
                    handleTimeoutLocked(room, now);
                    persist(room);
                }
            }
        }
    }

    private void applyInvalidPenalty(RoomContext room, String userId, long now, String reason) {
        room.scores.compute(userId, (k, v) -> (v == null ? 0 : v) - 1);
        room.streaks.put(userId, 0);
        room.lockoutUntilMillis.put(userId, now + 1000);
        broadcast(room, envelope("WORD_REJECTED", Map.of("reason", reason, "penalty", -1)));
        persist(room);
    }

    private void handleTimeoutLocked(RoomContext room, long now) {
        if (!"ACTIVE".equals(room.status) || room.turnPlayerId == null) {
            return;
        }

        String userId = room.turnPlayerId;
        room.scores.compute(userId, (k, v) -> (v == null ? 0 : v) - 3);
        room.streaks.put(userId, 0);
        room.recentTimeouts.addLast(true);
        trimDeque(room.recentTimeouts, 3);

        boolean shielded = room.shieldedUsers.remove(userId);
        if (!shielded) {
            int livesLeft = room.lives.getOrDefault(userId, 1) - 1;
            room.lives.put(userId, livesLeft);
            if (livesLeft <= 0) {
                room.alive.remove(userId);
                broadcast(room, envelope("PLAYER_ELIMINATED", Map.of("userId", userId)));
            }
        }

        broadcast(room, envelope("TURN_TIMEOUT", Map.of(
                "userId", userId,
                "scorePenalty", -3,
                "shielded", shielded,
                "lives", room.lives.getOrDefault(userId, 0))));

        if (room.alive.size() <= 1) {
            finishMatchLocked(room, now);
            return;
        }

        startNextTurnLocked(room, now);
    }

    private void startNextTurnLocked(RoomContext room, long now) {
        if (room.alive.size() <= 1) {
            finishMatchLocked(room, now);
            return;
        }

        List<String> order = room.orderedPlayers();
        if (order.isEmpty()) {
            return;
        }

        int nextIndex = findNextAliveIndex(room, order, room.turnIndex);
        if (nextIndex < 0) {
            finishMatchLocked(room, now);
            return;
        }

        room.turnIndex = nextIndex;
        room.turnPlayerId = order.get(nextIndex);
        room.turnCounter++;
        adjustDifficultyTier(room);
        room.substring = dictionaryService.pickSubstringForTier(room.tier, room.random);

        room.eventType = rollEvent(room);
        int turnSeconds = room.eventType == GameEventType.SUDDEN_DEATH_3S ? 3 : 8;
        if (room.eventType == GameEventType.SHIELD_NEXT_TIMEOUT) {
            room.shieldedUsers.add(room.turnPlayerId);
        }

        room.turnStartedAtMillis = now;
        room.turnEndsAtMillis = now + (turnSeconds * 1000L);

        broadcast(room, envelope("TURN_STARTED", Map.of(
                "roomId", room.roomId,
                "turnPlayerId", room.turnPlayerId,
                "substring", room.substring,
                "tier", room.tier,
                "eventType", room.eventType.name(),
                "turnEndsAt", room.turnEndsAtMillis)));
        broadcast(room, envelope("ROOM_STATE", statePayload(room)));
    }

    private void finishMatchLocked(RoomContext room, long now) {
        room.status = "ENDED";
        room.endedAtEpochMillis = now;
        room.turnEndsAtMillis = 0;

        if (room.alive.size() == 1) {
            room.winnerUserId = room.alive.iterator().next();
        } else {
            room.winnerUserId = room.scores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("unknown");
        }

        broadcast(room, envelope("MATCH_ENDED", Map.of(
                "roomId", room.roomId,
                "winnerUserId", room.winnerUserId,
                "scores", room.scores)));
        broadcast(room, envelope("ROOM_STATE", statePayload(room)));
        persist(room);
        callbackClient.sendMatchSummary(room);
    }

    private int findNextAliveIndex(RoomContext room, List<String> order, int currentIndex) {
        int size = order.size();
        for (int offset = 1; offset <= size; offset++) {
            int idx = (currentIndex + offset + size) % size;
            if (room.alive.contains(order.get(idx))) {
                return idx;
            }
        }
        return -1;
    }

    private void adjustDifficultyTier(RoomContext room) {
        int tier = room.tier;

        if (room.recentSolveMillis.size() >= 5) {
            long sum = 0;
            for (Long value : room.recentSolveMillis) {
                sum += value;
            }
            long avg = sum / room.recentSolveMillis.size();
            if (avg < 3000) {
                tier++;
            }
        }

        boolean timeoutInLastThree = room.recentTimeouts.stream().anyMatch(Boolean::booleanValue);
        if (timeoutInLastThree) {
            tier--;
        }

        room.tier = Math.max(1, Math.min(4, tier));
    }

    private GameEventType rollEvent(RoomContext room) {
        if (room.turnCounter % 3 != 0) {
            return GameEventType.NONE;
        }
        if (room.random.nextDouble() > 0.2) {
            return GameEventType.NONE;
        }

        List<GameEventType> events = List.of(
                GameEventType.DOUBLE_POINTS,
                GameEventType.SUDDEN_DEATH_3S,
                GameEventType.SHIELD_NEXT_TIMEOUT,
                GameEventType.HARD_MODE_DOUBLE_SUBSTRING,
                GameEventType.NO_REPEAT_OVER_2
        );
        return events.get(room.random.nextInt(events.size()));
    }

    private int calculatePoints(int wordLength, int tier, int streak) {
        int base = 2;
        int lengthBonus = Math.max(0, wordLength - 4);
        double difficultyMultiplier = switch (tier) {
            case 2 -> 1.2;
            case 3 -> 1.5;
            case 4 -> 2.0;
            default -> 1.0;
        };
        double streakMultiplier = 1 + 0.1 * Math.min(10, streak);
        return (int) Math.floor((base + lengthBonus) * difficultyMultiplier * streakMultiplier);
    }

    private boolean repeatsAnyCharMoreThan(String word, int limit) {
        Map<Character, Integer> counts = new HashMap<>();
        for (char c : word.toCharArray()) {
            int count = counts.getOrDefault(c, 0) + 1;
            if (count > limit) {
                return true;
            }
            counts.put(c, count);
        }
        return false;
    }

    private void trimDeque(java.util.Deque<?> deque, int size) {
        while (deque.size() > size) {
            deque.removeFirst();
        }
    }

    private Map<String, Object> statePayload(RoomContext room) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("roomId", room.roomId);
        payload.put("status", room.status);
        payload.put("minPlayers", room.minPlayers);
        payload.put("maxPlayers", room.maxPlayers);
        payload.put("players", new ArrayList<>(room.playerOrder));
        payload.put("alive", new ArrayList<>(room.alive));
        payload.put("scores", new HashMap<>(room.scores));
        payload.put("lives", new HashMap<>(room.lives));
        payload.put("turnPlayerId", room.turnPlayerId);
        payload.put("substring", room.substring);
        payload.put("turnEndsAt", room.turnEndsAtMillis);
        payload.put("eventType", room.eventType.name());
        payload.put("tier", room.tier);
        return payload;
    }

    private Map<String, Object> envelope(String type, Map<String, Object> payload) {
        return Map.of("type", type, "payload", payload);
    }

    private void broadcast(RoomContext room, Map<String, Object> message) {
        String json = toJson(message);
        if (json == null) {
            return;
        }
        TextMessage text = new TextMessage(json);
        for (Map.Entry<String, WebSocketSession> entry : new ArrayList<>(room.sessions.entrySet())) {
            sendToUser(entry.getValue(), text);
        }
    }

    private void broadcastToUser(RoomContext room, String userId, Map<String, Object> message) {
        Optional.ofNullable(room.sessions.get(userId)).ifPresent(session -> sendToUser(session, message));
    }

    private void sendToUser(WebSocketSession session, Map<String, Object> message) {
        String json = toJson(message);
        if (json == null) {
            return;
        }
        sendToUser(session, new TextMessage(json));
    }

    private void sendToUser(WebSocketSession session, TextMessage message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(message);
        } catch (IOException ex) {
            log.debug("Failed to send websocket message: {}", ex.getMessage());
        }
    }

    private String toJson(Map<String, Object> message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception ex) {
            log.warn("Unable to serialize websocket message: {}", ex.getMessage());
            return null;
        }
    }

    private void persist(RoomContext room) {
        redisRoomStateRepository.persist(room, props.getRoomTtlSeconds());
    }
}
