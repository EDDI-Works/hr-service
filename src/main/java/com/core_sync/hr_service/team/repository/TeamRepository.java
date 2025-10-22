package com.core_sync.hr_service.team.repository;

import com.core_sync.hr_service.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByLeaderId(Long leaderId);
}
