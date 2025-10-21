package com.core_sync.hr_service.work_session.repository;

import com.core_sync.hr_service.work_session.entity.WorkSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkSessionRepository extends JpaRepository<WorkSession, Long> {
    
    List<WorkSession> findByAttendanceIdOrderByStartTimeAsc(Long attendanceId);
    
    @Query("SELECT ws FROM WorkSession ws WHERE ws.attendance.id = :attendanceId AND ws.endTime IS NULL")
    Optional<WorkSession> findActiveSession(@Param("attendanceId") Long attendanceId);
    
    void deleteByAttendanceId(Long attendanceId);
}
