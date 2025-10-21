package com.core_sync.hr_service.team.controller.request;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TeamCreateRequestForm {
    private String name;
    private Integer annualLeaveCount;
    private String description;
}
