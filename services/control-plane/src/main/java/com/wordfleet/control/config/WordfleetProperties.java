package com.wordfleet.control.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wordfleet")
public class WordfleetProperties {
    private String joinTokenSecret;
    private String sessionCallbackSecret;
    private String agonesAllocatorUrl;
    private String sessionWsFallback;
    private MagicLink magicLink = new MagicLink();

    public String getJoinTokenSecret() {
        return joinTokenSecret;
    }

    public void setJoinTokenSecret(String joinTokenSecret) {
        this.joinTokenSecret = joinTokenSecret;
    }

    public String getSessionCallbackSecret() {
        return sessionCallbackSecret;
    }

    public void setSessionCallbackSecret(String sessionCallbackSecret) {
        this.sessionCallbackSecret = sessionCallbackSecret;
    }

    public String getAgonesAllocatorUrl() {
        return agonesAllocatorUrl;
    }

    public void setAgonesAllocatorUrl(String agonesAllocatorUrl) {
        this.agonesAllocatorUrl = agonesAllocatorUrl;
    }

    public String getSessionWsFallback() {
        return sessionWsFallback;
    }

    public void setSessionWsFallback(String sessionWsFallback) {
        this.sessionWsFallback = sessionWsFallback;
    }

    public MagicLink getMagicLink() {
        return magicLink;
    }

    public void setMagicLink(MagicLink magicLink) {
        this.magicLink = magicLink;
    }

    public static class MagicLink {
        private String from;
        private String baseUrl;
        private String sendgridApiKey;

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getSendgridApiKey() {
            return sendgridApiKey;
        }

        public void setSendgridApiKey(String sendgridApiKey) {
            this.sendgridApiKey = sendgridApiKey;
        }
    }
}
