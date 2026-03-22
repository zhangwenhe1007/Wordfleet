package com.wordfleet.control.domain;

public record JoinRoomResponse(String roomId,
                               String userId,
                               String wsEndpoint,
                               String joinToken,
                               long expiresAtEpochSeconds) {
}
