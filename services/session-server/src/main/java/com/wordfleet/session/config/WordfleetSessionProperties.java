package com.wordfleet.session.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wordfleet")
public class WordfleetSessionProperties {
    private String joinTokenSecret;
    private String controlBaseUrl;
    private String sessionCallbackSecret;
    private long roomTtlSeconds;
    private boolean agonesSdkEnabled;
    private String agonesSdkUrl;

    public String getJoinTokenSecret() {
        return joinTokenSecret;
    }

    public void setJoinTokenSecret(String joinTokenSecret) {
        this.joinTokenSecret = joinTokenSecret;
    }

    public String getControlBaseUrl() {
        return controlBaseUrl;
    }

    public void setControlBaseUrl(String controlBaseUrl) {
        this.controlBaseUrl = controlBaseUrl;
    }

    public String getSessionCallbackSecret() {
        return sessionCallbackSecret;
    }

    public void setSessionCallbackSecret(String sessionCallbackSecret) {
        this.sessionCallbackSecret = sessionCallbackSecret;
    }

    public long getRoomTtlSeconds() {
        return roomTtlSeconds;
    }

    public void setRoomTtlSeconds(long roomTtlSeconds) {
        this.roomTtlSeconds = roomTtlSeconds;
    }

    public boolean isAgonesSdkEnabled() {
        return agonesSdkEnabled;
    }

    public void setAgonesSdkEnabled(boolean agonesSdkEnabled) {
        this.agonesSdkEnabled = agonesSdkEnabled;
    }

    public String getAgonesSdkUrl() {
        return agonesSdkUrl;
    }

    public void setAgonesSdkUrl(String agonesSdkUrl) {
        this.agonesSdkUrl = agonesSdkUrl;
    }
}
