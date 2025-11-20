package lk.sampath.leaderboard.services;

import lk.sampath.leaderboard.client.SonarQubeClient;
import lk.sampath.leaderboard.entity.*;
import lk.sampath.leaderboard.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingCalculationService {

    private final SonarQubeClient sonarClient;
    private final ProjectRepository projectRepository;
    private final DeveloperRepository developerRepository;
    private final IssueRepository issueRepository;
    private final IndividualRankingRepository individualRankingRepository;
    private final ProjectRankingRepository projectRankingRepository;
    private final KlocCalculationService klocCalculationService;

    private static final BigDecimal THOUSAND = new BigDecimal("1000");

    @Transactional
    public void calculateMonthlyRankings(LocalDate month) {
        log.info("Starting ranking calculation for {}", month);

        LocalDate startOfMonth = month.withDayOfMonth(1);
        LocalDate endOfMonth = month.withDayOfMonth(month.lengthOfMonth());
        LocalDateTime startDateTime = startOfMonth.atStartOfDay();
        LocalDateTime endDateTime = endOfMonth.atTime(23, 59, 59);

        // Calculate individual rankings
        calculateIndividualRankings(month, startDateTime, endDateTime);

        // Calculate project rankings
        calculateProjectRankings(month);

        log.info("Completed ranking calculation for {}", month);
    }

    private void calculateIndividualRankings(LocalDate month, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<Developer> activeDevelopers = developerRepository.findByIsActiveTrue();
        List<IndividualRanking> rankings = new ArrayList<>();

        for (Developer developer : activeDevelopers) {
            IndividualRanking ranking = individualRankingRepository
                    .findByDeveloperAndRankingPeriod(developer, month)
                    .orElseGet(() -> IndividualRanking.builder()
                            .developer(developer)
                            .rankingPeriod(month)
                            .build());

            // Calculate Defect Terminator metrics
            Long resolved = issueRepository.countResolvedIssues(developer.getId(), startDateTime, endDateTime);
            Long introduced = issueRepository.countIntroducedIssues(developer.getId(), startDateTime, endDateTime);
            ranking.setViolationsResolved(resolved.intValue());
            ranking.setViolationsIntroduced(introduced.intValue());
            ranking.setDefectTerminatorScore(resolved.intValue() - introduced.intValue());

            // todo ranking
            ranking.setTotalKloc(BigDecimal.ZERO);

            // Calculate Code Rock (Total open bugs - lower is better)
            Long bugCount = issueRepository.countOpenIssuesByDeveloperAndType(
                    developer.getId(), Issue.IssueType.BUG);
            ranking.setBugsPerKloc(BigDecimal.valueOf(bugCount)); // Using as absolute count
            ranking.setCodeRockScore(BigDecimal.valueOf(bugCount));

            // Calculate Code Shield (Total open vulnerabilities - lower is better)
            Long vulnCount = issueRepository.countOpenIssuesByDeveloperAndType(
                    developer.getId(), Issue.IssueType.VULNERABILITY);
            ranking.setVulnerabilitiesPerKloc(BigDecimal.valueOf(vulnCount)); // Using as absolute count
            ranking.setCodeShieldScore(BigDecimal.valueOf(vulnCount));

            // Calculate Craftsman (Total open code smells - lower is better)
            Long smellCount = issueRepository.countOpenIssuesByDeveloperAndType(
                    developer.getId(), Issue.IssueType.CODE_SMELL);
            ranking.setCodeSmellsPerKloc(BigDecimal.valueOf(smellCount)); // Using as absolute count
            ranking.setCraftsmanScore(BigDecimal.valueOf(smellCount));

            rankings.add(ranking);
        }

        // Save all rankings first
        rankings = individualRankingRepository.saveAll(rankings);

        // Assign ranks
        assignIndividualRanks(rankings, month);
    }

    private void assignIndividualRanks(List<IndividualRanking> rankings, LocalDate month) {
        // Rank Defect Terminator (highest score wins)
        List<IndividualRanking> sortedByDefect = rankings.stream()
                .sorted(Comparator.comparing(IndividualRanking::getDefectTerminatorScore).reversed())
                .collect(Collectors.toList());
        for (int i = 0; i < sortedByDefect.size(); i++) {
            sortedByDefect.get(i).setDefectTerminatorRank(i + 1);
        }

        // Rank Code Rock (lowest score wins)
        List<IndividualRanking> sortedByRock = rankings.stream()
                .sorted(Comparator.comparing(IndividualRanking::getCodeRockScore))
                .collect(Collectors.toList());
        for (int i = 0; i < sortedByRock.size(); i++) {
            sortedByRock.get(i).setCodeRockRank(i + 1);
        }

        // Rank Code Shield (lowest score wins)
        List<IndividualRanking> sortedByShield = rankings.stream()
                .sorted(Comparator.comparing(IndividualRanking::getCodeShieldScore))
                .collect(Collectors.toList());
        for (int i = 0; i < sortedByShield.size(); i++) {
            sortedByShield.get(i).setCodeShieldRank(i + 1);
        }

        // Rank Craftsman (lowest score wins)
        List<IndividualRanking> sortedByCraftsman = rankings.stream()
                .sorted(Comparator.comparing(IndividualRanking::getCraftsmanScore))
                .collect(Collectors.toList());
        for (int i = 0; i < sortedByCraftsman.size(); i++) {
            sortedByCraftsman.get(i).setCraftsmanRank(i + 1);
        }

        individualRankingRepository.saveAll(rankings);
        // After assigning category ranks, compute the Climber metric comparing with previous month
        computeClimberScores(rankings, month);
    }

    /**
     * Compute climber score for each developer as the average positive improvement across
     * the four categories comparing previous month -> current month.
     * Formula: sum(max(prevRank - currRank, 0) for each category) / 4
     */
    private void computeClimberScores(List<IndividualRanking> rankings, LocalDate month) {
        LocalDate prevMonth = month.minusMonths(1);

        for (IndividualRanking current : rankings) {
            int improvementSum = 0;

            Optional<IndividualRanking> prevOpt = individualRankingRepository
                    .findByDeveloperAndRankingPeriod(current.getDeveloper(), prevMonth);

            if (prevOpt.isPresent()) {
                IndividualRanking prev = prevOpt.get();

                int prevDef = prev.getDefectTerminatorRank() != null ? prev.getDefectTerminatorRank() : 0;
                int currDef = current.getDefectTerminatorRank() != null ? current.getDefectTerminatorRank() : Integer.MAX_VALUE;
                improvementSum += Math.max(prevDef - currDef, 0);

                int prevRock = prev.getCodeRockRank() != null ? prev.getCodeRockRank() : 0;
                int currRock = current.getCodeRockRank() != null ? current.getCodeRockRank() : Integer.MAX_VALUE;
                improvementSum += Math.max(prevRock - currRock, 0);

                int prevShield = prev.getCodeShieldRank() != null ? prev.getCodeShieldRank() : 0;
                int currShield = current.getCodeShieldRank() != null ? current.getCodeShieldRank() : Integer.MAX_VALUE;
                improvementSum += Math.max(prevShield - currShield, 0);

                int prevCraft = prev.getCraftsmanRank() != null ? prev.getCraftsmanRank() : 0;
                int currCraft = current.getCraftsmanRank() != null ? current.getCraftsmanRank() : Integer.MAX_VALUE;
                improvementSum += Math.max(prevCraft - currCraft, 0);
            } else {
                // No previous data -> no improvement
                improvementSum = 0;
            }

            BigDecimal avgImprovement = new BigDecimal(improvementSum).divide(new BigDecimal(4), 2, RoundingMode.HALF_UP);
            current.setClimberScore(avgImprovement);
        }

        // Rank climbers: higher average improvement wins
        List<IndividualRanking> sortedByClimber = rankings.stream()
                .sorted(Comparator.comparing(IndividualRanking::getClimberScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        for (int i = 0; i < sortedByClimber.size(); i++) {
            sortedByClimber.get(i).setClimberRank(i + 1);
        }

        individualRankingRepository.saveAll(rankings);
    }

    private void calculateProjectRankings(LocalDate month) {
        List<Project> activeProjects = projectRepository.findByIsActiveTrue();
        List<ProjectRanking> rankings = new ArrayList<>();

        for (Project project : activeProjects) {
            ProjectRanking ranking = projectRankingRepository
                    .findByProjectAndRankingPeriod(project, month)
                    .orElseGet(() -> ProjectRanking.builder()
                            .project(project)
                            .rankingPeriod(month)
                            .build());

            // Get KLOC from SonarQube using the KLOC service
            BigDecimal kloc = klocCalculationService.getProjectKloc(project);
            ranking.setTotalKloc(kloc);

            // Calculate violations for the project
            Long bugCount = issueRepository.countOpenIssuesByType(project.getId(), Issue.IssueType.BUG);
            Long vulnCount = issueRepository.countOpenIssuesByType(project.getId(), Issue.IssueType.VULNERABILITY);
            Long smellCount = issueRepository.countOpenIssuesByType(project.getId(), Issue.IssueType.CODE_SMELL);

            // Total defects
            int totalDefects = bugCount.intValue() + vulnCount.intValue() + smellCount.intValue();
            ranking.setDefectTerminatorScore(totalDefects);

            // Code Rock Score (Bugs per KLOC)
            BigDecimal bugsPerKloc = calculatePerKloc(BigDecimal.valueOf(bugCount), kloc);
            ranking.setBugsPerKloc(bugsPerKloc);
            ranking.setCodeRockScore(bugsPerKloc);

            // Code Shield Score (Vulnerabilities per KLOC)
            BigDecimal vulnPerKloc = calculatePerKloc(BigDecimal.valueOf(vulnCount), kloc);
            ranking.setVulnerabilitiesPerKloc(vulnPerKloc);
            ranking.setCodeShieldScore(vulnPerKloc);

            // Craftsman Score (Code Smells per KLOC)
            BigDecimal smellsPerKloc = calculatePerKloc(BigDecimal.valueOf(smellCount), kloc);
            ranking.setCodeSmellsPerKloc(smellsPerKloc);
            ranking.setCraftsmanScore(smellsPerKloc);

            rankings.add(ranking);
        }

        // Save all rankings first
        rankings = projectRankingRepository.saveAll(rankings);

        // Assign ranks
        assignProjectRanks(rankings, month);
    }

    private void assignProjectRanks(List<ProjectRanking> rankings, LocalDate month) {
        // Rank Defect Terminator (lowest total defects wins)
        List<ProjectRanking> sortedByDefect = rankings.stream()
                .sorted(Comparator.comparing(ProjectRanking::getDefectTerminatorScore))
                .collect(Collectors.toList());
        for (int i = 0; i < sortedByDefect.size(); i++) {
            sortedByDefect.get(i).setDefectTerminatorRank(i + 1);
        }

        // Rank Code Rock (lowest score wins)
        List<ProjectRanking> sortedByRock = rankings.stream()
                .sorted(Comparator.comparing(ProjectRanking::getCodeRockScore))
                .collect(Collectors.toList());
        for (int i = 0; i < sortedByRock.size(); i++) {
            sortedByRock.get(i).setCodeRockRank(i + 1);
        }

        // Rank Code Shield (lowest score wins)
        List<ProjectRanking> sortedByShield = rankings.stream()
                .sorted(Comparator.comparing(ProjectRanking::getCodeShieldScore))
                .collect(Collectors.toList());
        for (int i = 0; i < sortedByShield.size(); i++) {
            sortedByShield.get(i).setCodeShieldRank(i + 1);
        }

        // Rank Craftsman (lowest score wins)
        List<ProjectRanking> sortedByCraftsman = rankings.stream()
                .sorted(Comparator.comparing(ProjectRanking::getCraftsmanScore))
                .collect(Collectors.toList());
        for (int i = 0; i < sortedByCraftsman.size(); i++) {
            sortedByCraftsman.get(i).setCraftsmanRank(i + 1);
        }

        projectRankingRepository.saveAll(rankings);
    }

    private BigDecimal calculatePerKloc(BigDecimal count, BigDecimal kloc) {
        if (kloc.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return count.divide(kloc, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal parseMetric(String value) {
        if (value == null || value.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
