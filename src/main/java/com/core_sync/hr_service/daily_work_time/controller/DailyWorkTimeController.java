package com.core_sync.hr_service.daily_work_time.controller;

import com.core_sync.hr_service.daily_work_time.entity.DailyWorkTime;
import com.core_sync.hr_service.daily_work_time.service.DailyWorkTimeService;
import com.core_sync.hr_service.redis_cache.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/daily-work-time")
@RequiredArgsConstructor
public class DailyWorkTimeController {
    
    private final DailyWorkTimeService dailyWorkTimeService;
    private final RedisCacheService redisCacheService;
    
    // 오늘의 총 작업 시간 조회
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayWorkTime(
            @RequestHeader("Authorization") String token
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        DailyWorkTime dailyWorkTime = dailyWorkTimeService.getTodayWorkTime(accountId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("accountId", dailyWorkTime.getAccountId());
        response.put("date", dailyWorkTime.getDate().toString());
        response.put("totalSeconds", dailyWorkTime.getTotalSeconds());
        response.put("totalHours", dailyWorkTime.getTotalHours());
        
        return ResponseEntity.ok(response);
    }
    
    // 주간 작업 시간 조회
    @GetMapping("/week")
    public ResponseEntity<List<Map<String, Object>>> getWeekWorkTime(
            @RequestHeader("Authorization") String token
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        List<DailyWorkTime> weekData = dailyWorkTimeService.getWeekWorkTime(accountId);
        
        List<Map<String, Object>> response = weekData.stream()
                .map(data -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", data.getDate().toString());
                    map.put("totalSeconds", data.getTotalSeconds());
                    map.put("totalHours", data.getTotalHours());
                    return map;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
}
