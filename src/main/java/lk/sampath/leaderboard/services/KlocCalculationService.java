package lk.sampath.leaderboard.services;

import lk.sampath.leaderboard.client.SonarQubeClient;
import lk.sampath.leaderboard.entity.Developer;
import lk.sampath.leaderboard.entity.Issue;
import lk.sampath.leaderboard.entity.Project;
import lk.sampath.leaderboard.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KlocCalculationService {

    private final SonarQubeClient sonarClient;
    private final IssueRepository issueRepository;

    private static final BigDecimal THOUSAND = new BigDecimal("1000");

    /**
     * Get KLOC for a project from SonarQube
     */
    public BigDecimal getProjectKloc(Project project) {
        Map<String, String> metrics = sonarClient.fetchProjectMetrics(project.getProjectKey());
        String nclocValue = metrics.get("ncloc");

        if (nclocValue == null || nclocValue.isEmpty()) {
            log.warn("No KLOC data found for project: {}", project.getProjectKey());
            return BigDecimal.ZERO;
        }

        try {
            BigDecimal ncloc = new BigDecimal(nclocValue);
            return ncloc.divide(THOUSAND, 2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            log.error("Error parsing KLOC for project {}: {}", project.getProjectKey(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calculate developer's KLOC based on their contribution to projects
     * This uses the number of issues they've created as a proxy for their code contribution
     * More accurate would be to use SonarQube SCM data or Git commit analysis
     */
    public BigDecimal getDeveloperKloc(Developer developer, LocalDate month) {
        LocalDateTime startOfMonth = month.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = month.withDayOfMonth(month.lengthOfMonth()).atTime(23, 59, 59);

        // Get all issues created by this developer in the month
        List<Issue> developerIssues = issueRepository.findByDeveloperAndCreatedDateBetween(
                developer, startOfMonth, endOfMonth);

        if (developerIssues.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Group issues by project
        Map<Project, Long> issuesByProject = new HashMap<>();
        for (Issue issue : developerIssues) {
            issuesByProject.merge(issue.getProject(), 1L, Long::sum);
        }

        // Calculate weighted KLOC based on developer's contribution to each project
        BigDecimal totalKloc = BigDecimal.ZERO;

        for (Map.Entry<Project, Long> entry : issuesByProject.entrySet()) {
            Project project = entry.getKey();
            Long developerIssueCount = entry.getValue();

            // Get total issues in the project
            Long totalProjectIssues = issueRepository.countByProject(project.getId());

            if (totalProjectIssues > 0) {
                // Get project KLOC
                BigDecimal projectKloc = getProjectKloc(project);

                // Calculate developer's proportional KLOC
                // (developer's issues / total issues) * project KLOC
                BigDecimal contributionRatio = BigDecimal.valueOf(developerIssueCount)
                        .divide(BigDecimal.valueOf(totalProjectIssues), 4, RoundingMode.HALF_UP);

                BigDecimal developerProjectKloc = projectKloc.multiply(contributionRatio);
                totalKloc = totalKloc.add(developerProjectKloc);

                log.debug("Developer {} contributed {} issues out of {} in project {} (KLOC: {})",
                        developer.getAuthorKey(), developerIssueCount, totalProjectIssues,
                        project.getProjectKey(), developerProjectKloc);
            }
        }

        return totalKloc.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Alternative: Calculate developer KLOC using SonarQube SCM data
     * This is more accurate but requires the SCM plugin to be configured in SonarQube
     */
    public BigDecimal getDeveloperKlocFromScm(Developer developer, LocalDate month) {
        // This would require additional SonarQube API calls to the SCM endpoint
        // Example endpoint: /api/measures/component?component=PROJECT_KEY&metricKeys=ncloc&author=DEVELOPER
        // SonarQube doesn't directly expose per-author KLOC, so we use the issue-based approach above

        log.warn("SCM-based KLOC calculation not implemented. Using issue-based calculation instead.");
        return getDeveloperKloc(developer, month);
    }
}
