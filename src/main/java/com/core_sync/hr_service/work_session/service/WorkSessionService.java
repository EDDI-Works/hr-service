package com.core_sync.hr_service.work_session.service;

import com.core_sync.hr_service.work_session.entity.WorkSession;

import java.time.LocalTime;
import java.util.List;

public interface WorkSessionService {
    
    // 작업 세션 시작
    WorkSession startSession(Long attendanceId, LocalTime startTime);
    
    // 작업 세션 종료
    WorkSession endSession(Long attendanceId, LocalTime endTime);
    
    // 활성 세션 조회
    WorkSession getActiveSession(Long attendanceId);
    
    // 모든 세션 조회
    List<WorkSession> getAllSessions(Long attendanceId);
    
    // 총 작업 시간 계산 (초 단위)
    int calculateTotalSeconds(Long attendanceId);
    
    // 활성 세션 여부 확인
    boolean hasActiveSession(Long attendanceId);
    
    // 현재 활성 세션의 시작 시간 조회
    LocalTime getCurrentSessionStartTime(Long attendanceId);
}
