package com.wordfleet.control.domain;

public record CreateRoomResponse(String roomId,
                                 int minPlayers,
                                 int maxPlayers,
                                 String status,
                                 String wsEndpoint) {
}
