package lk.sampath.leaderboard.controller;

import lk.sampath.leaderboard.client.SonarQubeClient;
import lk.sampath.leaderboard.dto.SyncResponse;
import lk.sampath.leaderboard.entity.Project;
import lk.sampath.leaderboard.entity.SyncLog;
import lk.sampath.leaderboard.repository.ProjectRepository;
import lk.sampath.leaderboard.repository.SyncLogRepository;
import lk.sampath.leaderboard.services.SonarQubeSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SonarQubeSyncService syncService;
    private final SonarQubeClient sonarQubeClient;
    private final ProjectRepository projectRepository;
    private final SyncLogRepository syncLogRepository;

    /**
     * Trigger manual sync for all projects
     * POST /api/sync/all
     */
    @PostMapping("/all")
    public ResponseEntity<SyncResponse> syncAll(@RequestParam(defaultValue = "false") boolean fullSync) {
        log.info("Manual sync triggered - fullSync: {}", fullSync);

        try {
            SyncResponse response = syncService.syncAllProjects(fullSync);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Manual sync failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SyncResponse.failure("Sync failed: " + e.getMessage()));
        }
    }

    /**
     * Trigger manual sync for a specific project
     * POST /api/sync/project/{projectKey}
     */
    @PostMapping("/project/{projectKey}")
    public ResponseEntity<SyncResponse> syncProject(
            @PathVariable String projectKey,
            @RequestParam(defaultValue = "false") boolean fullSync) {

        log.info("Manual project sync triggered - project: {}, fullSync: {}", projectKey, fullSync);

        try {
            Project project = projectRepository.findByProjectKey(projectKey)
                    .orElseThrow(() -> new RuntimeException("Project not found: " + projectKey));

            long startTime = System.currentTimeMillis();
            SyncResponse.SyncStats stats = syncService.syncProject(project, fullSync);
            stats.setDurationMs(System.currentTimeMillis() - startTime);
            stats.setProjectsProcessed(1);

            return ResponseEntity.ok(SyncResponse.success(
                    "Project synced successfully", null, stats));

        } catch (Exception e) {
            log.error("Project sync failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SyncResponse.failure("Project sync failed: " + e.getMessage()));
        }
    }

    /**
     * Test SonarQube connection
     * GET /api/sync/test-connection
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        log.info("Testing SonarQube connection");

        Map<String, Object> response = new HashMap<>();
        boolean connected = sonarQubeClient.testConnection();

        response.put("connected", connected);
        response.put("status", connected ? "SUCCESS" : "FAILED");
        response.put("message", connected ?
                "Successfully connected to SonarQube" :
                "Failed to connect to SonarQube");

        return ResponseEntity.ok(response);
    }

    /**
     * Get sync logs
     * GET /api/sync/logs
     */
    @GetMapping("/logs")
    public ResponseEntity<List<SyncLog>> getSyncLogs(
            @RequestParam(required = false) Integer limit) {

        List<SyncLog> logs = syncLogRepository.findAllOrderByStartTimeDesc();

        if (limit != null && limit > 0 && logs.size() > limit) {
            logs = logs.subList(0, limit);
        }

        return ResponseEntity.ok(logs);
    }

    /**
     * Get sync log by ID
     * GET /api/sync/logs/{id}
     */
    @GetMapping("/logs/{id}")
    public ResponseEntity<SyncLog> getSyncLog(@PathVariable Integer id) {
        return syncLogRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all projects
     * GET /api/sync/projects
     */
    @GetMapping("/projects")
    public ResponseEntity<List<Project>> getProjects() {
        return ResponseEntity.ok(projectRepository.findAll());
    }

    /**
     * Get sync status
     * GET /api/sync/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        Map<String, Object> status = new HashMap<>();

        // Get last sync log
        List<SyncLog> recentLogs = syncLogRepository.findAllOrderByStartTimeDesc();
        if (!recentLogs.isEmpty()) {
            SyncLog lastSync = recentLogs.get(0);
            status.put("lastSyncTime", lastSync.getStartTime());
            status.put("lastSyncStatus", lastSync.getStatus());
            status.put("lastSyncType", lastSync.getSyncType());

            if (lastSync.getEndTime() != null && lastSync.getStartTime() != null) {
                long duration = java.time.Duration.between(
                        lastSync.getStartTime(),
                        lastSync.getEndTime()).toMillis();
                status.put("lastSyncDurationMs", duration);
            }
        }

        // Count projects and active projects
        long totalProjects = projectRepository.count();
        long activeProjects = projectRepository.findByIsActiveTrue().size();

        status.put("totalProjects", totalProjects);
        status.put("activeProjects", activeProjects);

        return ResponseEntity.ok(status);
    }
}
