package com.core_sync.hr_service.attendance.controller.response;

import com.core_sync.hr_service.attendance.entity.Attendance;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AttendanceResponse {
    private Long id;
    private Long teamId;
    private Long accountId;
    private String date;
    private String checkIn;
    private String checkOut;
    private String status;
    private Double workHours;
    private Boolean isWorking;
    private String currentSessionStart;  // 현재 세션 시작 시간
    
    public static AttendanceResponse from(Attendance attendance, boolean isWorking, String currentSessionStart) {
        return new AttendanceResponse(
            attendance.getId(),
            attendance.getTeam().getId(),
            attendance.getAccountId(),
            attendance.getDate().toString(),
            attendance.getCheckIn() != null ? attendance.getCheckIn().toString() : null,
            attendance.getCheckOut() != null ? attendance.getCheckOut().toString() : null,
            attendance.getStatus().name(),
            attendance.getWorkHours(),
            isWorking,
            currentSessionStart
        );
    }
}
