package com.wordfleet.control.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public record MatchSummaryRequest(@NotBlank String winnerUserId,
                                  @NotEmpty Map<String, Integer> scores,
                                  @NotEmpty List<String> participants,
                                  long startedAtEpochMillis,
                                  long endedAtEpochMillis) {
}
