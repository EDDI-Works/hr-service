package com.core_sync.hr_service.team.service.response;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamCreateResponse {
    private Long teamId;
    private String name;
}
