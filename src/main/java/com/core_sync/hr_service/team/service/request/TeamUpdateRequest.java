package com.core_sync.hr_service.team.service.request;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamUpdateRequest {
    private String name;
    private Integer annualLeaveCount;
    private Double minimumWorkHours;
    private String description;
}
