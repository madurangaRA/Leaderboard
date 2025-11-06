package lk.sampath.leaderboard.repository;

import lk.sampath.leaderboard.entity.Project;
import lk.sampath.leaderboard.entity.ProjectMetricsDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMetricsDailyRepository extends JpaRepository<ProjectMetricsDaily, Integer> {
    Optional<ProjectMetricsDaily> findByProjectAndDateRecorded(Project project, LocalDate date);

    @Query("SELECT p FROM ProjectMetricsDaily p WHERE p.project = :project AND p.dateRecorded >= :fromDate ORDER BY p.dateRecorded DESC")
    List<ProjectMetricsDaily> findByProjectAndDateAfter(
            @Param("project") Project project,
            @Param("fromDate") LocalDate fromDate
    );

    boolean existsByProjectAndDateRecorded(Project project, LocalDate date);
}