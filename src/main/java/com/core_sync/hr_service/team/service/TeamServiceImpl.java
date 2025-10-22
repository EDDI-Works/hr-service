package com.core_sync.hr_service.team.service;

import com.core_sync.hr_service.annual_leave.repository.AnnualLeaveRepository;
import com.core_sync.hr_service.attendance.repository.AttendanceRepository;
import com.core_sync.hr_service.team.entity.Team;
import com.core_sync.hr_service.team.repository.TeamRepository;
import com.core_sync.hr_service.team.service.request.*;
import com.core_sync.hr_service.team.service.response.*;
import com.core_sync.hr_service.team_member.entity.TeamMember;
import com.core_sync.hr_service.team_member.repository.TeamMemberRepository;
import com.core_sync.hr_service.work_session.repository.WorkSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {
    
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AttendanceRepository attendanceRepository;
    private final AnnualLeaveRepository annualLeaveRepository;
    private final WorkSessionRepository workSessionRepository;
    private final RestTemplate restTemplate;
    
    @Value("${account.service.url}")
    private String accountServiceUrl;
    
    @Value("${agile.service.url}")
    private String agileServiceUrl;
    
    @Override
    @Transactional
    public TeamCreateResponse createTeam(TeamCreateRequest request) {
        log.info("팀 생성 - name: {}, leaderId: {}", request.getName(), request.getLeaderId());
        
        // 팀 생성
        Team team = Team.builder()
                .name(request.getName())
                .leaderId(request.getLeaderId())
                .annualLeaveCount(request.getAnnualLeaveCount() != null ? request.getAnnualLeaveCount() : 15)
                .description(request.getDescription())
                .build();
        
        Team savedTeam = teamRepository.save(team);
        
        // 팀장을 팀 멤버로 추가
        TeamMember leader = TeamMember.builder()
                .team(savedTeam)
                .accountId(request.getLeaderId())
                .role(TeamMember.TeamRole.LEADER)
                .build();
        
        teamMemberRepository.save(leader);
        
        log.info("팀 생성 완료 - teamId: {}", savedTeam.getId());
        
        return TeamCreateResponse.builder()
                .teamId(savedTeam.getId())
                .name(savedTeam.getName())
                .build();
    }
    
    @Override
    public TeamResponse getTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        
        int memberCount = teamMemberRepository.countByTeamId(teamId);
        
        return TeamResponse.from(team, memberCount);
    }
    
    @Override
    public List<TeamResponse> getTeamsByAccountId(Long accountId) {
        List<TeamMember> teamMembers = teamMemberRepository.findByAccountIdWithTeam(accountId);
        
        return teamMembers.stream()
                .map(tm -> {
                    Team team = tm.getTeam();
                    int memberCount = teamMemberRepository.countByTeamId(team.getId());
                    return TeamResponse.from(team, memberCount);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public TeamResponse updateTeam(Long teamId, TeamUpdateRequest request, Long accountId) {
        log.info("팀 업데이트 - teamId: {}, accountId: {}", teamId, accountId);
        
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        
        // 팀장 권한 확인
        if (!team.getLeaderId().equals(accountId)) {
            throw new IllegalArgumentException("팀장만 팀 정보를 수정할 수 있습니다.");
        }
        
        // 팀 정보 업데이트
        if (request.getName() != null) {
            team.setName(request.getName());
        }
        if (request.getAnnualLeaveCount() != null) {
            team.setAnnualLeaveCount(request.getAnnualLeaveCount());
        }
        if (request.getMinimumWorkHours() != null) {
            team.setMinimumWorkHours(request.getMinimumWorkHours());
        }
        if (request.getDescription() != null) {
            team.setDescription(request.getDescription());
        }
        
        Team updatedTeam = teamRepository.save(team);
        int memberCount = teamMemberRepository.countByTeamId(teamId);
        
        log.info("팀 업데이트 완료 - teamId: {}", teamId);
        
        return TeamResponse.from(updatedTeam, memberCount);
    }
    
    @Override
    @Transactional
    public void deleteTeam(Long teamId, Long accountId) {
        log.info("팀 삭제 - teamId: {}, accountId: {}", teamId, accountId);
        
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        
        // 팀장 권한 확인
        if (!team.getLeaderId().equals(accountId)) {
            throw new IllegalArgumentException("팀장만 팀을 삭제할 수 있습니다.");
        }
        
        // 1. agile_service의 팀 프로젝트 삭제
        try {
            String url = agileServiceUrl + "/project/team/" + teamId;
            log.info("agile_service 팀 프로젝트 삭제 요청: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            log.info("agile_service 팀 프로젝트 삭제 성공");
        } catch (Exception e) {
            log.warn("agile_service 팀 프로젝트 삭제 실패 (계속 진행): {}", e.getMessage());
        }
        
        // 2. hr_service 내부 데이터 삭제
        // 팀 관련 근태 데이터 삭제
        log.info("팀 관련 근킬 데이터 삭제 시작 - teamId: {}", teamId);
        List<com.core_sync.hr_service.attendance.entity.Attendance> attendances = 
            attendanceRepository.findByTeamIdAndDateBetween(teamId, 
                java.time.LocalDate.of(2000, 1, 1), 
                java.time.LocalDate.of(2100, 12, 31));
        
        // work_session 먼저 삭제 (외래 키 제약)
        for (com.core_sync.hr_service.attendance.entity.Attendance attendance : attendances) {
            workSessionRepository.deleteByAttendanceId(attendance.getId());
        }
        log.info("work_session 데이터 삭제 완료");
        
        attendanceRepository.deleteAll(attendances);
        log.info("근킬 데이터 {} 건 삭제", attendances.size());
        
        // 팀 관련 연차 데이터 삭제
        List<com.core_sync.hr_service.annual_leave.entity.AnnualLeave> leaves = 
            annualLeaveRepository.findByTeamIdOrderByCreatedAtDesc(teamId);
        annualLeaveRepository.deleteAll(leaves);
        log.info("연차 데이터 {} 건 삭제", leaves.size());
        
        // 팀 멤버 모두 삭제
        List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
        teamMemberRepository.deleteAll(members);
        log.info("팀 멤버 {} 명 삭제", members.size());
        
        // 팀 삭제
        teamRepository.delete(team);
        
        log.info("팀 삭제 완료 - teamId: {}", teamId);
    }
    
    @Override
    @Transactional
    public void inviteMember(Long teamId, Long accountId, Long inviterId) {
        log.info("팀 멤버 초대 - teamId: {}, accountId: {}, inviterId: {}", teamId, accountId, inviterId);
        
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        
        // 초대자가 팀 멤버인지 확인
        if (!teamMemberRepository.existsByTeamIdAndAccountId(teamId, inviterId)) {
            throw new IllegalArgumentException("팀 멤버만 초대할 수 있습니다.");
        }
        
        // 이미 팀 멤버인지 확인
        if (teamMemberRepository.existsByTeamIdAndAccountId(teamId, accountId)) {
            throw new IllegalArgumentException("이미 팀에 속한 멤버입니다.");
        }
        
        // 팀 멤버로 추가
        TeamMember member = TeamMember.builder()
                .team(team)
                .accountId(accountId)
                .role(TeamMember.TeamRole.MEMBER)
                .build();
        
        teamMemberRepository.save(member);
        
        log.info("팀 멤버 초대 완료 - accountId: {}", accountId);
    }
    
    @Override
    @Transactional
    public void removeMember(Long teamId, Long memberId, Long leaderId) {
        log.info("팀 멤버 제거 - teamId: {}, memberId: {}, leaderId: {}", teamId, memberId, leaderId);
        
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        
        // 팀장 권한 확인 (자기 자신을 제거하는 경우는 팀 탈퇴로 허용)
        if (!team.getLeaderId().equals(leaderId) && !leaderId.equals(memberId)) {
            throw new IllegalArgumentException("팀장만 멤버를 제거할 수 있습니다.");
        }
        
        // 팀장은 탈퇴할 수 없음
        if (team.getLeaderId().equals(memberId)) {
            throw new IllegalArgumentException("팀장은 탈퇴할 수 없습니다. 먼저 팀장 권한을 다른 사람에게 넘기거나 팀을 삭제하세요.");
        }
        
        TeamMember member = teamMemberRepository.findByTeamIdAndAccountId(teamId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("팀 멤버를 찾을 수 없습니다."));
        
        // 1. 해당 팀원의 근태 데이터 삭제
        log.info("팀원의 근태 데이터 삭제 - teamId: {}, accountId: {}", teamId, memberId);
        List<com.core_sync.hr_service.attendance.entity.Attendance> attendances = 
            attendanceRepository.findByTeamIdAndAccountIdAndDateBetween(
                teamId, 
                memberId,
                java.time.LocalDate.of(2000, 1, 1), 
                java.time.LocalDate.of(2100, 12, 31)
            );
        
        // work_session 먼저 삭제 (외래 키 제약)
        for (com.core_sync.hr_service.attendance.entity.Attendance attendance : attendances) {
            workSessionRepository.deleteByAttendanceId(attendance.getId());
        }
        log.info("work_session 데이터 삭제 완료");
        
        attendanceRepository.deleteAll(attendances);
        log.info("근태 데이터 {} 건 삭제", attendances.size());
        
        // 2. 해당 팀원의 연차 데이터 삭제
        log.info("팀원의 연차 데이터 삭제 - teamId: {}, accountId: {}", teamId, memberId);
        List<com.core_sync.hr_service.annual_leave.entity.AnnualLeave> leaves = 
            annualLeaveRepository.findByTeamIdAndAccountIdOrderByCreatedAtDesc(teamId, memberId);
        annualLeaveRepository.deleteAll(leaves);
        log.info("연차 데이터 {} 건 삭제", leaves.size());
        
        // 3. 팀 멤버 제거
        teamMemberRepository.delete(member);
        
        log.info("팀 멤버 제거 완료 - memberId: {}", memberId);
    }
    
    @Override
    public List<TeamMemberResponse> getTeamMembers(Long teamId) {
        List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
        
        return members.stream()
                .map(member -> {
                    // account_service에서 닉네임 조회
                    String nickname = null;
                    try {
                        String url = accountServiceUrl + "/api/account/" + member.getAccountId() + "/nickname";
                        java.util.Map response = restTemplate.getForObject(url, java.util.Map.class);
                        if (response != null && response.containsKey("nickname")) {
                            nickname = (String) response.get("nickname");
                        }
                    } catch (Exception e) {
                        log.warn("닉네임 조회 실패 - accountId: {}, error: {}", member.getAccountId(), e.getMessage());
                    }
                    
                    return TeamMemberResponse.from(member, nickname);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean isTeamLeader(Long teamId, Long accountId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        
        return team.getLeaderId().equals(accountId);
    }
    
    @Override
    public void validateTeamMember(Long teamId, Long accountId) {
        if (!teamMemberRepository.existsByTeamIdAndAccountId(teamId, accountId)) {
            throw new IllegalArgumentException("해당 팀의 멤버가 아닙니다.");
        }
    }
}
