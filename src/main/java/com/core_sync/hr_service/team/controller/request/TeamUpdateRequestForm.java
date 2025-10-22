package com.core_sync.hr_service.team.controller.request;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TeamUpdateRequestForm {
    private String name;
    private Integer annualLeaveCount;
    private Double minimumWorkHours;
    private String description;
}
