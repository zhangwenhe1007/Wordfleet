package com.wordfleet.protocol;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

public final class HmacTokenUtil {
    private HmacTokenUtil() {
    }

    public static String issueJoinToken(String roomId, String userId, long ttlSeconds, String secret) {
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = "room=" + safe(roomId) + "&user=" + safe(userId) + "&exp=" + exp;
        String payloadB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String sig = sign(payloadB64, secret);
        return payloadB64 + "." + sig;
    }

    public static Optional<TokenClaims> verifyJoinToken(String token, String secret) {
        if (token == null || token.isBlank() || !token.contains(".")) {
            return Optional.empty();
        }

        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }

        String payloadB64 = parts[0];
        String providedSig = parts[1];
        String expectedSig = sign(payloadB64, secret);

        if (!MessageDigest.isEqual(providedSig.getBytes(StandardCharsets.UTF_8),
                expectedSig.getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }

        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }

        String roomId = null;
        String userId = null;
        long exp = 0;

        String[] pairs = payload.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            switch (kv[0]) {
                case "room" -> roomId = kv[1];
                case "user" -> userId = kv[1];
                case "exp" -> {
                    try {
                        exp = Long.parseLong(kv[1]);
                    } catch (NumberFormatException ignored) {
                        return Optional.empty();
                    }
                }
                default -> {
                }
            }
        }

        if (roomId == null || userId == null || exp == 0) {
            return Optional.empty();
        }

        if (Instant.now().getEpochSecond() > exp) {
            return Optional.empty();
        }

        return Optional.of(new TokenClaims(roomId, userId, exp));
    }

    private static String sign(String payloadB64, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payloadB64.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign token", ex);
        }
    }

    private static String safe(String value) {
        return value.replace("&", "").replace("=", "");
    }
}
