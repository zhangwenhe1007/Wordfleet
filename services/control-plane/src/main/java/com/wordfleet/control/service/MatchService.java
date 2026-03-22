package com.wordfleet.control.service;

import com.wordfleet.control.config.WordfleetProperties;
import com.wordfleet.control.domain.MatchSummaryRequest;
import com.wordfleet.control.domain.MatchSummaryView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MatchService {
    private final WordfleetProperties props;
    private final LeaderboardService leaderboardService;
    private final ConcurrentHashMap<String, MatchSummaryView> matchByRoom = new ConcurrentHashMap<>();

    public MatchService(WordfleetProperties props, LeaderboardService leaderboardService) {
        this.props = props;
        this.leaderboardService = leaderboardService;
    }

    public MatchSummaryView complete(String roomId, MatchSummaryRequest request, String signatureHeader) {
        if (!verifySignature(roomId, request, signatureHeader)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid callback signature");
        }

        MatchSummaryView summary = new MatchSummaryView(
                roomId,
                request.winnerUserId(),
                request.scores(),
                request.participants(),
                request.startedAtEpochMillis(),
                request.endedAtEpochMillis(),
                Instant.now().toEpochMilli());

        matchByRoom.put(roomId, summary);
        leaderboardService.applyMatchScores(request.scores(), request.endedAtEpochMillis());
        return summary;
    }

    public MatchSummaryView findByRoomId(String roomId) {
        MatchSummaryView summary = matchByRoom.get(roomId);
        if (summary == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match summary not found");
        }
        return summary;
    }

    private boolean verifySignature(String roomId, MatchSummaryRequest request, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        String canonical = roomId + "|" + request.winnerUserId() + "|" + request.endedAtEpochMillis();
        String expected = hmac(canonical, props.getSessionCallbackSecret());
        return expected.equals(signatureHeader);
    }

    private String hmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute callback signature", ex);
        }
    }
}
