package com.wordfleet.session.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class RedisRoomStateRepository {
    private final StringRedisTemplate redis;

    public RedisRoomStateRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void persist(RoomContext room, long ttlSeconds) {
        String metaKey = "room:" + room.roomId + ":meta";
        String playersKey = "room:" + room.roomId + ":players";
        String aliveKey = "room:" + room.roomId + ":alive";
        String livesKey = "room:" + room.roomId + ":lives";
        String scoresKey = "room:" + room.roomId + ":scores";
        String bannedKey = "room:" + room.roomId + ":banned";

        Map<String, String> meta = new HashMap<>();
        meta.put("substring", room.substring);
        meta.put("tier", Integer.toString(room.tier));
        meta.put("turnIndex", Integer.toString(room.turnIndex));
        meta.put("turnPlayerId", room.turnPlayerId == null ? "" : room.turnPlayerId);
        meta.put("turnEndsAt", Long.toString(room.turnEndsAtMillis));
        meta.put("eventType", room.eventType.name());
        meta.put("status", room.status);
        redis.opsForHash().putAll(metaKey, meta);

        redis.delete(playersKey);
        List<String> orderedPlayers = room.orderedPlayers();
        if (!orderedPlayers.isEmpty()) {
            redis.opsForList().rightPushAll(playersKey, orderedPlayers);
        }

        redis.delete(aliveKey);
        if (!room.alive.isEmpty()) {
            redis.opsForSet().add(aliveKey, room.alive.toArray(new String[0]));
        }

        redis.delete(livesKey);
        if (!room.lives.isEmpty()) {
            Map<String, String> lives = new HashMap<>();
            room.lives.forEach((k, v) -> lives.put(k, Integer.toString(v)));
            redis.opsForHash().putAll(livesKey, lives);
        }

        redis.delete(scoresKey);
        ZSetOperations<String, String> zset = redis.opsForZSet();
        room.scores.forEach((userId, score) -> zset.add(scoresKey, userId, score));

        redis.delete(bannedKey);
        if (!room.bannedWords.isEmpty()) {
            redis.opsForSet().add(bannedKey, room.bannedWords.toArray(new String[0]));
        }

        Duration ttl = Duration.ofSeconds(ttlSeconds);
        redis.expire(metaKey, ttl);
        redis.expire(playersKey, ttl);
        redis.expire(aliveKey, ttl);
        redis.expire(livesKey, ttl);
        redis.expire(scoresKey, ttl);
        redis.expire(bannedKey, ttl);
    }
}
