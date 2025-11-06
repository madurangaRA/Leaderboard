package lk.sampath.leaderboard.repository;

import lk.sampath.leaderboard.entity.Developer;
import lk.sampath.leaderboard.entity.DeveloperMetricsDaily;
import lk.sampath.leaderboard.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeveloperMetricsDailyRepository extends JpaRepository<DeveloperMetricsDaily, Integer> {
    Optional<DeveloperMetricsDaily> findByDeveloperAndProjectAndDateRecorded(
            Developer developer,
            Project project,
            LocalDate date
    );

    @Query("SELECT d FROM DeveloperMetricsDaily d WHERE d.developer = :developer AND d.dateRecorded >= :fromDate ORDER BY d.dateRecorded DESC")
    List<DeveloperMetricsDaily> findByDeveloperAndDateAfter(
            @Param("developer") Developer developer,
            @Param("fromDate") LocalDate fromDate
    );

    @Query("SELECT d FROM DeveloperMetricsDaily d WHERE d.project = :project AND d.dateRecorded = :date")
    List<DeveloperMetricsDaily> findByProjectAndDate(
            @Param("project") Project project,
            @Param("date") LocalDate date
    );
}
