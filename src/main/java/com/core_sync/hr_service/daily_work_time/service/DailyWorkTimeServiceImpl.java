package com.core_sync.hr_service.daily_work_time.service;

import com.core_sync.hr_service.daily_work_time.entity.DailyWorkTime;
import com.core_sync.hr_service.daily_work_time.repository.DailyWorkTimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyWorkTimeServiceImpl implements DailyWorkTimeService {
    
    private final DailyWorkTimeRepository dailyWorkTimeRepository;
    
    @Override
    public DailyWorkTime getTodayWorkTime(Long accountId) {
        return getWorkTimeByDate(accountId, LocalDate.now());
    }
    
    @Override
    public DailyWorkTime getWorkTimeByDate(Long accountId, LocalDate date) {
        return dailyWorkTimeRepository.findByAccountIdAndDate(accountId, date)
                .orElseGet(() -> new DailyWorkTime(accountId, date));
    }
    
    @Override
    @Transactional
    public void addWorkTime(Long accountId, LocalDate date, int seconds) {
        DailyWorkTime dailyWorkTime = dailyWorkTimeRepository.findByAccountIdAndDate(accountId, date)
                .orElseGet(() -> {
                    DailyWorkTime newRecord = new DailyWorkTime(accountId, date);
                    return dailyWorkTimeRepository.save(newRecord);
                });
        
        dailyWorkTime.addSeconds(seconds);
        dailyWorkTimeRepository.save(dailyWorkTime);
        
        log.info("일일 작업 시간 추가 - accountId: {}, date: {}, addedSeconds: {}, totalSeconds: {}", 
                accountId, date, seconds, dailyWorkTime.getTotalSeconds());
    }
    
    @Override
    public List<DailyWorkTime> getWeekWorkTime(Long accountId) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);
        return dailyWorkTimeRepository.findByAccountIdAndDateBetweenOrderByDateAsc(accountId, weekAgo, today);
    }
}
