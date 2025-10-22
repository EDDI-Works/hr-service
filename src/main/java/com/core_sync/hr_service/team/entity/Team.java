package com.core_sync.hr_service.team.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private Long leaderId; // 팀장 accountId
    
    @Column
    private Integer annualLeaveCount; // 연차 수
    
    @Column
    private Double minimumWorkHours; // 기준 작업시간 (시간 단위, 예: 8.0)
    
    @Column(length = 1000)
    private String description; // 팀 설명
}
