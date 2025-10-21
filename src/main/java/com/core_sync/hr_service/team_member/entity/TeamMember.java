package com.core_sync.hr_service.team_member.entity;

import com.core_sync.hr_service.team.entity.Team;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    
    @Column(nullable = false)
    private Long accountId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamRole role; // LEADER, MEMBER
    
    public enum TeamRole {
        LEADER,  // 팀장
        MEMBER   // 일반 멤버
    }
}
