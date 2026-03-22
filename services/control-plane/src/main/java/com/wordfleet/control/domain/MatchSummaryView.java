package com.wordfleet.control.domain;

import java.util.List;
import java.util.Map;

public record MatchSummaryView(String roomId,
                               String winnerUserId,
                               Map<String, Integer> scores,
                               List<String> participants,
                               long startedAtEpochMillis,
                               long endedAtEpochMillis,
                               long storedAtEpochMillis) {
}
