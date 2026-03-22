package com.wordfleet.control.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateRoomRequest(@Min(2) @Max(12) int minPlayers,
                                @Min(2) @Max(12) int maxPlayers) {
}
