package com.wordfleet.protocol;

public record TokenClaims(String roomId, String userId, long expiresAtEpochSeconds) {
}
