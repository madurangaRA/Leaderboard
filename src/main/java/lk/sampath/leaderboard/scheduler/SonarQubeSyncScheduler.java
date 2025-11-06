package lk.sampath.leaderboard.scheduler;


import lk.sampath.leaderboard.dto.SyncResponse;
import lk.sampath.leaderboard.services.SonarQubeSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sonarqube.sync.enabled", havingValue = "true", matchIfMissing = true)
public class SonarQubeSyncScheduler {

    private final SonarQubeSyncService syncService;

    /**
     * Scheduled daily sync - configured via cron expression in application.yml
     * Default: "0 0 2 * * ?" (2 AM daily)
     */
    @Scheduled(cron = "${sonarqube.sync.cron:0 0 2 * * ?}")
    public void scheduledSync() {
        log.info("=== Starting scheduled SonarQube sync ===");

        try {
            SyncResponse response = syncService.syncAllProjects(false);

            if (response.isSuccess()) {
                SyncResponse.SyncStats stats = response.getStats();
                log.info("Scheduled sync completed successfully:");
                log.info("  - Projects: {}", stats.getProjectsProcessed());
                log.info("  - Issues Created: {}", stats.getIssuesCreated());
                log.info("  - Issues Updated: {}", stats.getIssuesUpdated());
                log.info("  - Developers: {}", stats.getDevelopersCreated());
                log.info("  - Duration: {}ms", stats.getDurationMs());
            } else {
                log.error("Scheduled sync failed: {}", response.getMessage());
            }

        } catch (Exception e) {
            log.error("Error during scheduled sync", e);
        }

        log.info("=== Scheduled SonarQube sync completed ===");
    }

    /**
     * Optional: Run sync on startup for development/testing
     * Uncomment @Scheduled annotation to enable
     */
    // @Scheduled(initialDelay = 30000) // 30 seconds after startup
    public void initialSync() {
        log.info("Running initial sync on startup");
        scheduledSync();
    }
}
