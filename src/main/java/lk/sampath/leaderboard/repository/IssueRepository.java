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

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.project = :project AND i.issueType = :type AND i.status NOT IN (lk.sampath.leaderboard.entity.Issue.IssueStatus.RESOLVED, lk.sampath.leaderboard.entity.Issue.IssueStatus.CLOSED)")
    Long countOpenIssuesByProjectAndType(
            @Param("project") Project project,
            @Param("type") Issue.IssueType type
    );





    @Query("SELECT COUNT(i) FROM Issue i WHERE i.developer.id = :developerId " +
            "AND i.status = 'RESOLVED' AND i.resolvedDate BETWEEN :startDate AND :endDate")
    Long countResolvedIssues(@Param("developerId") Integer developerId,
                             @Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.developer.id = :developerId " +
            "AND i.createdDate BETWEEN :startDate AND :endDate")
    Long countIntroducedIssues(@Param("developerId") Integer developerId,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.project.id = :projectId " +
            "AND i.issueType = :issueType AND i.status IN (lk.sampath.leaderboard.entity.Issue.IssueStatus.OPEN, lk.sampath.leaderboard.entity.Issue.IssueStatus.CONFIRMED, lk.sampath.leaderboard.entity.Issue.IssueStatus.REOPENED)")
    Long countOpenIssuesByType(@Param("projectId") Integer projectId,
                               @Param("issueType") Issue.IssueType issueType);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.developer.id = :developerId " +
            "AND i.issueType = :issueType AND i.status IN (lk.sampath.leaderboard.entity.Issue.IssueStatus.OPEN, lk.sampath.leaderboard.entity.Issue.IssueStatus.CONFIRMED, lk.sampath.leaderboard.entity.Issue.IssueStatus.REOPENED)")
    Long countOpenIssuesByDeveloperAndType(@Param("developerId") Integer developerId,
                                           @Param("issueType") Issue.IssueType issueType);

    List<Issue> findByProjectAndCreatedDateBetween(Project project,
                                                   LocalDateTime startDate,
                                                   LocalDateTime endDate);

    List<Issue> findByDeveloperAndCreatedDateBetween(Developer developer,
                                                     LocalDateTime startDate,
                                                     LocalDateTime endDate);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.project.id = :projectId")
    Long countByProject(@Param("projectId") Integer projectId);
}
