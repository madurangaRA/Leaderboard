package lk.sampath.leaderboard.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for ranking calculations and queries
 */
@Slf4j
@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {
//
//    private final RankingCalculationService rankingCalculationService;
//    private final IndividualRankingRepository individualRankingRepository;
//    private final ProjectRankingRepository projectRankingRepository;
//    private final MonthlyChampionRepository monthlyChampionRepository;
//
//    /**
//     * Trigger manual ranking calculation for a specific month
//     * POST /api/rankings/calculate?month=2024-01-01
//     */
//    @PostMapping("/calculate")
//    public ResponseEntity<Map<String, Object>> calculateRankings(
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
//
//        log.info("Manual ranking calculation triggered for month: {}", month);
//
//        // Default to current month if not specified
//        if (month == null) {
//            month = LocalDate.now().withDayOfMonth(1);
//        } else {
//            month = month.withDayOfMonth(1);
//        }
//
//        Map<String, Object> response = new HashMap<>();
//
//        try {
//            long startTime = System.currentTimeMillis();
//
//            rankingCalculationService.calculateMonthlyRankings(month);
//
//            long duration = System.currentTimeMillis() - startTime;
//
//            response.put("success", true);
//            response.put("message", "Rankings calculated successfully");
//            response.put("month", month);
//            response.put("durationMs", duration);
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("Error calculating rankings", e);
//            response.put("success", false);
//            response.put("message", "Failed to calculate rankings: " + e.getMessage());
//            response.put("month", month);
//
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }
//
//    /**
//     * Get individual rankings for a specific month
//     * GET /api/rankings/individual?month=2024-01-01
//     */
//    @GetMapping("/individual")
//    public ResponseEntity<List<IndividualRanking>> getIndividualRankings(
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
//
//        if (month == null) {
//            month = LocalDate.now().withDayOfMonth(1);
//        }
//
//        List<IndividualRanking> rankings = individualRankingRepository.findByRankingPeriod(month);
//        return ResponseEntity.ok(rankings);
//    }
//
//    /**
//     * Get project rankings for a specific month
//     * GET /api/rankings/project?month=2024-01-01
//     */
//    @GetMapping("/project")
//    public ResponseEntity<List<ProjectRanking>> getProjectRankings(
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
//
//        if (month == null) {
//            month = LocalDate.now().withDayOfMonth(1);
//        }
//
//        List<ProjectRanking> rankings = projectRankingRepository.findByRankingPeriod(month);
//        return ResponseEntity.ok(rankings);
//    }
//
//    /**
//     * Get champions for a specific month
//     * GET /api/rankings/champions?month=2024-01-01
//     */
//    @GetMapping("/champions")
//    public ResponseEntity<Map<String, Object>> getChampions(
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
//
//        if (month == null) {
//            month = LocalDate.now().withDayOfMonth(1);
//        }
//
//        Map<String, Object> response = new HashMap<>();
//
//        List<MonthlyChampion> individualChampions =
//                monthlyChampionRepository.findIndividualChampionsByPeriod(month);
//
//        List<MonthlyChampion> projectChampions =
//                monthlyChampionRepository.findProjectChampionsByPeriod(month);
//
//        response.put("month", month);
//        response.put("individualChampions", individualChampions);
//        response.put("projectChampions", projectChampions);
//
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * Get top 3 for specific category
//     * GET /api/rankings/top3/defect-terminator?month=2024-01-01&type=individual
//     */
//    @GetMapping("/top3/{category}")
//    public ResponseEntity<?> getTop3(
//            @PathVariable String category,
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
//            @RequestParam(defaultValue = "individual") String type) {
//
//        if (month == null) {
//            month = LocalDate.now().withDayOfMonth(1);
//        }
//
//        if ("individual".equalsIgnoreCase(type)) {
//            return ResponseEntity.ok(getTop3Individual(category, month));
//        } else if ("project".equalsIgnoreCase(type)) {
//            return ResponseEntity.ok(getTop3Project(category, month));
//        } else {
//            return ResponseEntity.badRequest().body("Invalid type. Use 'individual' or 'project'");
//        }
//    }
//
//    private List<?> getTop3Individual(String category, LocalDate month) {
//        return switch (category.toLowerCase()) {
//            case "defect-terminator" -> individualRankingRepository.findTop3DefectTerminators(month);
//            case "code-rock" -> individualRankingRepository.findTop3CodeRock(month);
//            case "code-shield" -> individualRankingRepository.findTop3CodeShield(month);
//            case "craftsman" -> individualRankingRepository.findTop3Craftsman(month);
//            case "climber" -> individualRankingRepository.findTop3Climber(month);
//            default -> List.of();
//        };
//    }
//
//    private List<?> getTop3Project(String category, LocalDate month) {
//        return switch (category.toLowerCase()) {
//            case "defect-terminator" -> projectRankingRepository.findTop3DefectTerminators(month);
//            case "code-rock" -> projectRankingRepository.findTop3CodeRock(month);
//            case "code-shield" -> projectRankingRepository.findTop3CodeShield(month);
//            case "craftsman" -> projectRankingRepository.findTop3Craftsman(month);
//            default -> List.of();
//        };
//    }
//
//    /**
//     * Get ranking statistics
//     * GET /api/rankings/stats
//     */
//    @GetMapping("/stats")
//    public ResponseEntity<Map<String, Object>> getRankingStats() {
//        Map<String, Object> stats = new HashMap<>();
//
//        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
//
//        long individualCount = individualRankingRepository.findByRankingPeriod(currentMonth).size();
//        long projectCount = projectRankingRepository.findByRankingPeriod(currentMonth).size();
//        long championCount = monthlyChampionRepository.findByPeriod(currentMonth).size();
//
//        stats.put("currentMonth", currentMonth);
//        stats.put("individualsRanked", individualCount);
//        stats.put("projectsRanked", projectCount);
//        stats.put("championsIdentified", championCount);
//
//        return ResponseEntity.ok(stats);
//    }
}
