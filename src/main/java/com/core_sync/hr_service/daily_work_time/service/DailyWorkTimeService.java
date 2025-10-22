package com.core_sync.hr_service.daily_work_time.service;

import com.core_sync.hr_service.daily_work_time.entity.DailyWorkTime;

import java.time.LocalDate;
import java.util.List;

public interface DailyWorkTimeService {
    
    // 오늘의 총 작업 시간 조회
    DailyWorkTime getTodayWorkTime(Long accountId);
    
    // 특정 날짜의 총 작업 시간 조회
    DailyWorkTime getWorkTimeByDate(Long accountId, LocalDate date);
    
    // 작업 시간 추가
    void addWorkTime(Long accountId, LocalDate date, int seconds);
    
    // 주간 작업 시간 조회 (최근 7일)
    List<DailyWorkTime> getWeekWorkTime(Long accountId);
}
