package com.core_sync.hr_service.dashboard.controller;

import com.core_sync.hr_service.redis_cache.service.RedisCacheService;
import com.core_sync.hr_service.team.service.TeamService;
import com.core_sync.hr_service.team.service.response.TeamResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final TeamService teamService;
    private final RedisCacheService redisCacheService;
    
    // 사용자의 팀 통계 조회
    @GetMapping("/team-stats")
    public ResponseEntity<Map<String, Object>> getTeamStats(
            @RequestHeader("Authorization") String token
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        // 사용자가 속한 팀 목록 조회
        List<TeamResponse> teams = teamService.getTeamsByAccountId(accountId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("teamCount", teams.size());
        
        log.info("팀 통계 조회 - accountId: {}, teamCount: {}", accountId, teams.size());
        
        return ResponseEntity.ok(response);
    }
}
