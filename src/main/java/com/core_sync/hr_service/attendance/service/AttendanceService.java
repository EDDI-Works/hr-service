package com.core_sync.hr_service.attendance.service;

import com.core_sync.hr_service.annual_leave.entity.AnnualLeave;
import com.core_sync.hr_service.attendance.entity.Attendance;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface AttendanceService {
    
    // 일 시작 (출근)
    Attendance checkIn(Long teamId, Long accountId, LocalTime checkInTime);
    
    // 일 종료 (퇴근)
    Attendance checkOut(Long teamId, Long accountId, LocalTime checkOutTime);
    
    // 특정 기간의 근태 조회
    List<Attendance> getAttendanceList(Long teamId, Long accountId, int year, int month);
    
    // 오늘의 근태 조회
    Attendance getTodayAttendance(Long teamId, Long accountId);
    
    // 연차 신청
    AnnualLeave applyLeave(Long teamId, Long accountId, LocalDate startDate, LocalDate endDate, String reason);
    
    // 연차 목록 조회
    List<AnnualLeave> getLeaveList(Long teamId, Long accountId);
    
    // 연차 승인/거절
    AnnualLeave approveLeave(Long leaveId, Long approverId, boolean approved);
    
    // 연차 통계
    Map<String, Object> getLeaveStats(Long teamId, Long accountId);
    
    // 활성 세션 여부 확인
    boolean hasActiveSession(Long attendanceId);
    
    // 팀장용: 팀 전체 연차 신청 목록 조회
    List<AnnualLeave> getTeamLeaveList(Long teamId);
    
    // 팀장용: 특정 팀원의 근태 목록 조회
    List<Attendance> getMemberAttendanceList(Long teamId, Long accountId, int year, int month);
}
