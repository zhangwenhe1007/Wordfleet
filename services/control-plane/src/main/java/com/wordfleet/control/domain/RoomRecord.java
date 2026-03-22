package com.wordfleet.control.domain;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RoomRecord {
    private final String roomId;
    private final String ownerUserId;
    private final int minPlayers;
    private final int maxPlayers;
    private final long createdAtEpochMillis;
    private final String wsEndpoint;
    private final Set<String> players;
    private volatile String status;

    public RoomRecord(String roomId,
                      String ownerUserId,
                      int minPlayers,
                      int maxPlayers,
                      long createdAtEpochMillis,
                      String wsEndpoint) {
        this.roomId = roomId;
        this.ownerUserId = ownerUserId;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.createdAtEpochMillis = createdAtEpochMillis;
        this.wsEndpoint = wsEndpoint;
        this.players = ConcurrentHashMap.newKeySet();
        this.players.add(ownerUserId);
        this.status = "WAITING";
    }

    public String getRoomId() {
        return roomId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public long getCreatedAtEpochMillis() {
        return createdAtEpochMillis;
    }

    public String getWsEndpoint() {
        return wsEndpoint;
    }

    public Set<String> getPlayers() {
        return players;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
