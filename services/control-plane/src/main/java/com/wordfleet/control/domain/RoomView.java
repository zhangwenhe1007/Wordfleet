package com.wordfleet.control.domain;

import java.util.Set;

public record RoomView(String roomId,
                       String status,
                       int minPlayers,
                       int maxPlayers,
                       Set<String> players,
                       long createdAtEpochMillis,
                       String wsEndpoint) {
}
