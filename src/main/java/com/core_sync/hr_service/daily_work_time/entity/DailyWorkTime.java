package com.core_sync.hr_service.daily_work_time.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "daily_work_time", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "date"}))
public class DailyWorkTime {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    @Column(name = "date", nullable = false)
    private LocalDate date;
    
    @Column(name = "total_seconds", nullable = false)
    private Integer totalSeconds = 0;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public DailyWorkTime(Long accountId, LocalDate date) {
        this.accountId = accountId;
        this.date = date;
        this.totalSeconds = 0;
    }
    
    public void addSeconds(int seconds) {
        this.totalSeconds += seconds;
    }
    
    public Double getTotalHours() {
        return totalSeconds / 3600.0;
    }
}
