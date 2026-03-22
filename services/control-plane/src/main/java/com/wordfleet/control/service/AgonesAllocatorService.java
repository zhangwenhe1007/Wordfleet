package com.wordfleet.control.service;

import com.wordfleet.control.config.WordfleetProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class AgonesAllocatorService {
    private final WebClient webClient;
    private final WordfleetProperties props;

    public AgonesAllocatorService(WebClient webClient, WordfleetProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    public String allocateWsEndpoint(String roomId) {
        String allocatorUrl = props.getAgonesAllocatorUrl();
        if (allocatorUrl == null || allocatorUrl.isBlank()) {
            return props.getSessionWsFallback() + "/" + roomId;
        }

        Map<String, Object> request = Map.of(
                "namespace", "wordfleet",
                "gameServerSelectors", List.of(Map.of("matchLabels", Map.of("agones.dev/fleet", "wordfleet-session"))));

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(allocatorUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                return props.getSessionWsFallback() + "/" + roomId;
            }

            String address = (String) response.getOrDefault("address", "");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ports = (List<Map<String, Object>>) response.get("ports");
            if (address.isBlank() || ports == null || ports.isEmpty()) {
                return props.getSessionWsFallback() + "/" + roomId;
            }

            int port = ((Number) ports.getFirst().get("port")).intValue();
            return "ws://" + address + ":" + port + "/ws/room/" + roomId;
        } catch (Exception ex) {
            return props.getSessionWsFallback() + "/" + roomId;
        }
    }
}
