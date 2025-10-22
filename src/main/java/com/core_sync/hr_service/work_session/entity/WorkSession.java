package com.core_sync.hr_service.work_session.entity;

import com.core_sync.hr_service.attendance.entity.Attendance;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "work_session")
public class WorkSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", nullable = false)
    private Attendance attendance;
    
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    
    @Column(name = "end_time")
    private LocalTime endTime;
    
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    public WorkSession(Attendance attendance, LocalTime startTime) {
        this.attendance = attendance;
        this.startTime = startTime;
    }
    
    public void endSession(LocalTime endTime) {
        this.endTime = endTime;
        if (startTime != null && endTime != null) {
            long seconds = java.time.Duration.between(startTime, endTime).toSeconds();
            this.durationSeconds = (int) seconds;
        }
    }
}
