package com.core_sync.hr_service.work_session.service;

import com.core_sync.hr_service.attendance.entity.Attendance;
import com.core_sync.hr_service.attendance.repository.AttendanceRepository;
import com.core_sync.hr_service.work_session.entity.WorkSession;
import com.core_sync.hr_service.work_session.repository.WorkSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkSessionServiceImpl implements WorkSessionService {
    
    private final WorkSessionRepository workSessionRepository;
    private final AttendanceRepository attendanceRepository;
    
    @Override
    @Transactional
    public WorkSession startSession(Long attendanceId, LocalTime startTime) {
        log.info("작업 세션 시작 요청 - attendanceId: {}, startTime: {}", attendanceId, startTime);
        
        // 이미 활성 세션이 있는지 확인
        Optional<WorkSession> activeSession = workSessionRepository.findActiveSession(attendanceId);
        if (activeSession.isPresent()) {
            log.warn("이미 진행 중인 작업 세션이 있습니다 - sessionId: {}", activeSession.get().getId());
            throw new IllegalStateException("이미 진행 중인 작업 세션이 있습니다.");
        }
        
        // Attendance 조회
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("근태 기록을 찾을 수 없습니다."));
        
        log.info("Attendance 조회 성공 - attendanceId: {}, teamId: {}, accountId: {}", 
                attendance.getId(), attendance.getTeam().getId(), attendance.getAccountId());
        
        // 새로운 작업 세션 생성
        WorkSession session = new WorkSession(attendance, startTime);
        
        WorkSession saved = workSessionRepository.save(session);
        log.info("작업 세션 저장 완료 - sessionId: {}, attendanceId: {}, startTime: {}", 
                saved.getId(), attendanceId, startTime);
        
        return saved;
    }
    
    @Override
    @Transactional
    public WorkSession endSession(Long attendanceId, LocalTime endTime) {
        log.info("작업 세션 종료 요청 - attendanceId: {}, endTime: {}", attendanceId, endTime);
        
        WorkSession session = workSessionRepository.findActiveSession(attendanceId)
                .orElseThrow(() -> {
                    log.error("진행 중인 작업 세션을 찾을 수 없습니다 - attendanceId: {}", attendanceId);
                    return new IllegalStateException("진행 중인 작업 세션이 없습니다.");
                });
        
        log.info("활성 세션 찾음 - sessionId: {}, startTime: {}", session.getId(), session.getStartTime());
        
        session.endSession(endTime);
        WorkSession saved = workSessionRepository.save(session);
        
        log.info("작업 세션 종료 완료 - sessionId: {}, attendanceId: {}, endTime: {}, duration: {}분", 
                saved.getId(), attendanceId, endTime, saved.getDurationSeconds());
        
        return saved;
    }
    
    @Override
    public WorkSession getActiveSession(Long attendanceId) {
        return workSessionRepository.findActiveSession(attendanceId)
                .orElse(null);
    }
    
    @Override
    public List<WorkSession> getAllSessions(Long attendanceId) {
        return workSessionRepository.findByAttendanceIdOrderByStartTimeAsc(attendanceId);
    }
    
    @Override
    public int calculateTotalSeconds(Long attendanceId) {
        List<WorkSession> sessions = getAllSessions(attendanceId);
        log.info("총 작업 시간 계산 - attendanceId: {}, 세션 수: {}", attendanceId, sessions.size());
        
        int totalSeconds = sessions.stream()
                .filter(s -> s.getDurationSeconds() != null)
                .mapToInt(WorkSession::getDurationSeconds)
                .sum();
        
        log.info("총 작업 시간 - attendanceId: {}, totalSeconds: {}", attendanceId, totalSeconds);
        return totalSeconds;
    }
    
    @Override
    public boolean hasActiveSession(Long attendanceId) {
        return workSessionRepository.findActiveSession(attendanceId).isPresent();
    }
    
    @Override
    public LocalTime getCurrentSessionStartTime(Long attendanceId) {
        return workSessionRepository.findActiveSession(attendanceId)
                .map(WorkSession::getStartTime)
                .orElse(null);
    }
}
