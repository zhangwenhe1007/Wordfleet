package com.wordfleet.session.service;

import com.wordfleet.session.game.GameEventType;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

class RoomContext {
    final String roomId;
    final LinkedHashSet<String> playerOrder = new LinkedHashSet<>();
    final Set<String> alive = new HashSet<>();
    final Map<String, String> playerNames = new HashMap<>();
    final Map<String, Integer> lives = new HashMap<>();
    final Map<String, Integer> scores = new HashMap<>();
    final Map<String, Integer> streaks = new HashMap<>();
    final Set<String> bannedWords = new HashSet<>();
    final Map<String, Long> lockoutUntilMillis = new HashMap<>();
    final Set<String> shieldedUsers = new HashSet<>();
    final Deque<Long> recentSolveMillis = new ArrayDeque<>();
    final Deque<Boolean> recentTimeouts = new ArrayDeque<>();
    final Map<String, WebSocketSession> sessions = new HashMap<>();
    final Random random = new Random();

    String status = "WAITING";
    int minPlayers = 2;
    int maxPlayers = 12;
    int tier = 1;
    int turnIndex = 0;
    int turnCounter = 0;
    String turnPlayerId;
    String substring = "";
    long turnStartedAtMillis;
    long turnEndsAtMillis;
    GameEventType eventType = GameEventType.NONE;
    long startedAtEpochMillis;
    long endedAtEpochMillis;
    String winnerUserId;

    RoomContext(String roomId) {
        this.roomId = roomId;
    }

    List<String> orderedPlayers() {
        return new ArrayList<>(playerOrder);
    }
}
