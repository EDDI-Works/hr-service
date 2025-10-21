package com.core_sync.hr_service.annual_leave.repository;

import com.core_sync.hr_service.annual_leave.entity.AnnualLeave;
import com.core_sync.hr_service.annual_leave.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnnualLeaveRepository extends JpaRepository<AnnualLeave, Long> {
    
    List<AnnualLeave> findByTeamIdAndAccountIdOrderByCreatedAtDesc(Long teamId, Long accountId);
    
    List<AnnualLeave> findByTeamIdOrderByCreatedAtDesc(Long teamId);
    
    @Query("SELECT COALESCE(SUM(al.days), 0) FROM AnnualLeave al " +
           "WHERE al.team.id = :teamId AND al.accountId = :accountId AND al.status = :status")
    Integer sumDaysByTeamIdAndAccountIdAndStatus(
            @Param("teamId") Long teamId,
            @Param("accountId") Long accountId,
            @Param("status") LeaveStatus status
    );
}
