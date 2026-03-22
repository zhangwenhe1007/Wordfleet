package com.wordfleet.session.ws;

import com.wordfleet.protocol.HmacTokenUtil;
import com.wordfleet.protocol.TokenClaims;
import com.wordfleet.session.config.WordfleetSessionProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class GameHandshakeInterceptor implements HandshakeInterceptor {
    private final WordfleetSessionProperties props;

    public GameHandshakeInterceptor(WordfleetSessionProperties props) {
        this.props = props;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        URI uri = request.getURI();
        String path = uri.getPath();
        String roomId = path.substring(path.lastIndexOf('/') + 1);
        if (roomId.isBlank()) {
            return false;
        }

        Map<String, String> query = parseQuery(uri.getQuery());
        String token = query.get("token");
        Optional<TokenClaims> claimsOpt = HmacTokenUtil.verifyJoinToken(token, props.getJoinTokenSecret());
        if (claimsOpt.isEmpty()) {
            return false;
        }

        TokenClaims claims = claimsOpt.get();
        if (!roomId.equals(claims.roomId())) {
            return false;
        }

        attributes.put("roomId", roomId);
        attributes.put("userId", claims.userId());

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest raw = servletRequest.getServletRequest();
            attributes.put("remoteAddress", raw.getRemoteAddr());
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isBlank()) {
            return result;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                result.put(kv[0], kv[1]);
            }
        }
        return result;
    }
}
