package lk.sampath.leaderboard.repository;

import lk.sampath.leaderboard.entity.Project;
import lk.sampath.leaderboard.entity.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SyncLogRepository extends JpaRepository<SyncLog, Integer> {
    @Query("SELECT s FROM SyncLog s ORDER BY s.startTime DESC")
    List<SyncLog> findAllOrderByStartTimeDesc();

    @Query("SELECT s FROM SyncLog s WHERE s.project = :project ORDER BY s.startTime DESC")
    List<SyncLog> findByProjectOrderByStartTimeDesc(@Param("project") Project project);

    @Query("SELECT s FROM SyncLog s WHERE s.status = :status ORDER BY s.startTime DESC")
    List<SyncLog> findByStatusOrderByStartTimeDesc(@Param("status") SyncLog.SyncStatus status);

    @Query("SELECT s FROM SyncLog s WHERE s.project = :project AND s.status = 'SUCCESS' ORDER BY s.startTime DESC")
    Optional<SyncLog> findLastSuccessfulSync(@Param("project") Project project);
}
