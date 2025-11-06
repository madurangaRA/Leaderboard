package lk.sampath.leaderboard.services.impl;

import lk.sampath.leaderboard.client.SonarQubeClient;
import lk.sampath.leaderboard.dto.SonarIssuesSearchResponse;
import lk.sampath.leaderboard.dto.SonarMeasuresResponse;
import lk.sampath.leaderboard.dto.SonarProjectSearchResponse;
import lk.sampath.leaderboard.dto.SyncResponse;
import lk.sampath.leaderboard.entity.*;
import lk.sampath.leaderboard.repository.*;
import lk.sampath.leaderboard.services.SonarQubeSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SonarQubeSyncServiceImpl implements SonarQubeSyncService {

    private final SonarQubeClient sonarQubeClient;
    private final ProjectRepository projectRepository;
    private final DeveloperRepository developerRepository;
    private final IssueRepository issueRepository;
    private final ProjectMetricsDailyRepository projectMetricsDailyRepository;
    private final DeveloperMetricsDailyRepository developerMetricsDailyRepository;
    private final SyncLogRepository syncLogRepository;

    @Value("${sonarqube.sync.request-delay-ms:100}")
    private long requestDelayMs;

    @Value("${sonarqube.sync.historical-days:90}")
    private int historicalDays;

    @Value("${sonarqube.sync.max-issues-per-request:500}")
    private int maxIssuesPerRequest;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    /**
     * Sync all projects and their data
     */
    @Override
    @Transactional
    public SyncResponse syncAllProjects(boolean fullSync) {
        log.info("========================================");
        log.info("Starting {} sync for all projects", fullSync ? "FULL" : "INCREMENTAL");
        log.info("========================================");

        long startTime = System.currentTimeMillis();
        SyncResponse.SyncStats stats = new SyncResponse.SyncStats();

        SyncLog syncLog = createSyncLog(null, fullSync);

        try {
            // Step 1: Test connection first
            log.info("Testing SonarQube connection...");
            if (!sonarQubeClient.testConnection()) {
                throw new RuntimeException("Cannot connect to SonarQube");
            }
            log.info("✓ Connection successful");

            // Step 2: Sync projects from SonarQube
            log.info("Fetching projects from SonarQube...");
            List<Project> projects = syncProjects();
            stats.setProjectsProcessed(projects.size());
            log.info("✓ Synced {} projects", projects.size());

            if (projects.isEmpty()) {
                log.warn("No projects found in SonarQube");
                throw new RuntimeException("No projects found in SonarQube");
            }

            // Step 3: For each project, sync issues and metrics
            int projectCount = 0;
            for (Project project : projects) {
                projectCount++;
                try {
                    log.info("----------------------------------------");
                    log.info("Processing project {}/{}: {}", projectCount, projects.size(), project.getProjectKey());
                    log.info("----------------------------------------");

                    SyncResponse.SyncStats projectStats = syncProject(project, fullSync);

                    // Aggregate stats
                    stats.setDevelopersCreated(stats.getDevelopersCreated() + projectStats.getDevelopersCreated());
                    stats.setIssuesCreated(stats.getIssuesCreated() + projectStats.getIssuesCreated());
                    stats.setIssuesUpdated(stats.getIssuesUpdated() + projectStats.getIssuesUpdated());
                    stats.setMetricsCreated(stats.getMetricsCreated() + projectStats.getMetricsCreated());

                    log.info("✓ Project {} completed", project.getProjectKey());
                    log.info("  - Issues created: {}", projectStats.getIssuesCreated());
                    log.info("  - Issues updated: {}", projectStats.getIssuesUpdated());
                    log.info("  - Developers: {}", projectStats.getDevelopersCreated());

                    // Delay between projects to avoid rate limiting
                    if (projectCount < projects.size()) {
                        log.debug("Waiting {}ms before next project...", requestDelayMs);
                        Thread.sleep(requestDelayMs);
                    }

                } catch (Exception e) {
                    log.error("✗ Error syncing project {}: {}", project.getProjectKey(), e.getMessage(), e);
                    // Continue with next project
                }
            }

            // Update sync log
            stats.setDurationMs(System.currentTimeMillis() - startTime);
            syncLog.setStatus(SyncLog.SyncStatus.SUCCESS);
            syncLog.setRecordsProcessed(stats.getIssuesCreated() + stats.getIssuesUpdated());
            syncLog.setRecordsCreated(stats.getIssuesCreated());
            syncLog.setRecordsUpdated(stats.getIssuesUpdated());
            syncLog.setEndTime(LocalDateTime.now());

            // Create sync details JSON
            String syncDetails = String.format(
                    "{\"projects\": %d, \"developers\": %d, \"issues_created\": %d, \"issues_updated\": %d, \"metrics\": %d}",
                    stats.getProjectsProcessed(),
                    stats.getDevelopersCreated(),
                    stats.getIssuesCreated(),
                    stats.getIssuesUpdated(),
                    stats.getMetricsCreated()
            );
            syncLog.setSyncDetails(syncDetails);

            syncLogRepository.save(syncLog);

            log.info("========================================");
            log.info("✓ SYNC COMPLETED SUCCESSFULLY");
            log.info("  Duration: {}ms ({}s)", stats.getDurationMs(), stats.getDurationMs() / 1000);
            log.info("  Projects: {}", stats.getProjectsProcessed());
            log.info("  Developers: {}", stats.getDevelopersCreated());
            log.info("  Issues Created: {}", stats.getIssuesCreated());
            log.info("  Issues Updated: {}", stats.getIssuesUpdated());
            log.info("  Metrics Created: {}", stats.getMetricsCreated());
            log.info("========================================");

            return SyncResponse.success("Sync completed successfully", syncLog.getId(), stats);

        } catch (Exception e) {
            log.error("========================================");
            log.error("✗ SYNC FAILED: {}", e.getMessage(), e);
            log.error("========================================");

            syncLog.setStatus(SyncLog.SyncStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setEndTime(LocalDateTime.now());
            syncLog.setRecordsProcessed(stats.getIssuesCreated() + stats.getIssuesUpdated());
            syncLog.setRecordsCreated(stats.getIssuesCreated());
            syncLog.setRecordsUpdated(stats.getIssuesUpdated());
            syncLogRepository.save(syncLog);

            return SyncResponse.failure("Sync failed: " + e.getMessage());
        }
    }

    /**
     * Sync a specific project
     */
    @Override
    @Transactional
    public SyncResponse.SyncStats syncProject(Project project, boolean fullSync) {
        SyncResponse.SyncStats stats = new SyncResponse.SyncStats();

        // Determine the date range for sync
        String createdAfter = calculateSyncDateRange(project, fullSync);

        log.info("Syncing issues from: {}", createdAfter != null ? createdAfter : "beginning");

        // Sync issues
        Set<String> developerKeys = new HashSet<>();
        int issuesCreated = 0;
        int issuesUpdated = 0;
        int page = 1;
        boolean hasMore = true;
        int totalIssues = 0;

        while (hasMore) {
            log.debug("Fetching issues page {}...", page);

            SonarIssuesSearchResponse response = sonarQubeClient.searchAllIssues(
                    project.getProjectKey(), page, createdAfter, null);

            if (response == null || response.getIssues() == null) {
                log.warn("No response from SonarQube for page {}", page);
                break;
            }

            log.debug("Retrieved {} issues from page {}", response.getIssues().size(), page);

            for (SonarIssuesSearchResponse.IssueDetail issueDetail : response.getIssues()) {
                try {
                    boolean isNew = syncIssue(issueDetail, project);
                    if (isNew) {
                        issuesCreated++;
                    } else {
                        issuesUpdated++;
                    }
                    totalIssues++;

                    // Track developers
                    if (issueDetail.getAuthor() != null && !issueDetail.getAuthor().isEmpty()) {
                        developerKeys.add(issueDetail.getAuthor());
                    }

                } catch (Exception e) {
                    log.error("Error syncing issue {}: {}", issueDetail.getKey(), e.getMessage());
                }
            }

            // Check if there are more pages
            int currentTotal = response.getPaging().getPageIndex() * response.getPaging().getPageSize();
            hasMore = currentTotal < response.getPaging().getTotal();

            if (hasMore) {
                log.debug("More issues available. Total so far: {}/{}",
                        currentTotal, response.getPaging().getTotal());
            }

            page++;

            // Delay between requests to avoid rate limiting
            if (hasMore) {
                try {
                    Thread.sleep(requestDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Thread interrupted during delay");
                    break;
                }
            }
        }

        stats.setIssuesCreated(issuesCreated);
        stats.setIssuesUpdated(issuesUpdated);
        stats.setDevelopersCreated(developerKeys.size());

        log.info("Issue sync complete: {} total issues processed", totalIssues);

        // Sync project metrics
        try {
            log.info("Fetching project metrics...");
            syncProjectMetrics(project);
            stats.setMetricsCreated(1);
            log.info("✓ Project metrics synced");
        } catch (Exception e) {
            log.error("Error syncing project metrics: {}", e.getMessage());
        }

        // Calculate developer metrics for current month
        try {
            log.info("Calculating developer metrics...");
            LocalDate today = LocalDate.now();
            LocalDate monthStart = today.withDayOfMonth(1);

            // Calculate metrics for each day in current month
            LocalDate date = monthStart;
            while (!date.isAfter(today)) {
                calculateDeveloperMetrics(project, date);
                date = date.plusDays(1);
            }
            log.info("✓ Developer metrics calculated");
        } catch (Exception e) {
            log.error("Error calculating developer metrics: {}", e.getMessage());
        }

        return stats;
    }

    /**
     * Sync projects from SonarQube
     */
    private List<Project> syncProjects() {
        List<Project> projects = new ArrayList<>();
        int page = 1;
        boolean hasMore = true;

        while (hasMore) {
            log.debug("Fetching projects page {}...", page);

            SonarProjectSearchResponse response = sonarQubeClient.searchProjects(page);

            if (response == null || response.getComponents() == null) {
                log.warn("No response from SonarQube for projects page {}", page);
                break;
            }

            for (SonarProjectSearchResponse.Component component : response.getComponents()) {
                try {
                    Project project = projectRepository.findByProjectKey(component.getKey())
                            .orElse(new Project());

                    boolean isNew = project.getId() == null;
                    project.setProjectKey(component.getKey());
                    project.setProjectName(component.getName());
                    project.setIsActive(true);

                    project = projectRepository.save(project);
                    projects.add(project);

                    log.debug("{} project: {} ({})",
                            isNew ? "Created" : "Updated",
                            component.getName(),
                            component.getKey());

                } catch (Exception e) {
                    log.error("Error syncing project {}: {}", component.getKey(), e.getMessage());
                }
            }

            // Check if there are more pages
            hasMore = response.getPaging().getPageIndex() * response.getPaging().getPageSize()
                    < response.getPaging().getTotal();
            page++;

            // Delay between requests
            if (hasMore) {
                try {
                    Thread.sleep(requestDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return projects;
    }

    /**
     * Sync a single issue
     */
    private boolean syncIssue(SonarIssuesSearchResponse.IssueDetail issueDetail, Project project) {
        Issue issue = issueRepository.findByIssueKey(issueDetail.getKey())
                .orElse(new Issue());

        boolean isNew = issue.getId() == null;

        issue.setIssueKey(issueDetail.getKey());
        issue.setProject(project);
        issue.setRuleKey(issueDetail.getRule());
        issue.setSeverity(parseSeverity(issueDetail.getSeverity()));
        issue.setIssueType(parseIssueType(issueDetail.getType()));
        issue.setStatus(parseStatus(issueDetail.getStatus()));
        issue.setComponentPath(issueDetail.getComponent());
        issue.setLineNumber(issueDetail.getLine());
        issue.setMessage(issueDetail.getMessage());
        issue.setEffortMinutes(parseEffort(issueDetail.getEffort()));

        // Parse dates
        if (issueDetail.getCreationDate() != null) {
            try {
                issue.setCreatedDate(LocalDateTime.parse(issueDetail.getCreationDate(), ISO_FORMATTER));
            } catch (Exception e) {
                log.warn("Could not parse creation date: {}", issueDetail.getCreationDate());
            }
        }

        if (issueDetail.getUpdateDate() != null) {
            try {
                issue.setUpdatedDate(LocalDateTime.parse(issueDetail.getUpdateDate(), ISO_FORMATTER));
            } catch (Exception e) {
                log.warn("Could not parse update date: {}", issueDetail.getUpdateDate());
            }
        }

        if (issueDetail.getCloseDate() != null) {
            try {
                issue.setResolvedDate(LocalDateTime.parse(issueDetail.getCloseDate(), ISO_FORMATTER));
            } catch (Exception e) {
                log.warn("Could not parse close date: {}", issueDetail.getCloseDate());
            }
        }

        // Handle developer
        if (issueDetail.getAuthor() != null && !issueDetail.getAuthor().isEmpty()) {
            try {
                Developer developer = syncDeveloper(issueDetail.getAuthor());
                issue.setDeveloper(developer);
            } catch (Exception e) {
                log.warn("Could not sync developer {}: {}", issueDetail.getAuthor(), e.getMessage());
            }
        }

        issueRepository.save(issue);

        if (isNew) {
            log.trace("Created issue: {} ({})", issueDetail.getKey(), issueDetail.getType());
        } else {
            log.trace("Updated issue: {} ({})", issueDetail.getKey(), issueDetail.getType());
        }

        return isNew;
    }

    /**
     * Sync or create a developer
     */
    private Developer syncDeveloper(String authorKey) {
        return developerRepository.findByAuthorKey(authorKey)
                .orElseGet(() -> {
                    Developer developer = new Developer();
                    developer.setAuthorKey(authorKey);
                    developer.setDisplayName(formatDisplayName(authorKey));
                    developer.setIsActive(true);

                    developer = developerRepository.save(developer);
                    log.debug("Created developer: {} ({})", developer.getDisplayName(), authorKey);

                    return developer;
                });
    }

    /**
     * Sync project metrics for today
     */
    private void syncProjectMetrics(Project project) {
        LocalDate today = LocalDate.now();

        SonarMeasuresResponse response = sonarQubeClient.getProjectMeasures(project.getProjectKey());
        if (response == null || response.getComponent() == null || response.getComponent().getMeasures() == null) {
            log.warn("No measures found for project: {}", project.getProjectKey());
            return;
        }

        ProjectMetricsDaily metrics = projectMetricsDailyRepository
                .findByProjectAndDateRecorded(project, today)
                .orElse(new ProjectMetricsDaily());

        boolean isNew = metrics.getId() == null;
        metrics.setProject(project);
        metrics.setDateRecorded(today);

        // Parse measures into a map
        Map<String, String> measureMap = response.getComponent().getMeasures().stream()
                .collect(Collectors.toMap(
                        SonarMeasuresResponse.Measure::getMetric,
                        SonarMeasuresResponse.Measure::getValue,
                        (v1, v2) -> v1
                ));

        metrics.setLinesOfCode(parseInt(measureMap.get("ncloc"), 0));
        metrics.setBugsCount(parseInt(measureMap.get("bugs"), 0));
        metrics.setVulnerabilitiesCount(parseInt(measureMap.get("vulnerabilities"), 0));
        metrics.setCodeSmellsCount(parseInt(measureMap.get("code_smells"), 0));
        metrics.setReliabilityRating(parseDoubledouble(measureMap.get("reliability_rating"), 0.0));
        metrics.setSecurityRating(parseDoubledouble(measureMap.get("security_rating"), 0.0));
        metrics.setMaintainabilityRating(parseDoubledouble(measureMap.get("sqale_rating"), 0.0));

        projectMetricsDailyRepository.save(metrics);

        log.debug("{} project metrics for {} - LOC: {}, Bugs: {}, Vulnerabilities: {}, Code Smells: {}",
                isNew ? "Created" : "Updated",
                project.getProjectKey(),
                metrics.getLinesOfCode(),
                metrics.getBugsCount(),
                metrics.getVulnerabilitiesCount(),
                metrics.getCodeSmellsCount());
    }

    /**
     * Calculate developer metrics for a specific date
     */
    private void calculateDeveloperMetrics(Project project, LocalDate date) {
        LocalDateTime dateStart = date.atStartOfDay();
        LocalDateTime dateEnd = date.plusDays(1).atStartOfDay();

        // Get all issues for this project
        List<Issue> allIssues = issueRepository.findByProjectAndCreatedDateAfter(project, dateStart.minusMonths(3));

        // Group by developer
        Map<Integer, DeveloperMetricsDaily> metricsMap = new HashMap<>();

        for (Issue issue : allIssues) {
            if (issue.getDeveloper() == null) continue;

            Integer devId = issue.getDeveloper().getId();
            DeveloperMetricsDaily metrics = metricsMap.computeIfAbsent(devId, id -> {
                DeveloperMetricsDaily dm = developerMetricsDailyRepository
                        .findByDeveloperAndProjectAndDateRecorded(
                                issue.getDeveloper(), project, date)
                        .orElse(new DeveloperMetricsDaily());
                dm.setDeveloper(issue.getDeveloper());
                dm.setProject(project);
                dm.setDateRecorded(date);
                return dm;
            });

            // Count violations introduced on this date
            if (issue.getCreatedDate() != null &&
                    issue.getCreatedDate().toLocalDate().equals(date)) {

                metrics.setViolationsIntroduced(metrics.getViolationsIntroduced() + 1);

                switch (issue.getIssueType()) {
                    case BUG:
                        metrics.setBugsIntroduced(metrics.getBugsIntroduced() + 1);
                        break;
                    case VULNERABILITY:
                        metrics.setVulnerabilitiesIntroduced(
                                metrics.getVulnerabilitiesIntroduced() + 1);
                        break;
                    case CODE_SMELL:
                        metrics.setCodeSmellsIntroduced(metrics.getCodeSmellsIntroduced() + 1);
                        break;
                }

                // Estimate LOC contributed (rough estimate: 50 lines per issue)
                metrics.setLinesOfCodeContributed(metrics.getLinesOfCodeContributed() + 50);
            }

            // Count violations resolved on this date
            if (issue.getResolvedDate() != null &&
                    issue.getResolvedDate().toLocalDate().equals(date)) {
                metrics.setViolationsResolved(metrics.getViolationsResolved() + 1);
            }
        }

        // Save all metrics
        if (!metricsMap.isEmpty()) {
            metricsMap.values().forEach(developerMetricsDailyRepository::save);
            log.debug("Calculated metrics for {} developers on {}", metricsMap.size(), date);
        }
    }

    // ============ HELPER METHODS ============

    private SyncLog createSyncLog(Project project, boolean fullSync) {
        SyncLog syncLog = new SyncLog();
        syncLog.setProject(project);
        syncLog.setSyncType(fullSync ? SyncLog.SyncType.FULL : SyncLog.SyncType.INCREMENTAL);
        syncLog.setStatus(SyncLog.SyncStatus.STARTED);
        return syncLogRepository.save(syncLog);
    }

    private String calculateSyncDateRange(Project project, boolean fullSync) {
        if (!fullSync) {
            LocalDateTime lastSync = getLastSuccessfulSyncDate(project);
            if (lastSync != null) {
                return lastSync.format(ISO_FORMATTER);
            }
        }

        // Full sync or no previous sync - get last N days
        return LocalDateTime.now()
                .minusDays(historicalDays)
                .format(ISO_FORMATTER);
    }

    private LocalDateTime getLastSuccessfulSyncDate(Project project) {
        return syncLogRepository.findLastSuccessfulSync(project)
                .map(SyncLog::getEndTime)
                .orElse(null);
    }

    private String formatDisplayName(String authorKey) {
        // Convert "john.doe" to "John Doe"
        if (authorKey == null || authorKey.isEmpty()) {
            return "Unknown";
        }

        String[] parts = authorKey.split("[._-]");
        StringBuilder displayName = new StringBuilder();

        for (String part : parts) {
            if (part.length() > 0) {
                displayName.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return displayName.toString().trim();
    }

    private Issue.Severity parseSeverity(String severity) {
        if (severity == null || severity.isEmpty()) {
            return Issue.Severity.MAJOR;
        }
        try {
            return Issue.Severity.valueOf(severity.toUpperCase());
        } catch (Exception e) {
            log.warn("Unknown severity: {}", severity);
            return Issue.Severity.MAJOR;
        }
    }

    private Issue.IssueType parseIssueType(String type) {
        if (type == null || type.isEmpty()) {
            return Issue.IssueType.CODE_SMELL;
        }
        try {
            return Issue.IssueType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            log.warn("Unknown issue type: {}", type);
            return Issue.IssueType.CODE_SMELL;
        }
    }

    private Issue.IssueStatus parseStatus(String status) {
        if (status == null || status.isEmpty()) {
            return Issue.IssueStatus.OPEN;
        }
        try {
            return Issue.IssueStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            log.warn("Unknown status: {}", status);
            return Issue.IssueStatus.OPEN;
        }
    }

    private Integer parseEffort(String effort) {
        if (effort == null || effort.isEmpty()) {
            return 0;
        }
        try {
            // Parse formats like "10min", "1h30min", "2d"
            String cleaned = effort.toLowerCase()
                    .replaceAll("min", "")
                    .replaceAll("h", "*60+")
                    .replaceAll("d", "*480+")
                    .replaceAll("\\+$", "");

            // Simple parsing - just extract numbers
            return Integer.parseInt(cleaned.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            log.trace("Could not parse effort: {}", effort);
            return 0;
        }
    }

    private Integer parseInt(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            log.trace("Could not parse int: {}", value);
            return defaultValue;
        }
    }

    private Double parseDouble(String value, double defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            log.trace("Could not parse double: {}", value);
            return defaultValue;
        }
    }

    private BigDecimal parseDoubledouble(String value, double defaultValue) {
        if (value == null || value.isEmpty()) {
            return BigDecimal.valueOf(defaultValue);
        }
        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            log.trace("Could not parse BigDecimal: {}", value);
            return BigDecimal.valueOf(defaultValue);
        }
    }

}
