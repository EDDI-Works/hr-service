package com.core_sync.hr_service.team.service.request;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamCreateRequest {
    private String name;
    private Long leaderId;
    private Integer annualLeaveCount;
    private String description;
}
