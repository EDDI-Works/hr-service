package com.core_sync.hr_service.attendance.entity;

import com.core_sync.hr_service.team.entity.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "attendance", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "account_id", "date"}))
public class Attendance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(name = "check_in")
    private LocalTime checkIn;
    
    @Column(name = "check_out")
    private LocalTime checkOut;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;
    
    @Column(name = "work_hours")
    private Double workHours;
    
    @Column(length = 500)
    private String note;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public Attendance(Team team, Long accountId, LocalDate date, AttendanceStatus status) {
        this.team = team;
        this.accountId = accountId;
        this.date = date;
        this.status = status;
    }
    
    public void calculateWorkHours() {
        if (checkIn != null && checkOut != null) {
            long minutes = java.time.Duration.between(checkIn, checkOut).toMinutes();
            this.workHours = minutes / 60.0;
        }
    }
}
