package com.wordfleet.control.api;

import com.wordfleet.control.domain.MatchSummaryRequest;
import com.wordfleet.control.domain.MatchSummaryView;
import com.wordfleet.control.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/matches")
public class MatchController {
    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping("/{roomId}/complete")
    public MatchSummaryView complete(@PathVariable String roomId,
                                     @RequestHeader("X-Session-Signature") String signature,
                                     @Valid @RequestBody MatchSummaryRequest request) {
        return matchService.complete(roomId, request, signature);
    }

    @GetMapping("/{roomId}")
    public MatchSummaryView byRoom(@PathVariable String roomId) {
        return matchService.findByRoomId(roomId);
    }
}
