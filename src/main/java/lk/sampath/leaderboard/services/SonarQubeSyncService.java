package lk.sampath.leaderboard.services;


import lk.sampath.leaderboard.client.SonarQubeClient;
import lk.sampath.leaderboard.dto.SonarProjectSearchResponse;
import lk.sampath.leaderboard.dto.SonarIssuesSearchResponse;
import lk.sampath.leaderboard.dto.SyncResponse;
import lk.sampath.leaderboard.entity.Developer;
import lk.sampath.leaderboard.entity.Issue;
import lk.sampath.leaderboard.entity.Project;
import lk.sampath.leaderboard.repository.DeveloperRepository;
import lk.sampath.leaderboard.repository.IssueRepository;
import lk.sampath.leaderboard.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SonarQubeSyncService {

    private final SonarQubeClient sonarClient;
    private final ProjectRepository projectRepository;
    private final DeveloperRepository developerRepository;
    private final IssueRepository issueRepository;

    @Transactional
    public void syncMonthlyData(LocalDate month) {
        log.info("Starting monthly SonarQube data sync for {}", month);

        LocalDate startOfMonth = month.withDayOfMonth(1);
        LocalDate endOfMonth = month.withDayOfMonth(month.lengthOfMonth());

        // Sync projects
        List<SonarProjectSearchResponse.Component> sonarProjects = sonarClient.fetchAllProjects();
        Map<String, Project> projects = syncProjects(sonarProjects);

        // Sync issues for each project
        for (Project project : projects.values()) {
            syncIssuesForProject(project, startOfMonth, endOfMonth);
        }

        log.info("Completed monthly SonarQube data sync for {}", month);
    }

    private Map<String, Project> syncProjects(List<SonarProjectSearchResponse.Component> sonarProjects) {
        Map<String, Project> projects = new HashMap<>();

        for (SonarProjectSearchResponse.Component sonarProject : sonarProjects) {
            Project project = projectRepository.findByProjectKey(sonarProject.getKey())
                    .orElseGet(() -> Project.builder()
                            .projectKey(sonarProject.getKey())
                            .projectName(sonarProject.getName())
                            .isActive(true)
                            .build());

            project.setProjectName(sonarProject.getName());
            project = projectRepository.save(project);
            projects.put(project.getProjectKey(), project);

            log.debug("Synced project: {}", project.getProjectKey());
        }

        return projects;
    }

    private int syncIssuesForProject(Project project, LocalDate startDate, LocalDate endDate) {
        log.info("Syncing issues for project: {}", project.getProjectKey());

        var sonarIssues = sonarClient.fetchIssuesForProject(
                project.getProjectKey(),
                startDate,
                endDate
        );

        Set<String> authors = new HashSet<>();
        int processed = 0;

        for (var sonarIssue : sonarIssues) {
            try {
                syncIssue(sonarIssue, project);
                if (sonarIssue.getAuthor() != null && !sonarIssue.getAuthor().isEmpty()) {
                    authors.add(sonarIssue.getAuthor());
                }
                processed++;
            } catch (Exception e) {
                log.error("Error syncing issue {}: {}", sonarIssue.getKey(), e.getMessage());
            }
        }

        // Sync developers
        for (String authorKey : authors) {
            syncDeveloper(authorKey);
        }

        log.info("Synced {} issues for project {}", processed, project.getProjectKey());
        return processed;
    }

    public SyncResponse syncAllProjects(boolean fullSync) {
        log.info("Starting full SonarQube sync - fullSync: {}", fullSync);
        long start = System.currentTimeMillis();
        try {
            List<SonarProjectSearchResponse.Component> sonarProjects = sonarClient.fetchAllProjects();
            Map<String, Project> projects = syncProjects(sonarProjects);

            int totalProjects = projects.size();
            int totalIssues = 0;

            // Use last 1 month range for sync by default
            LocalDate startDate = LocalDate.now().minusMonths(1).withDayOfMonth(1);
            LocalDate endDate = LocalDate.now();

            for (Project project : projects.values()) {
                try {
                    totalIssues += syncIssuesForProject(project, startDate, endDate);
                } catch (Exception e) {
                    log.error("Error syncing project {}: {}", project.getProjectKey(), e.getMessage());
                }
            }

            SyncResponse.SyncStats stats = new SyncResponse.SyncStats();
            stats.setProjectsProcessed(totalProjects);
            stats.setIssuesCreated(totalIssues);
            stats.setDevelopersCreated(0);
            stats.setIssuesUpdated(0);
            stats.setMetricsCreated(0);
            stats.setDurationMs(System.currentTimeMillis() - start);

            return SyncResponse.success("Full sync completed", null, stats);
        } catch (Exception e) {
            log.error("Full sync failed", e);
            return SyncResponse.failure("Full sync failed: " + e.getMessage());
        }
    }

    // Changed parameter type to SonarIssuesSearchResponse.IssueDetail and fixed mapping
    private void syncIssue(SonarIssuesSearchResponse.IssueDetail sonarIssue, Project project) {
        Issue issue = issueRepository.findByIssueKey(sonarIssue.getKey())
                .orElseGet(() -> {
                    Issue newIssue = new Issue();
                    newIssue.setIssueKey(sonarIssue.getKey());
                    return newIssue;
                });

        issue.setIssueKey(sonarIssue.getKey());
        issue.setProject(project);
        issue.setRuleKey(sonarIssue.getRule());
        issue.setSeverity(parseSeverity(sonarIssue.getSeverity()));
        issue.setIssueType(parseIssueType(sonarIssue.getType()));
        issue.setStatus(parseStatus(sonarIssue.getStatus()));
        issue.setComponentPath(sonarIssue.getComponent());
        issue.setLineNumber(sonarIssue.getLine());
        issue.setMessage(sonarIssue.getMessage());
        issue.setEffortMinutes(parseEffort(sonarIssue.getEffort()));

        // Parse dates from Sonar DTO fields (may be null)
        issue.setCreatedDate(parseDateTime(sonarIssue.getCreationDate()));
        issue.setUpdatedDate(parseDateTime(sonarIssue.getUpdateDate()));
        issue.setResolvedDate(parseDateTime(sonarIssue.getCloseDate()));

        if (sonarIssue.getAuthor() != null && !sonarIssue.getAuthor().isEmpty()) {
            Developer developer = syncDeveloper(sonarIssue.getAuthor());
            issue.setDeveloper(developer);
        }

        issueRepository.save(issue);
    }

    private Developer syncDeveloper(String authorKey) {
        return developerRepository.findByAuthorKey(authorKey)
                .orElseGet(() -> {
                    String displayName = sonarClient.fetchUserDisplayName(authorKey).orElse(authorKey);
                    Developer developer = Developer.builder()
                            .authorKey(authorKey)
                            .displayName(displayName)
                            .isActive(true)
                            .build();
                    return developerRepository.save(developer);
                });
    }

    private Issue.Severity parseSeverity(String severity) {
        try {
            return Issue.Severity.valueOf(severity.toUpperCase());
        } catch (Exception e) {
            return Issue.Severity.MAJOR;
        }
    }

    private Issue.IssueType parseIssueType(String type) {
        try {
            return Issue.IssueType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            return Issue.IssueType.CODE_SMELL;
        }
    }

    private Issue.IssueStatus parseStatus(String status) {
        try {
            return Issue.IssueStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            return Issue.IssueStatus.OPEN;
        }
    }

    private Integer parseEffort(String effort) {
        if (effort == null || effort.isEmpty()) {
            return 0;
        }
        try {
            // Effort is in format like "5min", "1h", etc.
            effort = effort.toLowerCase().replace("min", "").replace("h", "").trim();
            return Integer.parseInt(effort);
        } catch (Exception e) {
            return 0;
        }
    }

    private LocalDateTime parseDateTime(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse datetime: {}", dateTime);
            return null;
        }
    }
}