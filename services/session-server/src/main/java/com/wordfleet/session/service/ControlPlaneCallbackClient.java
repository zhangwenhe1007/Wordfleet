package com.wordfleet.session.service;

import com.wordfleet.session.config.WordfleetSessionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class ControlPlaneCallbackClient {
    private static final Logger log = LoggerFactory.getLogger(ControlPlaneCallbackClient.class);

    private final WebClient webClient;
    private final WordfleetSessionProperties props;

    public ControlPlaneCallbackClient(WebClient webClient, WordfleetSessionProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    public void sendMatchSummary(RoomContext room) {
        if (room.winnerUserId == null || room.endedAtEpochMillis == 0) {
            return;
        }

        String roomId = room.roomId;
        Map<String, Object> body = Map.of(
                "winnerUserId", room.winnerUserId,
                "scores", room.scores,
                "participants", List.copyOf(room.playerOrder),
                "startedAtEpochMillis", room.startedAtEpochMillis,
                "endedAtEpochMillis", room.endedAtEpochMillis
        );

        String canonical = roomId + "|" + room.winnerUserId + "|" + room.endedAtEpochMillis;
        String signature = sign(canonical, props.getSessionCallbackSecret());

        try {
            webClient.post()
                    .uri(props.getControlBaseUrl() + "/v1/matches/" + roomId + "/complete")
                    .header("X-Session-Signature", signature)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            log.warn("Failed to post match summary for room {}: {}", roomId, ex.getMessage());
        }
    }

    private String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign callback", ex);
        }
    }
}
