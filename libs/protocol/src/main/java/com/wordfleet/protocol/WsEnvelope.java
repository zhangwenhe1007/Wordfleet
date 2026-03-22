package com.wordfleet.protocol;

import java.util.Map;

public record WsEnvelope(String type, Map<String, Object> payload) {
}
