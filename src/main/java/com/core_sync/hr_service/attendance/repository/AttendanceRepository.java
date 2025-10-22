package com.core_sync.hr_service.attendance.repository;

import com.core_sync.hr_service.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    Optional<Attendance> findByTeamIdAndAccountIdAndDate(Long teamId, Long accountId, LocalDate date);
    
    @Query("SELECT a FROM Attendance a WHERE a.team.id = :teamId AND a.accountId = :accountId " +
           "AND a.date BETWEEN :startDate AND :endDate ORDER BY a.date DESC")
    List<Attendance> findByTeamIdAndAccountIdAndDateBetween(
            @Param("teamId") Long teamId,
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT a FROM Attendance a WHERE a.team.id = :teamId " +
           "AND a.date BETWEEN :startDate AND :endDate ORDER BY a.date DESC")
    List<Attendance> findByTeamIdAndDateBetween(
            @Param("teamId") Long teamId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
