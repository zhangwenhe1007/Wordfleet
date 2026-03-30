package com.wordfleet.control.api;

import com.wordfleet.control.domain.CreateRoomRequest;
import com.wordfleet.control.domain.CreateRoomResponse;
import com.wordfleet.control.domain.JoinRoomResponse;
import com.wordfleet.control.domain.RoomView;
import com.wordfleet.control.service.IdentityService;
import com.wordfleet.control.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/rooms")
public class RoomController {
    private final IdentityService identityService;
    private final RoomService roomService;

    public RoomController(IdentityService identityService, RoomService roomService) {
        this.identityService = identityService;
        this.roomService = roomService;
    }

    @PostMapping
    public CreateRoomResponse createRoom(@RequestHeader("Authorization") String authHeader,
                                         @Valid @RequestBody CreateRoomRequest request) {
        String userId = identityService.requireUserId(authHeader);
        return roomService.createRoom(userId, request);
    }

    @PostMapping("/{roomId}/join")
    public JoinRoomResponse joinRoom(@RequestHeader("Authorization") String authHeader,
                                     @PathVariable("roomId") String roomId) {
        String userId = identityService.requireUserId(authHeader);
        return roomService.joinRoom(roomId, userId);
    }

    @GetMapping("/{roomId}")
    public RoomView getRoom(@PathVariable("roomId") String roomId) {
        return roomService.getRoom(roomId);
    }
}
