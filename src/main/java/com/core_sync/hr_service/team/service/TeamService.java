package com.core_sync.hr_service.team.service;

import com.core_sync.hr_service.team.service.request.*;
import com.core_sync.hr_service.team.service.response.*;

import java.util.List;

public interface TeamService {
    TeamCreateResponse createTeam(TeamCreateRequest request);
    TeamResponse getTeam(Long teamId);
    List<TeamResponse> getTeamsByAccountId(Long accountId);
    TeamResponse updateTeam(Long teamId, TeamUpdateRequest request, Long accountId);
    void deleteTeam(Long teamId, Long accountId);
    void inviteMember(Long teamId, Long accountId, Long inviterId);
    void removeMember(Long teamId, Long memberId, Long leaderId);
    List<TeamMemberResponse> getTeamMembers(Long teamId);
    boolean isTeamLeader(Long teamId, Long accountId);
    void validateTeamMember(Long teamId, Long accountId);
}
