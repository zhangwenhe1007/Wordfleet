package com.wordfleet.control.service;

import com.wordfleet.control.domain.AuthResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdentityService {
    private static final Duration SESSION_TTL = Duration.ofDays(7);
    private static final Duration MAGIC_LINK_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final SendGridEmailService sendGridEmailService;

    public IdentityService(StringRedisTemplate redis, SendGridEmailService sendGridEmailService) {
        this.redis = redis;
        this.sendGridEmailService = sendGridEmailService;
    }

    public AuthResponse issueGuestSession(String displayNameInput) {
        String userId = "guest_" + UUID.randomUUID().toString().substring(0, 12);
        String displayName = (displayNameInput == null || displayNameInput.isBlank())
                ? "Guest-" + userId.substring(userId.length() - 4)
                : displayNameInput;
        return issueSession(userId, displayName);
    }

    public void requestMagicLink(String email) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set("magic:token:" + token, email, MAGIC_LINK_TTL);
        sendGridEmailService.sendMagicLink(email, token);
    }

    public AuthResponse verifyMagicLink(String token, String displayNameInput) {
        String email = redis.opsForValue().get("magic:token:" + token);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired magic-link token");
        }
        redis.delete("magic:token:" + token);

        String userId = "user_" + UUID.nameUUIDFromBytes(email.getBytes()).toString().substring(0, 12);
        String displayName = (displayNameInput == null || displayNameInput.isBlank())
                ? email.split("@")[0]
                : displayNameInput;

        return issueSession(userId, displayName);
    }

    public String requireUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        String sessionToken = authorizationHeader.substring("Bearer ".length()).trim();
        if (sessionToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Empty bearer token");
        }
        String userId = redis.opsForValue().get("auth:session:" + sessionToken);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session token");
        }
        return userId;
    }

    public Optional<String> findDisplayName(String userId) {
        return Optional.ofNullable(redis.opsForValue().get("user:" + userId + ":name"));
    }

    private AuthResponse issueSession(String userId, String displayName) {
        String sessionToken = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set("auth:session:" + sessionToken, userId, SESSION_TTL);
        redis.opsForValue().set("user:" + userId + ":name", displayName, SESSION_TTL);
        return new AuthResponse(userId, displayName, sessionToken);
    }
}
