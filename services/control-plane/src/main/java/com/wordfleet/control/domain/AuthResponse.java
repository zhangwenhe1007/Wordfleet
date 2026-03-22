package com.wordfleet.control.domain;

public record AuthResponse(String userId, String displayName, String sessionToken) {
}
