package com.core_sync.hr_service.attendance.controller;

import com.core_sync.hr_service.attendance.controller.response.AttendanceResponse;
import com.core_sync.hr_service.attendance.entity.Attendance;
import com.core_sync.hr_service.annual_leave.entity.AnnualLeave;
import com.core_sync.hr_service.attendance.service.AttendanceService;
import com.core_sync.hr_service.redis_cache.service.RedisCacheService;
import com.core_sync.hr_service.work_session.service.WorkSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    
    private final AttendanceService attendanceService;
    private final RedisCacheService redisCacheService;
    private final WorkSessionService workSessionService;
    
    // 일 시작 (출근)
    @PostMapping("/check-in")
    public ResponseEntity<AttendanceResponse> checkIn(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        Long teamId = ((Number) request.get("teamId")).longValue();
        LocalTime checkInTime = LocalTime.now();
        
        Attendance attendance = attendanceService.checkIn(teamId, accountId, checkInTime);
        boolean isWorking = attendanceService.hasActiveSession(attendance.getId());
        LocalTime currentSessionStart = workSessionService.getCurrentSessionStartTime(attendance.getId());
        String currentSessionStartStr = currentSessionStart != null ? currentSessionStart.toString() : null;
        return ResponseEntity.ok(AttendanceResponse.from(attendance, isWorking, currentSessionStartStr));
    }
    
    // 일 종료 (퇴근)
    @PostMapping("/check-out")
    public ResponseEntity<AttendanceResponse> checkOut(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        Long teamId = ((Number) request.get("teamId")).longValue();
        LocalTime checkOutTime = LocalTime.now();
        
        Attendance attendance = attendanceService.checkOut(teamId, accountId, checkOutTime);
        boolean isWorking = attendanceService.hasActiveSession(attendance.getId());
        LocalTime currentSessionStart = workSessionService.getCurrentSessionStartTime(attendance.getId());
        String currentSessionStartStr = currentSessionStart != null ? currentSessionStart.toString() : null;
        return ResponseEntity.ok(AttendanceResponse.from(attendance, isWorking, currentSessionStartStr));
    }
    
    // 근태 목록 조회
    @GetMapping("/list")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceList(
            @RequestHeader("Authorization") String token,
            @RequestParam Long teamId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        List<Attendance> attendanceList = attendanceService.getAttendanceList(teamId, accountId, year, month);
        List<AttendanceResponse> responseList = attendanceList.stream()
                .map(a -> {
                    boolean isWorking = attendanceService.hasActiveSession(a.getId());
                    LocalTime currentSessionStart = workSessionService.getCurrentSessionStartTime(a.getId());
                    String currentSessionStartStr = currentSessionStart != null ? currentSessionStart.toString() : null;
                    return AttendanceResponse.from(a, isWorking, currentSessionStartStr);
                })
                .toList();
        return ResponseEntity.ok(responseList);
    }
    
    // 오늘의 근태 조회
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayAttendance(
            @RequestHeader("Authorization") String token,
            @RequestParam Long teamId
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        Attendance attendance = attendanceService.getTodayAttendance(teamId, accountId);
        
        Map<String, Object> response = new HashMap<>();
        if (attendance != null) {
            response.put("id", attendance.getId());
            response.put("teamId", attendance.getTeam().getId());
            response.put("accountId", attendance.getAccountId());
            response.put("date", attendance.getDate().toString());
            response.put("checkIn", attendance.getCheckIn() != null ? attendance.getCheckIn().toString() : null);
            response.put("checkOut", attendance.getCheckOut() != null ? attendance.getCheckOut().toString() : null);
            response.put("status", attendance.getStatus().name());
            response.put("workHours", attendance.getWorkHours());
            response.put("isWorking", attendanceService.hasActiveSession(attendance.getId()));
        }
        
        return ResponseEntity.ok(response);
    }
    
    // 연차 신청
    @PostMapping("/leave/apply")
    public ResponseEntity<AnnualLeave> applyLeave(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        Long teamId = ((Number) request.get("teamId")).longValue();
        LocalDate startDate = LocalDate.parse((String) request.get("startDate"));
        LocalDate endDate = LocalDate.parse((String) request.get("endDate"));
        String reason = (String) request.get("reason");
        
        AnnualLeave leave = attendanceService.applyLeave(teamId, accountId, startDate, endDate, reason);
        return ResponseEntity.ok(leave);
    }
    
    // 연차 목록 조회
    @GetMapping("/leave/list")
    public ResponseEntity<List<AnnualLeave>> getLeaveList(
            @RequestHeader("Authorization") String token,
            @RequestParam Long teamId
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        List<AnnualLeave> leaveList = attendanceService.getLeaveList(teamId, accountId);
        return ResponseEntity.ok(leaveList);
    }
    
    // 연차 통계
    @GetMapping("/leave/stats")
    public ResponseEntity<Map<String, Object>> getLeaveStats(
            @RequestHeader("Authorization") String token,
            @RequestParam Long teamId
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        Map<String, Object> stats = attendanceService.getLeaveStats(teamId, accountId);
        return ResponseEntity.ok(stats);
    }
    
    // 연차 승인/거절 (팀장만 가능)
    @PostMapping("/leave/{leaveId}/approve")
    public ResponseEntity<AnnualLeave> approveLeave(
            @RequestHeader("Authorization") String token,
            @PathVariable Long leaveId,
            @RequestBody Map<String, Object> request
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        boolean approved = (boolean) request.get("approved");
        
        AnnualLeave leave = attendanceService.approveLeave(leaveId, accountId, approved);
        return ResponseEntity.ok(leave);
    }
    
    // 팀장용: 팀 전체 연차 신청 목록 조회
    @GetMapping("/leave/team/{teamId}")
    public ResponseEntity<List<AnnualLeave>> getTeamLeaveList(
            @RequestHeader("Authorization") String token,
            @PathVariable Long teamId
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        List<AnnualLeave> leaveList = attendanceService.getTeamLeaveList(teamId);
        return ResponseEntity.ok(leaveList);
    }
    
    // 팀장용: 특정 팀원의 근태 목록 조회
    @GetMapping("/member/{accountId}/list")
    public ResponseEntity<List<AttendanceResponse>> getMemberAttendanceList(
            @RequestHeader("Authorization") String token,
            @PathVariable Long accountId,
            @RequestParam Long teamId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long requesterId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (requesterId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        List<Attendance> attendanceList = attendanceService.getMemberAttendanceList(teamId, accountId, year, month);
        List<AttendanceResponse> responseList = attendanceList.stream()
                .map(a -> {
                    boolean isWorking = attendanceService.hasActiveSession(a.getId());
                    LocalTime currentSessionStart = workSessionService.getCurrentSessionStartTime(a.getId());
                    String currentSessionStartStr = currentSessionStart != null ? currentSessionStart.toString() : null;
                    return AttendanceResponse.from(a, isWorking, currentSessionStartStr);
                })
                .toList();
        return ResponseEntity.ok(responseList);
    }
}
