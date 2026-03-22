package com.wordfleet.control.service;

import com.wordfleet.control.config.WordfleetProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class SendGridEmailService {
    private static final Logger log = LoggerFactory.getLogger(SendGridEmailService.class);

    private final WebClient webClient;
    private final WordfleetProperties props;

    public SendGridEmailService(WebClient webClient, WordfleetProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    public void sendMagicLink(String email, String token) {
        String apiKey = props.getMagicLink().getSendgridApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.info("SENDGRID_API_KEY not set. Skipping outbound magic-link email for {}", email);
            return;
        }

        String link = props.getMagicLink().getBaseUrl() + "?token=" + token;
        Map<String, Object> body = Map.of(
                "personalizations", List.of(Map.of("to", List.of(Map.of("email", email)))),
                "from", Map.of("email", props.getMagicLink().getFrom()),
                "subject", "Your Wordfleet login link",
                "content", List.of(Map.of("type", "text/plain", "value", "Use this link to log in: " + link))
        );

        try {
            webClient.post()
                    .uri("https://api.sendgrid.com/v3/mail/send")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            log.warn("Failed to send magic-link email to {}: {}", email, ex.getMessage());
        }
    }
}
