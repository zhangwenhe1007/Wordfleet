package com.wordfleet.session.config;

import com.wordfleet.session.ws.GameHandshakeInterceptor;
import com.wordfleet.session.ws.GameWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final GameWebSocketHandler gameWebSocketHandler;
    private final GameHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(GameWebSocketHandler gameWebSocketHandler,
                           GameHandshakeInterceptor handshakeInterceptor) {
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/ws/room/*")
                .setAllowedOriginPatterns("*")
                .addInterceptors(handshakeInterceptor);
    }
}
