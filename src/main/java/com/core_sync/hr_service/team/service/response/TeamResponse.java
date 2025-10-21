package com.core_sync.hr_service.team.service.response;

import com.core_sync.hr_service.team.entity.Team;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {
    private Long id;
    private String name;
    private Long leaderId;
    private Integer annualLeaveCount;
    private Double minimumWorkHours;
    private String description;
    private Integer memberCount;
    
    public static TeamResponse from(Team team, int memberCount) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .leaderId(team.getLeaderId())
                .annualLeaveCount(team.getAnnualLeaveCount())
                .minimumWorkHours(team.getMinimumWorkHours())
                .description(team.getDescription())
                .memberCount(memberCount)
                .build();
    }
}
