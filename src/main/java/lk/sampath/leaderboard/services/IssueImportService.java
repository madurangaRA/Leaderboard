package lk.sampath.leaderboard.services;

import lk.sampath.leaderboard.client.SonarQubeClient;
import lk.sampath.leaderboard.dto.SonarIssuesSearchResponse;
import lk.sampath.leaderboard.entity.Developer;
import lk.sampath.leaderboard.entity.Issue;
import lk.sampath.leaderboard.entity.Project;
import lk.sampath.leaderboard.repository.DeveloperRepository;
import lk.sampath.leaderboard.repository.IssueRepository;
import lk.sampath.leaderboard.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Dedicated service for importing issues from SonarQube API
 * This handles the complete issue import workflow
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueImportService {

    private final SonarQubeClient sonarQubeClient;
    private final ProjectRepository projectRepository;
    private final DeveloperRepository developerRepository;
    private final IssueRepository issueRepository;

    @Value("${sonarqube.sync.request-delay-ms:100}")
    private long requestDelayMs;

    @Value("${sonarqube.sync.historical-days:90}")
    private int historicalDays;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Import all issues for all projects
     *
     * @param fullSync If true, import last N days. If false, import from last sync.
     * @return Summary of import results
     */
//    @Transactional
//    public IssueImportResult importAllIssues(boolean fullSync) {
//        log.info("========================================");
//        log.info("Starting issue import - fullSync: {}", fullSync);
//        log.info("========================================");
//
//        IssueImportResult result = new IssueImportResult();
//
//        long startTime = System.currentTimeMillis();
//
//        try {
//            // Get all active projects
//            List<Project> projects = projectRepository.findByIsActiveTrue();
//
//            if (projects.isEmpty()) {
//                log.warn("No active projects found. Run project sync first.");
//                result.setSuccess(false);
//                result.setMessage("No projects found");
//                return result;
//            }
//
//            log.info("Found {} active projects", projects.size());
//            result.setProjectsProcessed(projects.size());
//
//            // Import issues for each project
//            for (Project project : projects) {
//                try {
//                    log.info("Importing issues for project: {}", project.getProjectKey());
//
//                    ProjectIssueImportResult projectResult = importProjectIssues(
//                            project, fullSync);
//
//                    result.addIssuesCreated(projectResult.getIssuesCreated());
//                    result.addIssuesUpdated(projectResult.getIssuesUpdated());
//                    result.addDevelopersCreated(projectResult.getDevelopersCreated());
//
//                    log.info("✓ Project {} complete - Created: {}, Updated: {}",
//                            project.getProjectKey(),
//                            projectResult.getIssuesCreated(),
//                            projectResult.getIssuesUpdated());
//
//                    // Delay between projects
//                    if (requestDelayMs > 0) {
//                        Thread.sleep(requestDelayMs);
//                    }
//
//                } catch (Exception e) {
//                    log.error("Error importing issues for project {}: {}",
//                            project.getProjectKey(), e.getMessage(), e);
//                    result.addError(project.getProjectKey(), e.getMessage());
//                }
//            }
//
//            result.setSuccess(true);
//            result.setDurationMs(System.currentTimeMillis() - startTime);
//            result.setMessage("Import completed successfully");
//
//            log.info("========================================");
//            log.info("✓ Issue import completed successfully");
//            log.info("  Projects: {}", result.getProjectsProcessed());
//            log.info("  Issues Created: {}", result.getIssuesCreated());
//            log.info("  Issues Updated: {}", result.getIssuesUpdated());
//            log.info("  Developers Created: {}", result.getDevelopersCreated());
//            log.info("  Duration: {}ms", result.getDurationMs());
//            log.info("========================================");
//
//        } catch (Exception e) {
//            log.error("Issue import failed", e);
//            result.setSuccess(false);
//            result.setMessage("Import failed: " + e.getMessage());
//            result.setDurationMs(System.currentTimeMillis() - startTime);
//        }
//
//        return result;
//    }

//    /**
//     * Import issues for a single project
//     *
//     * @param project The project to import issues for
//     * @param fullSync If true, import all issues from historical period
//     * @return Import results for this project
//     */
//    @Transactional
//    public ProjectIssueImportResult importProjectIssues(Project project, boolean fullSync) {
//        ProjectIssueImportResult result = new ProjectIssueImportResult();
//        result.setProjectKey(project.getProjectKey());
//
//        // Determine date range
//        String createdAfter = calculateDateRange(fullSync);
//
//        log.info("Importing issues created after: {}",
//                createdAfter != null ? createdAfter : "beginning");
//
//        Set<String> newDevelopers = new HashSet<>();
//        int page = 1;
//        boolean hasMore = true;
//        int totalIssues = 0;
//
//        while (hasMore) {
//            try {
//                log.debug("Fetching issues page {} for project {}", page, project.getProjectKey());
//
//                // Call SonarQube API
//                SonarIssuesSearchResponse response = sonarQubeClient.(
//                        project.getProjectKey(),
//                        page,
//                        createdAfter,
//                        null
//                );
//
//                if (response == null || response.getIssues() == null) {
//                    log.warn("No response from SonarQube for page {}", page);
//                    break;
//                }
//
//                log.debug("Retrieved {} issues from page {}",
//                        response.getIssues().size(), page);
//
//                // Process each issue
//                for (SonarIssuesSearchResponse.IssueDetail issueDetail : response.getIssues()) {
//                    try {
//                        boolean isNew = importSingleIssue(issueDetail, project);
//
//                        if (isNew) {
//                            result.incrementIssuesCreated();
//                        } else {
//                            result.incrementIssuesUpdated();
//                        }
//
//                        totalIssues++;
//
//                        // Track new developers
//                        if (issueDetail.getAuthor() != null &&
//                                !issueDetail.getAuthor().isEmpty()) {
//                            if (isNewDeveloper(issueDetail.getAuthor())) {
//                                newDevelopers.add(issueDetail.getAuthor());
//                            }
//                        }
//
//                    } catch (Exception e) {
//                        log.error("Error importing issue {}: {}",
//                                issueDetail.getKey(), e.getMessage());
//                        result.addError(issueDetail.getKey(), e.getMessage());
//                    }
//                }
//
//                // Check pagination
//                int currentTotal = response.getPaging().getPageIndex() *
//                        response.getPaging().getPageSize();
//                hasMore = currentTotal < response.getPaging().getTotal();
//
//                if (hasMore) {
//                    log.debug("More issues available. Progress: {}/{}",
//                            currentTotal, response.getPaging().getTotal());
//                }
//
//                page++;
//
//                // Delay between API calls
//                if (hasMore && requestDelayMs > 0) {
//                    Thread.sleep(requestDelayMs);
//                }
//
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                log.warn("Import interrupted");
//                break;
//            } catch (Exception e) {
//                log.error("Error fetching issues page {}: {}", page, e.getMessage());
//                break;
//            }
//        }
//
//        result.setDevelopersCreated(newDevelopers.size());
//
//        log.info("Project {} import complete - {} issues processed",
//                project.getProjectKey(), totalIssues);
//
//        return result;
//    }

    /**
     * Import a single issue from SonarQube
     *
     * @param issueDetail Issue details from API
     * @param project Project this issue belongs to
     * @return true if issue was created (new), false if updated (existing)
     */
    @Transactional
    public boolean importSingleIssue(SonarIssuesSearchResponse.IssueDetail issueDetail,
                                     Project project) {

        // Find existing issue or create new
        Issue issue = issueRepository.findByIssueKey(issueDetail.getKey())
                .orElse(new Issue());

        boolean isNew = issue.getId() == null;

        // Map API data to entity
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
        issue.setCreatedDate(parseDateTime(issueDetail.getCreationDate()));
        issue.setUpdatedDate(parseDateTime(issueDetail.getUpdateDate()));
        issue.setResolvedDate(parseDateTime(issueDetail.getCloseDate()));

        // Handle developer/author
        if (issueDetail.getAuthor() != null && !issueDetail.getAuthor().isEmpty()) {
            Developer developer = getOrCreateDeveloper(issueDetail.getAuthor());
            issue.setDeveloper(developer);
        }

        // Save to database
        issueRepository.save(issue);

        log.trace("{} issue: {} (Type: {}, Severity: {})",
                isNew ? "Created" : "Updated",
                issueDetail.getKey(),
                issueDetail.getType(),
                issueDetail.getSeverity());

        return isNew;
    }

    /**
     * Get existing developer or create new one
     */
    private Developer getOrCreateDeveloper(String authorKey) {
        return developerRepository.findByAuthorKey(authorKey)
                .orElseGet(() -> {
                    Developer developer = new Developer();
                    developer.setAuthorKey(authorKey);
                    // Prefer exact display name from SonarQube when available
                    String displayName = sonarQubeClient.fetchUserDisplayName(authorKey)
                            .orElse(formatDisplayName(authorKey));
                    developer.setDisplayName(displayName);
                    developer.setIsActive(true);

                    developer = developerRepository.save(developer);
                    log.debug("Created new developer: {} ({})",
                            developer.getDisplayName(), authorKey);

                    return developer;
                });
    }

    /**
     * Check if developer is new (doesn't exist in DB)
     */
    private boolean isNewDeveloper(String authorKey) {
        return !developerRepository.existsByAuthorKey(authorKey);
    }

    /**
     * Calculate date range for API query
     */
    private String calculateDateRange(boolean fullSync) {
        if (fullSync || historicalDays > 0) {
            // Full sync - get last N days
            LocalDateTime since = LocalDateTime.now().minusDays(historicalDays);
            return since.format(ISO_FORMATTER);
        }

        // Incremental sync - could check last sync time here
        // For now, return null to get all issues
        return null;
    }

    /**
     * Format author key into display name
     * Example: "john.doe" -> "John Doe"
     */
    private String formatDisplayName(String authorKey) {
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

    // ============ PARSING HELPER METHODS ============

    private Issue.Severity parseSeverity(String severity) {
        if (severity == null || severity.isEmpty()) {
            return Issue.Severity.MAJOR;
        }
        try {
            return Issue.Severity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown severity: {}, defaulting to MAJOR", severity);
            return Issue.Severity.MAJOR;
        }
    }

    private Issue.IssueType parseIssueType(String type) {
        if (type == null || type.isEmpty()) {
            return Issue.IssueType.CODE_SMELL;
        }
        try {
            return Issue.IssueType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown issue type: {}, defaulting to CODE_SMELL", type);
            return Issue.IssueType.CODE_SMELL;
        }
    }

    private Issue.IssueStatus parseStatus(String status) {
        if (status == null || status.isEmpty()) {
            return Issue.IssueStatus.OPEN;
        }
        try {
            return Issue.IssueStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown status: {}, defaulting to OPEN", status);
            return Issue.IssueStatus.OPEN;
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, ISO_FORMATTER);
        } catch (Exception e) {
            log.warn("Could not parse date: {}", dateTimeStr);
            return null;
        }
    }

    private Integer parseEffort(String effort) {
        if (effort == null || effort.isEmpty()) {
            return 0;
        }
        try {
            // Parse formats like "10min", "1h", "2d"
            String cleaned = effort.toLowerCase()
                    .replaceAll("min", "")
                    .replaceAll("h", "")
                    .replaceAll("d", "")
                    .trim();

            return Integer.parseInt(cleaned);
        } catch (Exception e) {
            log.trace("Could not parse effort: {}", effort);
            return 0;
        }
    }

    // ============ RESULT CLASSES ============

    /**
     * Result summary for complete import operation
     */
    public static class IssueImportResult {
        private boolean success;
        private String message;
        private int projectsProcessed;
        private int issuesCreated;
        private int issuesUpdated;
        private int developersCreated;
        private long durationMs;
        private Map<String, String> errors = new HashMap<>();

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public int getProjectsProcessed() { return projectsProcessed; }
        public void setProjectsProcessed(int projectsProcessed) {
            this.projectsProcessed = projectsProcessed;
        }

        public int getIssuesCreated() { return issuesCreated; }
        public void addIssuesCreated(int count) { this.issuesCreated += count; }

        public int getIssuesUpdated() { return issuesUpdated; }
        public void addIssuesUpdated(int count) { this.issuesUpdated += count; }

        public int getDevelopersCreated() { return developersCreated; }
        public void addDevelopersCreated(int count) { this.developersCreated += count; }

        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

        public Map<String, String> getErrors() { return errors; }
        public void addError(String key, String error) { this.errors.put(key, error); }

        public int getTotalIssues() { return issuesCreated + issuesUpdated; }
        public boolean hasErrors() { return !errors.isEmpty(); }
    }

    /**
     * Result for single project import
     */
    public static class ProjectIssueImportResult {
        private String projectKey;
        private int issuesCreated;
        private int issuesUpdated;
        private int developersCreated;
        private Map<String, String> errors = new HashMap<>();

        public String getProjectKey() { return projectKey; }
        public void setProjectKey(String projectKey) { this.projectKey = projectKey; }

        public int getIssuesCreated() { return issuesCreated; }
        public void incrementIssuesCreated() { this.issuesCreated++; }

        public int getIssuesUpdated() { return issuesUpdated; }
        public void incrementIssuesUpdated() { this.issuesUpdated++; }

        public int getDevelopersCreated() { return developersCreated; }
        public void setDevelopersCreated(int developersCreated) {
            this.developersCreated = developersCreated;
        }

        public Map<String, String> getErrors() { return errors; }
        public void addError(String key, String error) { this.errors.put(key, error); }

        public int getTotalIssues() { return issuesCreated + issuesUpdated; }
    }
}
