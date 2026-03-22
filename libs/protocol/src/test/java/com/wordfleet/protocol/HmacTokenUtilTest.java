package com.wordfleet.protocol;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HmacTokenUtilTest {

    @Test
    void issueAndVerifyJoinToken() {
        String secret = "test-secret";
        String token = HmacTokenUtil.issueJoinToken("room123", "user456", 300, secret);

        Optional<TokenClaims> claims = HmacTokenUtil.verifyJoinToken(token, secret);

        assertTrue(claims.isPresent());
        assertEquals("room123", claims.get().roomId());
        assertEquals("user456", claims.get().userId());
    }
}
