package lk.sampath.leaderboard.services;

import lk.sampath.leaderboard.entity.*;
import lk.sampath.leaderboard.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 * Service for calculating monthly rankings and identifying champions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingCalculationService {

    private final DeveloperRepository developerRepository;
    private final ProjectRepository projectRepository;
    private final DeveloperMetricsDailyRepository developerMetricsDailyRepository;
    private final ProjectMetricsDailyRepository projectMetricsDailyRepository;
    private final IndividualRankingRepository individualRankingRepository;
    private final ProjectRankingRepository projectRankingRepository;
    private final MonthlyChampionRepository monthlyChampionRepository;

    /**
     * Calculate all rankings for a given month
     * This is the main entry point for Phase 3
     */
    @Transactional
    public void calculateMonthlyRankings(LocalDate monthStart) {
        log.info("========================================");
        log.info("Starting monthly ranking calculation for: {}", monthStart);
        log.info("========================================");

        // Ensure it's the first day of the month
        monthStart = monthStart.withDayOfMonth(1);

        try {
            // Step 1: Calculate Individual Rankings
            log.info("Step 1: Calculating individual rankings...");
            calculateIndividualRankings(monthStart);

            // Step 2: Calculate Project Rankings
            log.info("Step 2: Calculating project rankings...");
            calculateProjectRankings(monthStart);

            // Step 3: Identify and Store Champions
            log.info("Step 3: Identifying champions...");
            identifyMonthlyChampions(monthStart);

            log.info("========================================");
            log.info("✓ Monthly ranking calculation completed successfully");
            log.info("========================================");

        } catch (Exception e) {
            log.error("Error calculating monthly rankings", e);
            throw new RuntimeException("Failed to calculate monthly rankings", e);
        }
    }

    /**
     * Calculate rankings for all active developers
     */
    @Transactional
    public void calculateIndividualRankings(LocalDate rankingPeriod) {
        log.info("Calculating individual rankings for period: {}", rankingPeriod);

        LocalDate monthStart = rankingPeriod.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        // Get all active developers
        List<Developer> developers = developerRepository.findByIsActiveTrue();
        log.info("Processing {} developers", developers.size());

        // Calculate metrics for each developer
        List<IndividualRanking> rankings = new ArrayList<>();

        for (Developer developer : developers) {
            try {
                IndividualRanking ranking = calculateDeveloperRanking(developer, monthStart, monthEnd);
                if (ranking != null) {
                    rankings.add(ranking);
                }
            } catch (Exception e) {
                log.error("Error calculating ranking for developer {}: {}",
                        developer.getDisplayName(), e.getMessage());
            }
        }

        // Assign ranks for each category
        assignDefectTerminatorRanks(rankings);
        assignCodeRockRanks(rankings);
        assignCodeShieldRanks(rankings);
        assignCraftsmanRanks(rankings);
        assignClimberRanks(rankings, monthStart);

        // Save all rankings
        individualRankingRepository.saveAll(rankings);

        log.info("✓ Calculated rankings for {} developers", rankings.size());
    }

    /**
     * Calculate ranking for a single developer
     */
    private IndividualRanking calculateDeveloperRanking(Developer developer,
                                                        LocalDate monthStart,
                                                        LocalDate monthEnd) {
        // Get all metrics for this developer in the month
        List<DeveloperMetricsDaily> metrics = developerMetricsDailyRepository
                .findByDeveloperAndDateAfter(developer, monthStart);

        // Filter for the specific month
        metrics = metrics.stream()
                .filter(m -> !m.getDateRecorded().isBefore(monthStart) &&
                        !m.getDateRecorded().isAfter(monthEnd))
                .collect(Collectors.toList());

        if (metrics.isEmpty()) {
            log.debug("No metrics found for developer: {}", developer.getDisplayName());
            return null;
        }

        // Aggregate metrics
        int violationsIntroduced = metrics.stream()
                .mapToInt(DeveloperMetricsDaily::getViolationsIntroduced)
                .sum();

        int violationsResolved = metrics.stream()
                .mapToInt(DeveloperMetricsDaily::getViolationsResolved)
                .sum();

        int bugsIntroduced = metrics.stream()
                .mapToInt(DeveloperMetricsDaily::getBugsIntroduced)
                .sum();

        int vulnerabilitiesIntroduced = metrics.stream()
                .mapToInt(DeveloperMetricsDaily::getVulnerabilitiesIntroduced)
                .sum();

        int codeSmellsIntroduced = metrics.stream()
                .mapToInt(DeveloperMetricsDaily::getCodeSmellsIntroduced)
                .sum();

        int linesOfCode = metrics.stream()
                .mapToInt(DeveloperMetricsDaily::getLinesOfCodeContributed)
                .sum();

        double totalKLOC = linesOfCode / 1000.0;

        // Create ranking record
        IndividualRanking ranking = new IndividualRanking();
        ranking.setDeveloper(developer);
        ranking.setRankingPeriod(monthStart);

        // Defect Terminator Score
        ranking.setDefectTerminatorScore(violationsResolved - violationsIntroduced);
        //ranking.setViolationsIntroduced(violationsIntroduced);
        ranking.setViolationsResolved(violationsResolved);

        // Code Rock Score (bugs per KLOC)
        ranking.setBugsPerKloc(BigDecimal.valueOf(totalKLOC > 0 ? bugsIntroduced / totalKLOC : 0.0));
        ranking.setCodeRockScore(ranking.getBugsPerKloc());

        // Code Shield Score (vulnerabilities per KLOC)
        ranking.setVulnerabilitiesPerKloc(BigDecimal.valueOf(totalKLOC > 0 ? vulnerabilitiesIntroduced / totalKLOC : 0.0));
        ranking.setCodeShieldScore(ranking.getVulnerabilitiesPerKloc());

        // Craftsman Score (code smells per KLOC)
        ranking.setCodeSmellsPerKloc(BigDecimal.valueOf(totalKLOC > 0 ? codeSmellsIntroduced / totalKLOC : 0.0));
        ranking.setCraftsmanScore(ranking.getCodeSmellsPerKloc());

        //ranking.setTotalKloc(totalKLOC);

        log.debug("Calculated ranking for {}: defect={}, bugs/kloc={}, vuln/kloc={}, smells/kloc={}",
                developer.getDisplayName(),
                ranking.getDefectTerminatorScore(),
                ranking.getBugsPerKloc(),
                ranking.getVulnerabilitiesPerKloc(),
                ranking.getCodeSmellsPerKloc());

        return ranking;
    }

    /**
     * Calculate rankings for all active projects
     */
    @Transactional
    public void calculateProjectRankings(LocalDate rankingPeriod) {
        log.info("Calculating project rankings for period: {}", rankingPeriod);

        LocalDate monthStart = rankingPeriod.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        List<Project> projects = projectRepository.findByIsActiveTrue();
        log.info("Processing {} projects", projects.size());

        List<ProjectRanking> rankings = new ArrayList<>();

        for (Project project : projects) {
            try {
                ProjectRanking ranking = calculateSingleProjectRanking(project, monthStart, monthEnd);
                if (ranking != null) {
                    rankings.add(ranking);
                }
            } catch (Exception e) {
                log.error("Error calculating ranking for project {}: {}",
                        project.getProjectName(), e.getMessage());
            }
        }

        // Assign ranks
        assignProjectDefectTerminatorRanks(rankings);
        assignProjectCodeRockRanks(rankings);
        assignProjectCodeShieldRanks(rankings);
        assignProjectCraftsmanRanks(rankings);

        // Save all rankings
        projectRankingRepository.saveAll(rankings);

        log.info("✓ Calculated rankings for {} projects", rankings.size());
    }

    /**
     * Calculate ranking for a single project
     */
    private ProjectRanking calculateSingleProjectRanking(Project project,
                                                         LocalDate monthStart,
                                                         LocalDate monthEnd) {
        // Get developer metrics for this project
        List<DeveloperMetricsDaily> devMetrics = new ArrayList<>();
        for (LocalDate date = monthStart; !date.isAfter(monthEnd); date = date.plusDays(1)) {
            devMetrics.addAll(developerMetricsDailyRepository.findByProjectAndDate(project, date));
        }

        if (devMetrics.isEmpty()) {
            log.debug("No metrics found for project: {}", project.getProjectName());
            return null;
        }

        // Aggregate metrics
        int violationsIntroduced = devMetrics.stream()
                .mapToInt(DeveloperMetricsDaily::getViolationsIntroduced)
                .sum();

        int violationsResolved = devMetrics.stream()
                .mapToInt(DeveloperMetricsDaily::getViolationsResolved)
                .sum();

        int bugsIntroduced = devMetrics.stream()
                .mapToInt(DeveloperMetricsDaily::getBugsIntroduced)
                .sum();

        int vulnerabilitiesIntroduced = devMetrics.stream()
                .mapToInt(DeveloperMetricsDaily::getVulnerabilitiesIntroduced)
                .sum();

        int codeSmellsIntroduced = devMetrics.stream()
                .mapToInt(DeveloperMetricsDaily::getCodeSmellsIntroduced)
                .sum();

        int linesOfCode = devMetrics.stream()
                .mapToInt(DeveloperMetricsDaily::getLinesOfCodeContributed)
                .sum();

        double totalKLOC = linesOfCode / 1000.0;

        ProjectRanking ranking = new ProjectRanking();
        ranking.setProject(project);
        ranking.setRankingPeriod(monthStart);

        ranking.setDefectTerminatorScore(violationsResolved - violationsIntroduced);
        //ranking.setViolationsIntroduced(violationsIntroduced);
        ranking.setViolationsResolved(violationsResolved);

        ranking.setBugsPerKloc(BigDecimal.valueOf(totalKLOC > 0 ? bugsIntroduced / totalKLOC : 0.0));
        ranking.setCodeRockScore(ranking.getBugsPerKloc());

        ranking.setVulnerabilitiesPerKloc(BigDecimal.valueOf(totalKLOC > 0 ? vulnerabilitiesIntroduced / totalKLOC : 0.0));
        ranking.setCodeShieldScore(ranking.getVulnerabilitiesPerKloc());

        ranking.setCodeSmellsPerKloc(BigDecimal.valueOf(totalKLOC > 0 ? codeSmellsIntroduced / totalKLOC : 0.0));
        ranking.setCraftsmanScore(ranking.getCodeSmellsPerKloc());

        //ranking.setTotalKloc(totalKLOC);

        return ranking;
    }

    /**
     * Identify and store monthly champions
     */
    @Transactional
    public void identifyMonthlyChampions(LocalDate monthStart) {
        log.info("Identifying champions for: {}", monthStart);

        // Clear existing champions for this month
        monthlyChampionRepository.deleteByPeriod(monthStart);

        // Get all rankings for this month
        List<IndividualRanking> individualRankings = individualRankingRepository
                .findByRankingPeriod(monthStart);
        List<ProjectRanking> projectRankings = projectRankingRepository
                .findByRankingPeriod(monthStart);

        // Identify individual champions
        identifyDefectTerminatorChampion(individualRankings, monthStart);
        identifyCodeRockChampion(individualRankings, monthStart);
        identifyCodeShieldChampion(individualRankings, monthStart);
        identifyCraftsmanChampion(individualRankings, monthStart);
        identifyClimberChampion(individualRankings, monthStart);

        // Identify project champions
        identifyProjectDefectTerminatorChampion(projectRankings, monthStart);
        identifyProjectCodeRockChampion(projectRankings, monthStart);
        identifyProjectCodeShieldChampion(projectRankings, monthStart);
        identifyProjectCraftsmanChampion(projectRankings, monthStart);

        log.info("✓ Champions identified and stored");
    }

    // ============ RANKING ASSIGNMENT METHODS ============

    private void assignDefectTerminatorRanks(List<IndividualRanking> rankings) {
        rankings.sort(Comparator.comparingInt(IndividualRanking::getDefectTerminatorScore).reversed());
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setDefectTerminatorRank(i + 1);
        }
    }

    private void assignCodeRockRanks(List<IndividualRanking> rankings) {
        // Filter only developers with sufficient code (> 1 KLOC)
        List<IndividualRanking> qualified = rankings.stream()
                .filter(r -> r.getTotalKloc() >= 1.0)
                .sorted(Comparator.comparingDouble((ToDoubleFunction<? super IndividualRanking>) IndividualRanking::getCodeRockScore))
                .collect(Collectors.toList());

        for (int i = 0; i < qualified.size(); i++) {
            qualified.get(i).setCodeRockRank(i + 1);
        }

        // Assign high rank to unqualified
        rankings.stream()
                .filter(r -> r.getTotalKloc() < 1.0)
                .forEach(r -> r.setCodeRockRank(999));
    }

    private void assignCodeShieldRanks(List<IndividualRanking> rankings) {
        List<IndividualRanking> qualified = rankings.stream()
                .filter(r -> r.getTotalKloc() >= 1.0)
                .sorted(Comparator.comparingDouble((ToDoubleFunction<? super IndividualRanking>) IndividualRanking::getCodeShieldScore))
                .collect(Collectors.toList());

        for (int i = 0; i < qualified.size(); i++) {
            qualified.get(i).setCodeShieldRank(i + 1);
        }

        rankings.stream()
                .filter(r -> r.getTotalKloc() < 1.0)
                .forEach(r -> r.setCodeShieldRank(999));
    }

    private void assignCraftsmanRanks(List<IndividualRanking> rankings) {
        List<IndividualRanking> qualified = rankings.stream()
                .filter(r -> r.getTotalKloc() >= 1.0)
                .sorted(Comparator.comparingDouble(IndividualRanking::getCraftsmanScore))
                .collect(Collectors.toList());

        for (int i = 0; i < qualified.size(); i++) {
            qualified.get(i).setCraftsmanRank(i + 1);
        }

        rankings.stream()
                .filter(r -> r.getTotalKloc() < 1.0)
                .forEach(r -> r.setCraftsmanRank(999));
    }

    private void assignClimberRanks(List<IndividualRanking> rankings, LocalDate currentMonth) {
        LocalDate previousMonth = currentMonth.minusMonths(1);

        for (IndividualRanking current : rankings) {
            // Get previous month's ranking
            Optional<IndividualRanking> previousOpt = individualRankingRepository
                    .findByDeveloperAndRankingPeriod(current.getDeveloper(), previousMonth);

            if (previousOpt.isPresent()) {
                IndividualRanking previous = previousOpt.get();

                // Calculate average rank improvement
                double avgPreviousRank = (previous.getDefectTerminatorRank() +
                        previous.getCodeRockRank() +
                        previous.getCodeShieldRank() +
                        previous.getCraftsmanRank()) / 4.0;

                double avgCurrentRank = (current.getDefectTerminatorRank() +
                        current.getCodeRockRank() +
                        current.getCodeShieldRank() +
                        current.getCraftsmanRank()) / 4.0;

                double improvement = avgPreviousRank - avgCurrentRank;
                current.setAvgRankImprovement(BigDecimal.valueOf(improvement));
                current.setClimberScore(BigDecimal.valueOf(improvement));
            } else {
                // No previous data - neutral score
                current.setAvgRankImprovement(BigDecimal.valueOf(0.0));
                current.setClimberScore(BigDecimal.valueOf(0.0));
            }
        }

        // Assign ranks based on improvement
        rankings.sort(Comparator.comparingDouble(IndividualRanking::getClimberScore).reversed());
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setClimberRank(i + 1);
        }
    }

    // ============ PROJECT RANKING ASSIGNMENT ============

    private void assignProjectDefectTerminatorRanks(List<ProjectRanking> rankings) {
        rankings.sort(Comparator.comparingInt(ProjectRanking::getDefectTerminatorScore).reversed());
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setDefectTerminatorRank(i + 1);
        }
    }

    private void assignProjectCodeRockRanks(List<ProjectRanking> rankings) {
        List<ProjectRanking> qualified = rankings.stream()
                .filter(r -> r.getTotalKloc() >= 1.0)
                .sorted(Comparator.comparingDouble(ProjectRanking::getCodeRockScore))
                .collect(Collectors.toList());

        for (int i = 0; i < qualified.size(); i++) {
            qualified.get(i).setCodeRockRank(i + 1);
        }

        rankings.stream()
                .filter(r -> r.getTotalKloc() < 1.0)
                .forEach(r -> r.setCodeRockRank(999));
    }

    private void assignProjectCodeShieldRanks(List<ProjectRanking> rankings) {
        List<ProjectRanking> qualified = rankings.stream()
                .filter(r -> r.getTotalKloc() >= 1.0)
                .sorted(Comparator.comparingDouble((ToDoubleFunction<? super ProjectRanking>) ProjectRanking::getCodeShieldScore))
                .collect(Collectors.toList());

        for (int i = 0; i < qualified.size(); i++) {
            qualified.get(i).setCodeShieldRank(i + 1);
        }

        rankings.stream()
                .filter(r -> r.getTotalKloc() < 1.0)
                .forEach(r -> r.setCodeShieldRank(999));
    }

    private void assignProjectCraftsmanRanks(List<ProjectRanking> rankings) {
        List<ProjectRanking> qualified = rankings.stream()
                .filter(r -> r.getTotalKloc() >= 1.0)
                .sorted(Comparator.comparingDouble(ProjectRanking::getCraftsmanScore))
                .collect(Collectors.toList());

        for (int i = 0; i < qualified.size(); i++) {
            qualified.get(i).setCraftsmanRank(i + 1);
        }

        rankings.stream()
                .filter(r -> r.getTotalKloc() < 1.0)
                .forEach(r -> r.setCraftsmanRank(999));
    }

    // ============ CHAMPION IDENTIFICATION ============

    private void identifyDefectTerminatorChampion(List<IndividualRanking> rankings, LocalDate month) {
        rankings.stream()
                .filter(r -> r.getDefectTerminatorRank() == 1)
                .findFirst()
                .ifPresent(r -> saveChampion(
                        month,
                        MonthlyChampion.ChampionCategory.DEFECT_TERMINATOR,
                        MonthlyChampion.EntityType.INDIVIDUAL,
                        r.getDeveloper().getId(),
                        r.getDeveloper().getDisplayName(),
                        (double) r.getDefectTerminatorScore(),
                        String.format("{\"violations_resolved\": %d, \"violations_introduced\": %d}",
                                r.getViolationsResolved(), r.getViolationsIntroduced())
                ));
    }

    private void identifyCodeRockChampion(List<IndividualRanking> rankings, LocalDate month) {
        rankings.stream()
                .filter(r -> r.getCodeRockRank() == 1 && r.getTotalKloc() >= 1.0)
                .findFirst()
                .ifPresent(r -> saveChampion(
                        month,
                        MonthlyChampion.ChampionCategory.CODE_ROCK,
                        MonthlyChampion.EntityType.INDIVIDUAL,
                        r.getDeveloper().getId(),
                        r.getDeveloper().getDisplayName(),
                        r.getBugsPerKloc(),
                        String.format("{\"bugs_per_kloc\": %.4f, \"total_kloc\": %.2f}",
                                r.getBugsPerKloc(), r.getTotalKloc())
                ));
    }

    private void identifyCodeShieldChampion(List<IndividualRanking> rankings, LocalDate month) {
        rankings.stream()
                .filter(r -> r.getCodeShieldRank() == 1 && r.getTotalKloc() >= 1.0)
                .findFirst()
                .ifPresent(r -> saveChampion(
                        month,
                        MonthlyChampion.ChampionCategory.CODE_SHIELD,
                        MonthlyChampion.EntityType.INDIVIDUAL,
                        r.getDeveloper().getId(),
                        r.getDeveloper().getDisplayName(),
                        r.getVulnerabilitiesPerKloc(),
                        String.format("{\"vulnerabilities_per_kloc\": %.4f, \"total_kloc\": %.2f}",
                                r.getVulnerabilitiesPerKloc(), r.getTotalKloc())
                ));
    }

    private void identifyCraftsmanChampion(List<IndividualRanking> rankings, LocalDate month) {
        rankings.stream()
                .filter(r -> r.getCraftsmanRank() == 1 && r.getTotalKloc() >= 1.0)
                .findFirst()
                .ifPresent(r -> saveChampion(
                        month,
                        MonthlyChampion.ChampionCategory.CRAFTSMAN,
                        MonthlyChampion.EntityType.INDIVIDUAL,
                        r.getDeveloper().getId(),
                        r.getDeveloper().getDisplayName(),
                        r.getCodeSmellsPerKloc(),
                        String.format("{\"code_smells_per_kloc\": %.4f, \"total_kloc\": %.2f}",
                                r.getCodeSmellsPerKloc(), r.getTotalKloc())
                ));
    }

    private void identifyClimberChampion(List<IndividualRanking> rankings, LocalDate month) {
        rankings.stream()
                .filter(r -> r.getClimberRank() == 1 && r.getAvgRankImprovement() > 0)
                .findFirst()
                .ifPresent(r -> saveChampion(
                        month,
                        MonthlyChampion.ChampionCategory.CLIMBER,
                        MonthlyChampion.EntityType.INDIVIDUAL,
                        r.getDeveloper().getId(),
                        r.getDeveloper().getDisplayName(),
                        r.getAvgRankImprovement(),
                        String.format("{\"rank_improvement\": %.2f}",
                                r.getAvgRankImprovement())
                ));
    }

    // Similar methods for project champions...
    private void identifyProjectDefectTerminatorChampion(List<ProjectRanking> rankings, LocalDate month) {
        rankings.stream()
                .filter(r -> r.getDefectTerminatorRank() == 1)
                .findFirst()
                .ifPresent(r -> saveChampion(
                        month,
                        MonthlyChampion.ChampionCategory.DEFECT_TERMINATOR,
                        MonthlyChampion.EntityType.PROJECT,
                        r.getProject().getId(),
                        r.getProject().getProjectName(),
                        (double) r.getDefectTerminatorScore(),
                        String.format("{\"violations_resolved\": %d, \"violations_introduced\": %d}",
                                r.getViolationsResolved(), r.getViolationsIntroduced())
                ));
    }

    private void identifyProjectCodeRockChampion(List<ProjectRanking> rankings, LocalDate month) {
        rankings.stream()
                .filter(r -> r.getCodeRockRank() == 1 && r.getTotalKloc() >= 1.0)
                .findFirst()
                .ifPresent(r -> saveChampion(
                        month,
                        MonthlyChampion.Category.CODE_ROCK,
                        MonthlyChampion.EntityType.PROJECT,
                        r.getProject().getId(),
                        r.getProject().getProjectName(),
                        r.getBugsPerKloc(),
                        String.format("{\"bugs_per_kloc\": %.4f}",
                                r.getBugsPerKloc())
                ));
    }

    private void identifyProjectCodeShieldChampion(List<ProjectRanking> rankings, LocalDate month) {
        rankings.stream()
                .filter(r -> r.getCodeShieldRank() == 1 && r.getTotalKloc() >= 1.0)
                .findFirst()
                .ifPresent(r -> saveChampion(
                        month,
                        MonthlyChampion.ChampionCategory.CODE_SHIELD,
                        MonthlyChampion.EntityType.PROJECT,
                        r.getProject().getId(),
                        r.getProject().getProjectName(),
                        r.getVulnerabilitiesPerKloc(),
                        String.format("{\"vulnerabilities_per_kloc\": %.4f}",
                                r.getVulnerabilitiesPerKloc())
                ));
    }

    private void identifyProjectCraftsmanChampion(List<ProjectRanking> rankings, LocalDate month) {
        rankings.stream()
                .filter(r -> r.getCraftsmanRank() == 1 && r.getTotalKloc() >= 1.0)
                .findFirst()
                .ifPresent(r -> saveChampion(
                        month,
                        MonthlyChampion.ChampionCategory.CRAFTSMAN,
                        MonthlyChampion.EntityType.PROJECT,
                        r.getProject().getId(),
                        r.getProject().getProjectName(),
                        r.getCodeSmellsPerKloc(),
                        String.format("{\"code_smells_per_kloc\": %.4f}",
                                r.getCodeSmellsPerKloc())
                ));
    }

    private void saveChampion(LocalDate period,
                              MonthlyChampion.ChampionCategory category,
                              MonthlyChampion.EntityType entityType,
                              Integer entityId,
                              String entityName,
                              Double score,
                              String metricDetails) {
        MonthlyChampion champion = new MonthlyChampion();
        champion.setPeriod(period);
        champion.setCategory(category);
        champion.setEntityType(entityType);
        champion.setEntityId(entityId);
        champion.setEntityName(entityName);
        champion.setScore(score);
        champion.setMetricDetails(metricDetails);

        monthlyChampionRepository.save(champion);

        log.info("✓ {} Champion ({}): {} with score {}"
        );
    }
}
