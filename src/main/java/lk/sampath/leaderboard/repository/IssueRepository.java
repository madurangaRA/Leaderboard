package lk.sampath.leaderboard.repository;

import lk.sampath.leaderboard.entity.Developer;
import lk.sampath.leaderboard.entity.Issue;
import lk.sampath.leaderboard.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Integer> {
    Optional<Issue> findByIssueKey(String issueKey);
    boolean existsByIssueKey(String issueKey);

    @Query("SELECT i FROM Issue i WHERE i.project = :project AND i.createdDate >= :fromDate")
    List<Issue> findByProjectAndCreatedDateAfter(
            @Param("project") Project project,
            @Param("fromDate") LocalDateTime fromDate
    );

    @Query("SELECT i FROM Issue i WHERE i.developer = :developer AND i.createdDate >= :fromDate")
    List<Issue> findByDeveloperAndCreatedDateAfter(
            @Param("developer") Developer developer,
            @Param("fromDate") LocalDateTime fromDate
    );

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.project = :project AND i.issueType = :type AND i.status NOT IN ('RESOLVED', 'CLOSED')")
    Long countOpenIssuesByProjectAndType(
            @Param("project") Project project,
            @Param("type") Issue.IssueType type
    );
}
