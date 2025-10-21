package com.core_sync.hr_service.team.service.response;

import com.core_sync.hr_service.team_member.entity.TeamMember;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberResponse {
    private Long id;
    private Long accountId;
    private String role;
    private String nickname;  // 닉네임 추가
    
    public static TeamMemberResponse from(TeamMember teamMember) {
        return TeamMemberResponse.builder()
                .id(teamMember.getId())
                .accountId(teamMember.getAccountId())
                .role(teamMember.getRole().name())
                .build();
    }
    
    public static TeamMemberResponse from(TeamMember teamMember, String nickname) {
        return TeamMemberResponse.builder()
                .id(teamMember.getId())
                .accountId(teamMember.getAccountId())
                .role(teamMember.getRole().name())
                .nickname(nickname)
                .build();
    }
}
