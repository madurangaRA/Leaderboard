package lk.sampath.leaderboard.repository;

import lk.sampath.leaderboard.entity.SonarQubeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SonarQubeConfigRepository extends JpaRepository<SonarQubeConfig, Long> {
    Optional<SonarQubeConfig> findTopByOrderByUpdatedAtDesc();
}

