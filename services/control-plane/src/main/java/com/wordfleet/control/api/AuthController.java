package com.wordfleet.control.api;

import com.wordfleet.control.domain.AuthResponse;
import com.wordfleet.control.domain.GuestAuthRequest;
import com.wordfleet.control.domain.MagicLinkRequest;
import com.wordfleet.control.domain.MagicLinkVerifyRequest;
import com.wordfleet.control.domain.SimpleMessage;
import com.wordfleet.control.service.IdentityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {
    private final IdentityService identityService;

    public AuthController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping("/guest")
    public AuthResponse guest(@Valid @RequestBody(required = false) GuestAuthRequest request) {
        String displayName = request == null ? null : request.displayName();
        return identityService.issueGuestSession(displayName);
    }

    @PostMapping("/magic-link/request")
    public SimpleMessage requestMagicLink(@Valid @RequestBody MagicLinkRequest request) {
        identityService.requestMagicLink(request.email());
        return new SimpleMessage("If the email exists, a login link has been sent.");
    }

    @PostMapping("/magic-link/verify")
    public AuthResponse verifyMagicLink(@Valid @RequestBody MagicLinkVerifyRequest request) {
        return identityService.verifyMagicLink(request.token(), request.displayName());
    }

    @GetMapping("/me")
    public Map<String, String> me(@RequestHeader("Authorization") String authHeader) {
        String userId = identityService.requireUserId(authHeader);
        String displayName = identityService.findDisplayName(userId).orElse("unknown");
        return Map.of("userId", userId, "displayName", displayName);
    }
}
