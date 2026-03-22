package com.wordfleet.session.service;

import com.wordfleet.session.config.WordfleetSessionProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AgonesSdkService {
    private static final Logger log = LoggerFactory.getLogger(AgonesSdkService.class);

    private final WebClient webClient;
    private final WordfleetSessionProperties props;

    public AgonesSdkService(WebClient webClient, WordfleetSessionProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @PostConstruct
    public void ready() {
        if (!props.isAgonesSdkEnabled()) {
            return;
        }
        try {
            webClient.post()
                    .uri(props.getAgonesSdkUrl() + "/ready")
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Agones SDK ready signal sent");
        } catch (Exception ex) {
            log.warn("Failed to signal Agones ready: {}", ex.getMessage());
        }
    }

    @Scheduled(fixedDelay = 2000)
    public void heartbeat() {
        if (!props.isAgonesSdkEnabled()) {
            return;
        }
        try {
            webClient.post()
                    .uri(props.getAgonesSdkUrl() + "/health")
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            log.debug("Agones heartbeat failed: {}", ex.getMessage());
        }
    }
}
