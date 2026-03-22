package com.wordfleet.control.service;

import com.wordfleet.control.domain.LeaderboardEntry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LeaderboardService {
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final StringRedisTemplate redis;

    public LeaderboardService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void applyMatchScores(Map<String, Integer> scores, long endedAtEpochMillis) {
        String dailyKey = "leaderboard:daily:" + DAY_FMT.format(Instant.ofEpochMilli(endedAtEpochMillis));
        scores.forEach((userId, score) -> {
            redis.opsForZSet().incrementScore("leaderboard:alltime", userId, score);
            redis.opsForZSet().incrementScore(dailyKey, userId, score);
        });
    }

    public List<LeaderboardEntry> topAllTime(int limit) {
        return top("leaderboard:alltime", limit);
    }

    public List<LeaderboardEntry> topDaily(int limit) {
        String dailyKey = "leaderboard:daily:" + DAY_FMT.format(Instant.now());
        return top(dailyKey, limit);
    }

    private List<LeaderboardEntry> top(String key, int limit) {
        Set<ZSetOperations.TypedTuple<String>> tuples = redis.opsForZSet()
                .reverseRangeWithScores(key, 0, Math.max(0, limit - 1));

        List<LeaderboardEntry> result = new ArrayList<>();
        if (tuples == null) {
            return result;
        }

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null || tuple.getScore() == null) {
                continue;
            }
            result.add(new LeaderboardEntry(tuple.getValue(), tuple.getScore()));
        }
        return result;
    }
}
