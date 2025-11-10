package lk.sampath.leaderboard.controller;

import lk.sampath.leaderboard.entity.Project;
import lk.sampath.leaderboard.repository.ProjectRepository;
import lk.sampath.leaderboard.services.IssueImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API Controller for importing issues from SonarQube
 */
//@Slf4j
//@RestController
//@RequestMapping("/api/issues")
//@RequiredArgsConstructor
public class IssueImportController {
//
//    private final IssueImportService issueImportService;
//    private final ProjectRepository projectRepository;
//
//    /**
//     * Import issues for all projects
//     * POST /api/issues/import/all
//     *
//     * Query params:
//     * - fullSync: true/false (default: false)
//     *
//     * Example:
//     * curl -X POST "http://localhost:8080/api/issues/import/all?fullSync=true"
//     */
//    @PostMapping("/import/all")
//    public ResponseEntity<IssueImportService.IssueImportResult> importAllIssues(
//            @RequestParam(defaultValue = "false") boolean fullSync) {
//
//        log.info("API call: Import all issues - fullSync: {}", fullSync);
//
//        try {
//            IssueImportService.IssueImportResult result =
//                    issueImportService.importAllIssues(fullSync);
//
//            if (result.isSuccess()) {
//                return ResponseEntity.ok(result);
//            } else {
//                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(result);
//            }
//
//        } catch (Exception e) {
//            log.error("Error importing issues", e);
//
//            IssueImportService.IssueImportResult errorResult =
//                    new IssueImportService.IssueImportResult();
//            errorResult.setSuccess(false);
//            errorResult.setMessage("Import failed: " + e.getMessage());
//
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(errorResult);
//        }
//    }
//
//    /**
//     * Import issues for a specific project
//     * POST /api/issues/import/project/{projectKey}
//     *
//     * Query params:
//     * - fullSync: true/false (default: false)
//     *
//     * Example:
//     * curl -X POST "http://localhost:8080/api/issues/import/project/my-project"
//     */
//    @PostMapping("/import/project/{projectKey}")
//    public ResponseEntity<?> importProjectIssues(
//            @PathVariable String projectKey,
//            @RequestParam(defaultValue = "false") boolean fullSync) {
//
//        log.info("API call: Import issues for project: {} - fullSync: {}",
//                projectKey, fullSync);
//
//        try {
//            // Find project
//            Project project = projectRepository.findByProjectKey(projectKey)
//                    .orElseThrow(() -> new RuntimeException(
//                            "Project not found: " + projectKey));
//
//            // Import issues
//            IssueImportService.ProjectIssueImportResult result =
//                    issueImportService.importProjectIssues(project, fullSync);
//
//            return ResponseEntity.ok(result);
//
//        } catch (RuntimeException e) {
//            log.error("Project not found: {}", projectKey);
//
//            Map<String, String> error = new HashMap<>();
//            error.put("error", e.getMessage());
//            error.put("projectKey", projectKey);
//
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
//
//        } catch (Exception e) {
//            log.error("Error importing issues for project: {}", projectKey, e);
//
//            Map<String, String> error = new HashMap<>();
//            error.put("error", "Import failed: " + e.getMessage());
//            error.put("projectKey", projectKey);
//
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
//        }
//    }
//
//    /**
//     * Get issue import statistics
//     * GET /api/issues/stats
//     *
//     * Example:
//     * curl "http://localhost:8080/api/issues/stats"
//     */
//    @GetMapping("/stats")
//    public ResponseEntity<Map<String, Object>> getIssueStats() {
//        log.info("API call: Get issue statistics");
//
//        try {
//            Map<String, Object> stats = new HashMap<>();
//
//            // Get counts from repositories (you can add these methods to repositories)
//            // For now, return basic info
//            stats.put("message", "Issue statistics");
//            stats.put("endpoints", Map.of(
//                    "importAll", "POST /api/issues/import/all?fullSync=true",
//                    "importProject", "POST /api/issues/import/project/{key}",
//                    "stats", "GET /api/issues/stats"
//            ));
//
//            return ResponseEntity.ok(stats);
//
//        } catch (Exception e) {
//            log.error("Error getting stats", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("error", e.getMessage()));
//        }
//    }
//
//    /**
//     * Import issues by date range
//     * POST /api/issues/import/range
//     *
//     * Query params:
//     * - projectKey: project key (optional, if not provided imports for all)
//     * - fromDate: start date in ISO format (e.g., 2024-01-01T00:00:00)
//     * - toDate: end date in ISO format (optional)
//     *
//     * Example:
//     * curl -X POST "http://localhost:8080/api/issues/import/range?projectKey=my-project&fromDate=2024-01-01T00:00:00"
//     */
//    @PostMapping("/import/range")
//    public ResponseEntity<?> importIssuesByDateRange(
//            @RequestParam(required = false) String projectKey,
//            @RequestParam String fromDate,
//            @RequestParam(required = false) String toDate) {
//
//        log.info("API call: Import issues by date range - project: {}, from: {}, to: {}",
//                projectKey, fromDate, toDate);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("message", "Date range import not yet implemented");
//        response.put("projectKey", projectKey);
//        response.put("fromDate", fromDate);
//        response.put("toDate", toDate);
//        response.put("suggestion", "Use fullSync=false for incremental import or fullSync=true for last 90 days");
//
//        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(response);
//    }
}