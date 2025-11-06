package lk.sampath.leaderboard.repository;

import lk.sampath.leaderboard.entity.Developer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeveloperRepository extends JpaRepository<Developer, Integer> {
    Optional<Developer> findByAuthorKey(String authorKey);
    List<Developer> findByIsActiveTrue();
    long countByIsActiveTrue();
}
