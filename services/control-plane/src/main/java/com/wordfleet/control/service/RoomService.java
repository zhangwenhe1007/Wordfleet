package com.wordfleet.control.service;

import com.wordfleet.control.config.WordfleetProperties;
import com.wordfleet.control.domain.CreateRoomRequest;
import com.wordfleet.control.domain.CreateRoomResponse;
import com.wordfleet.control.domain.JoinRoomResponse;
import com.wordfleet.control.domain.RoomRecord;
import com.wordfleet.control.domain.RoomView;
import com.wordfleet.protocol.HmacTokenUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {
    private static final long JOIN_TOKEN_TTL_SECONDS = 3600;

    private final AgonesAllocatorService allocatorService;
    private final WordfleetProperties props;
    private final ConcurrentHashMap<String, RoomRecord> rooms = new ConcurrentHashMap<>();

    public RoomService(AgonesAllocatorService allocatorService, WordfleetProperties props) {
        this.allocatorService = allocatorService;
        this.props = props;
    }

    public CreateRoomResponse createRoom(String ownerUserId, CreateRoomRequest request) {
        if (request.maxPlayers() < request.minPlayers()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxPlayers must be >= minPlayers");
        }

        String roomId = UUID.randomUUID().toString().substring(0, 8);
        String wsEndpoint = allocatorService.allocateWsEndpoint(roomId);
        RoomRecord record = new RoomRecord(roomId, ownerUserId, request.minPlayers(), request.maxPlayers(),
                Instant.now().toEpochMilli(), wsEndpoint);
        rooms.put(roomId, record);
        return new CreateRoomResponse(roomId, request.minPlayers(), request.maxPlayers(), record.getStatus(), wsEndpoint);
    }

    public JoinRoomResponse joinRoom(String roomId, String userId) {
        RoomRecord room = rooms.get(roomId);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }

        synchronized (room) {
            if (room.getPlayers().size() >= room.getMaxPlayers() && !room.getPlayers().contains(userId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is full");
            }

            room.getPlayers().add(userId);
            if (room.getPlayers().size() >= room.getMinPlayers() && room.getStatus().equals("WAITING")) {
                room.setStatus("READY");
            }
        }

        long expiresAt = Instant.now().getEpochSecond() + JOIN_TOKEN_TTL_SECONDS;
        String joinToken = HmacTokenUtil.issueJoinToken(roomId, userId, JOIN_TOKEN_TTL_SECONDS, props.getJoinTokenSecret());
        return new JoinRoomResponse(roomId, userId, room.getWsEndpoint(), joinToken, expiresAt);
    }

    public RoomView getRoom(String roomId) {
        RoomRecord room = rooms.get(roomId);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        Set<String> players = Set.copyOf(room.getPlayers());
        return new RoomView(room.getRoomId(), room.getStatus(), room.getMinPlayers(), room.getMaxPlayers(),
                players, room.getCreatedAtEpochMillis(), room.getWsEndpoint());
    }

    public Optional<RoomRecord> findRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }
}
