package com.wordfleet.control.api;

import com.wordfleet.control.domain.LeaderboardEntry;
import com.wordfleet.control.service.LeaderboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/leaderboards")
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/daily")
    public List<LeaderboardEntry> daily(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        return leaderboardService.topDaily(limit);
    }

    @GetMapping("/alltime")
    public List<LeaderboardEntry> alltime(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        return leaderboardService.topAllTime(limit);
    }
}
