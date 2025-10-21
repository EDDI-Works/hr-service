package com.core_sync.hr_service.team.controller;

import com.core_sync.hr_service.redis_cache.service.RedisCacheService;
import com.core_sync.hr_service.team.controller.request.*;
import com.core_sync.hr_service.team.service.TeamService;
import com.core_sync.hr_service.team.service.request.*;
import com.core_sync.hr_service.team.service.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
public class TeamController {
    
    private final TeamService teamService;
    private final RedisCacheService redisCacheService;
    private final RestTemplate restTemplate;
    
    @Value("${account.service.url}")
    private String accountServiceUrl;
    
    @Value("${agile.service.url}")
    private String agileServiceUrl;
    
    @PostMapping("/register")
    public ResponseEntity<TeamCreateResponse> createTeam(
            @RequestHeader("Authorization") String token,
            @RequestBody TeamCreateRequestForm requestForm
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        TeamCreateRequest request = TeamCreateRequest.builder()
                .name(requestForm.getName())
                .leaderId(accountId)
                .annualLeaveCount(requestForm.getAnnualLeaveCount())
                .description(requestForm.getDescription())
                .build();
        
        TeamCreateResponse response = teamService.createTeam(request);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponse> getTeam(@PathVariable Long teamId) {
        TeamResponse response = teamService.getTeam(teamId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/list")
    public ResponseEntity<List<TeamResponse>> getTeamList(
            @RequestHeader("Authorization") String token
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        List<TeamResponse> teams = teamService.getTeamsByAccountId(accountId);
        
        return ResponseEntity.ok(teams);
    }
    
    @PutMapping("/{teamId}")
    public ResponseEntity<TeamResponse> updateTeam(
            @RequestHeader("Authorization") String token,
            @PathVariable Long teamId,
            @RequestBody TeamUpdateRequestForm requestForm
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        TeamUpdateRequest request = TeamUpdateRequest.builder()
                .name(requestForm.getName())
                .annualLeaveCount(requestForm.getAnnualLeaveCount())
                .minimumWorkHours(requestForm.getMinimumWorkHours())
                .description(requestForm.getDescription())
                .build();
        
        TeamResponse response = teamService.updateTeam(teamId, request, accountId);
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Map<String, String>> deleteTeam(
            @RequestHeader("Authorization") String token,
            @PathVariable Long teamId
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        teamService.deleteTeam(teamId, accountId);
        
        return ResponseEntity.ok(Map.of("message", "팀이 삭제되었습니다."));
    }
    
    @PostMapping("/{teamId}/invite")
    public ResponseEntity<Map<String, String>> inviteMember(
            @RequestHeader("Authorization") String token,
            @PathVariable Long teamId,
            @RequestBody Map<String, Object> request
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long inviterId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (inviterId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        // accountId 또는 email로 초대 가능
        Long accountId = null;
        if (request.containsKey("accountId")) {
            accountId = ((Number) request.get("accountId")).longValue();
        } else if (request.containsKey("email")) {
            String email = (String) request.get("email");
            // account_service에서 이메일로 accountId 조회
            try {
                String url = accountServiceUrl + "/account/find-by-email?email=" + email;
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", token);
                
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
                );
                
                if (response.getBody() == null || !response.getBody().containsKey("accountId")) {
                    throw new IllegalArgumentException("해당 이메일로 등록된 계정을 찾을 수 없습니다.");
                }
                
                accountId = ((Number) response.getBody().get("accountId")).longValue();
                log.info("이메일로 계정 조회 성공 - email: {}, accountId: {}", email, accountId);
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                log.error("이메일로 계정 조회 실패 (4xx): {}", e.getMessage());
                throw new IllegalArgumentException("해당 이메일로 등록된 계정을 찾을 수 없습니다: " + email);
            } catch (org.springframework.web.client.HttpServerErrorException e) {
                log.error("이메일로 계정 조회 실패 (5xx): {}", e.getMessage());
                throw new RuntimeException("account_service 서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            } catch (Exception e) {
                log.error("이메일로 계정 조회 실패: {}", e.getMessage(), e);
                throw new RuntimeException("계정 조회 중 오류가 발생했습니다: " + e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("accountId 또는 email을 입력해주세요.");
        }
        
        teamService.inviteMember(teamId, accountId, inviterId);
        
        return ResponseEntity.ok(Map.of("message", "멤버 초대가 완료되었습니다."));
    }
    
    @DeleteMapping("/{teamId}/member/{memberId}")
    public ResponseEntity<Map<String, String>> removeMember(
            @RequestHeader("Authorization") String token,
            @PathVariable Long teamId,
            @PathVariable Long memberId
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long leaderId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (leaderId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        teamService.removeMember(teamId, memberId, leaderId);
        
        return ResponseEntity.ok(Map.of("message", "멤버가 제거되었습니다."));
    }
    
    @PostMapping("/{teamId}/leave")
    public ResponseEntity<Map<String, String>> leaveTeam(
            @RequestHeader("Authorization") String token,
            @PathVariable Long teamId
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        teamService.removeMember(teamId, accountId, accountId);
        
        return ResponseEntity.ok(Map.of("message", "팀에서 탈퇴했습니다."));
    }
    
    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponse>> getTeamMembers(@PathVariable Long teamId) {
        List<TeamMemberResponse> members = teamService.getTeamMembers(teamId);
        return ResponseEntity.ok(members);
    }
    
    @GetMapping("/{teamId}/is-leader")
    public ResponseEntity<Map<String, Boolean>> isTeamLeader(
            @RequestHeader("Authorization") String token,
            @PathVariable Long teamId
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        boolean isLeader = teamService.isTeamLeader(teamId, accountId);
        
        return ResponseEntity.ok(Map.of("isLeader", isLeader));
    }
    
    @GetMapping("/{teamId}/validate-member")
    public ResponseEntity<Map<String, Boolean>> validateMember(
            @RequestHeader("Authorization") String token,
            @PathVariable Long teamId
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        try {
            teamService.validateTeamMember(teamId, accountId);
            return ResponseEntity.ok(Map.of("isMember", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("isMember", false));
        }
    }
    
    @PostMapping("/{teamId}/project/register")
    public ResponseEntity<?> registerTeamProject(
            @RequestHeader("Authorization") String token,
            @PathVariable Long teamId,
            @RequestBody Map<String, String> requestForm
    ) {
        String userToken = token.replace("Bearer ", "").trim();
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        
        if (accountId == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        
        // 팀 멤버 검증
        teamService.validateTeamMember(teamId, accountId);
        
        String title = requestForm.get("title");
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("프로젝트 제목을 입력해주세요.");
        }
        
        // agile_service에 프로젝트 생성 요청
        try {
            String url = agileServiceUrl + "/project/register";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", token);
            
            Map<String, Object> projectRequest = new HashMap<>();
            projectRequest.put("title", title);
            projectRequest.put("teamId", teamId);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(projectRequest, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("프로젝트 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("프로젝트 생성에 실패했습니다: " + e.getMessage());
        }
    }
}
