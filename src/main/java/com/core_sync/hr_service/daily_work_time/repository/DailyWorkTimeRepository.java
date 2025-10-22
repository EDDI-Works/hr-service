package com.core_sync.hr_service.daily_work_time.repository;

import com.core_sync.hr_service.daily_work_time.entity.DailyWorkTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyWorkTimeRepository extends JpaRepository<DailyWorkTime, Long> {
    
    Optional<DailyWorkTime> findByAccountIdAndDate(Long accountId, LocalDate date);
    
    List<DailyWorkTime> findByAccountIdAndDateBetweenOrderByDateAsc(Long accountId, LocalDate startDate, LocalDate endDate);
}
