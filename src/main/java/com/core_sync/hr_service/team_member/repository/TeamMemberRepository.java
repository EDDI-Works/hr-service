package com.core_sync.hr_service.team_member.repository;

import com.core_sync.hr_service.team_member.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    
    @Query("SELECT tm FROM TeamMember tm JOIN FETCH tm.team WHERE tm.accountId = :accountId")
    List<TeamMember> findByAccountIdWithTeam(Long accountId);
    
    boolean existsByTeamIdAndAccountId(Long teamId, Long accountId);
    
    Optional<TeamMember> findByTeamIdAndAccountId(Long teamId, Long accountId);
    
    List<TeamMember> findByTeamId(Long teamId);
    
    int countByTeamId(Long teamId);
}
