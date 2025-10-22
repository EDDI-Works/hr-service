package com.core_sync.hr_service.attendance.service;

import com.core_sync.hr_service.attendance.entity.*;
import com.core_sync.hr_service.work_session.service.WorkSessionService;
import com.core_sync.hr_service.annual_leave.entity.AnnualLeave;
import com.core_sync.hr_service.annual_leave.entity.LeaveStatus;
import com.core_sync.hr_service.annual_leave.repository.AnnualLeaveRepository;
import com.core_sync.hr_service.attendance.repository.AttendanceRepository;
import com.core_sync.hr_service.daily_work_time.service.DailyWorkTimeService;
import com.core_sync.hr_service.team.entity.Team;
import com.core_sync.hr_service.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    
    private final AttendanceRepository attendanceRepository;
    private final AnnualLeaveRepository annualLeaveRepository;
    private final TeamRepository teamRepository;
    private final WorkSessionService workSessionService;
    private final DailyWorkTimeService dailyWorkTimeService;
    
    @Override
    @Transactional
    public Attendance checkIn(Long teamId, Long accountId, LocalTime checkInTime) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        
        LocalDate today = LocalDate.now();
        
        // 오늘 근태 기록 조회 또는 생성
        Attendance attendance = attendanceRepository
                .findByTeamIdAndAccountIdAndDate(teamId, accountId, today)
                .orElseGet(() -> {
                    Attendance newAttendance = new Attendance(team, accountId, today, AttendanceStatus.PRESENT);
                    return attendanceRepository.save(newAttendance);
                });
        
        // 첫 출근 시간 기록 (checkIn이 null인 경우에만)
        if (attendance.getCheckIn() == null) {
            attendance.setCheckIn(checkInTime);
        }
        attendance.setStatus(AttendanceStatus.PRESENT);
        Attendance savedAttendance = attendanceRepository.save(attendance);
        
        // 새로운 작업 세션 시작 (WorkSessionService 사용)
        workSessionService.startSession(savedAttendance.getId(), checkInTime);
        
        log.info("일 시작 - attendanceId: {}, time: {}", savedAttendance.getId(), checkInTime);
        
        return savedAttendance;
    }
    
    @Override
    @Transactional
    public Attendance checkOut(Long teamId, Long accountId, LocalTime checkOutTime) {
        LocalDate today = LocalDate.now();
        
        log.info("일 종료 요청 - teamId: {}, accountId: {}, checkOutTime: {}", teamId, accountId, checkOutTime);
        
        Attendance attendance = attendanceRepository
                .findByTeamIdAndAccountIdAndDate(teamId, accountId, today)
                .orElseThrow(() -> new IllegalArgumentException("출근 기록이 없습니다."));
        
        log.info("Attendance 조회 - attendanceId: {}, checkIn: {}", attendance.getId(), attendance.getCheckIn());
        
        // 작업 세션 종료 (WorkSessionService 사용)
        int sessionSeconds = 0;
        try {
            workSessionService.endSession(attendance.getId(), checkOutTime);
            log.info("작업 세션 종료 성공");
            
            // 방금 종료한 세션의 시간 계산
            sessionSeconds = workSessionService.calculateTotalSeconds(attendance.getId()) - 
                            (int)(attendance.getWorkHours() != null ? attendance.getWorkHours() * 3600 : 0);
        } catch (Exception e) {
            log.error("작업 세션 종료 실패: {}", e.getMessage(), e);
        }
        
        // 마지막 퇴근 시간 업데이트
        attendance.setCheckOut(checkOutTime);
        
        // 전체 작업 시간 계산 (WorkSessionService 사용) - 초 단위
        int totalSeconds = workSessionService.calculateTotalSeconds(attendance.getId());
        double totalHours = totalSeconds / 3600.0;
        attendance.setWorkHours(totalHours);
        
        // 기준 작업시간 체크하여 출근 상태 결정
        Team team = attendance.getTeam();
        if (team.getMinimumWorkHours() != null && team.getMinimumWorkHours() > 0) {
            if (totalHours >= team.getMinimumWorkHours()) {
                attendance.setStatus(AttendanceStatus.PRESENT);
                log.info("기준 작업시간 충족 - totalHours: {}, minimumWorkHours: {}", 
                        totalHours, team.getMinimumWorkHours());
            } else {
                attendance.setStatus(AttendanceStatus.ABSENT);
                log.info("기준 작업시간 미달 - totalHours: {}, minimumWorkHours: {}", 
                        totalHours, team.getMinimumWorkHours());
            }
        }
        
        Attendance savedAttendance = attendanceRepository.save(attendance);
        
        // 일일 총 작업 시간 업데이트 (팀 상관없이 회원별 누적)
        if (sessionSeconds > 0) {
            dailyWorkTimeService.addWorkTime(accountId, today, sessionSeconds);
        }
        
        log.info("일 종료 완료 - attendanceId: {}, time: {}, totalHours: {}, sessionSeconds: {}", 
                savedAttendance.getId(), checkOutTime, savedAttendance.getWorkHours(), sessionSeconds);
        
        return savedAttendance;
    }
    
    @Override
    public List<Attendance> getAttendanceList(Long teamId, Long accountId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        return attendanceRepository.findByTeamIdAndAccountIdAndDateBetween(
                teamId, accountId, startDate, endDate
        );
    }
    
    @Override
    public Attendance getTodayAttendance(Long teamId, Long accountId) {
        LocalDate today = LocalDate.now();
        return attendanceRepository
                .findByTeamIdAndAccountIdAndDate(teamId, accountId, today)
                .orElse(null);
    }
    
    @Override
    @Transactional
    public AnnualLeave applyLeave(Long teamId, Long accountId, LocalDate startDate, LocalDate endDate, String reason) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        
        // 일수 계산
        int days = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        AnnualLeave leave = new AnnualLeave(team, accountId, startDate, endDate, days, reason);
        return annualLeaveRepository.save(leave);
    }
    
    @Override
    public List<AnnualLeave> getLeaveList(Long teamId, Long accountId) {
        return annualLeaveRepository.findByTeamIdAndAccountIdOrderByCreatedAtDesc(teamId, accountId);
    }
    
    @Override
    @Transactional
    public AnnualLeave approveLeave(Long leaveId, Long approverId, boolean approved) {
        AnnualLeave leave = annualLeaveRepository.findById(leaveId)
                .orElseThrow(() -> new IllegalArgumentException("연차 신청을 찾을 수 없습니다."));
        
        leave.setStatus(approved ? LeaveStatus.APPROVED : LeaveStatus.REJECTED);
        leave.setApprovedBy(approverId);
        leave.setApprovedAt(java.time.LocalDateTime.now());
        
        return annualLeaveRepository.save(leave);
    }
    
    @Override
    public Map<String, Object> getLeaveStats(Long teamId, Long accountId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        
        Integer usedDays = annualLeaveRepository.sumDaysByTeamIdAndAccountIdAndStatus(
                teamId, accountId, LeaveStatus.APPROVED
        );
        
        if (usedDays == null) {
            usedDays = 0;
        }
        
        int totalDays = team.getAnnualLeaveCount();
        int remainingDays = totalDays - usedDays;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDays", totalDays);
        stats.put("usedDays", usedDays);
        stats.put("remainingDays", remainingDays);
        
        return stats;
    }
    
    @Override
    public boolean hasActiveSession(Long attendanceId) {
        return workSessionService.hasActiveSession(attendanceId);
    }
    
    @Override
    public List<AnnualLeave> getTeamLeaveList(Long teamId) {
        return annualLeaveRepository.findByTeamIdOrderByCreatedAtDesc(teamId);
    }
    
    @Override
    public List<Attendance> getMemberAttendanceList(Long teamId, Long accountId, int year, int month) {
        return getAttendanceList(teamId, accountId, year, month);
    }
}
