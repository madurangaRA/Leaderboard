package lk.sampath.leaderboard.controller;

import lk.sampath.leaderboard.services.RankingCalculationService;
import lk.sampath.leaderboard.services.SonarQubeSyncService;
import lk.sampath.leaderboard.services.LeaderboardSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
@Slf4j
public class LeaderboardController {

    private final SonarQubeSyncService syncService;
    private final RankingCalculationService rankingService;
    private final LeaderboardSchedulerService schedulerService;
    private final lk.sampath.leaderboard.repository.IndividualRankingRepository individualRankingRepository;
    private final lk.sampath.leaderboard.repository.ProjectRankingRepository projectRankingRepository;

    // ==================== Manual Job Triggers ====================

    /**
     * Manually trigger the monthly data sync job
     * Same as the scheduled job that runs on 1st of each month at 2 AM
     */
    @PostMapping("/jobs/monthly-sync")
    public ResponseEntity<?> triggerMonthlySyncJob() {
        log.info("Manual trigger: Monthly data sync job");
        try {
            schedulerService.monthlyDataSync();
            return ResponseEntity.ok(Map.of(
                    "message", "Monthly data sync job completed successfully",
                    "job", "monthlyDataSync"
            ));
        } catch (Exception e) {
            log.error("Error during manual monthly sync job", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Manually trigger the monthly ranking calculation job
     * Same as the scheduled job that runs on 1st of each month at 3 AM
     */
    @PostMapping("/jobs/monthly-ranking")
    public ResponseEntity<?> triggerMonthlyRankingJob() {
        log.info("Manual trigger: Monthly ranking calculation job");
        try {
            schedulerService.monthlyRankingCalculation();
            return ResponseEntity.ok(Map.of(
                    "message", "Monthly ranking calculation job completed successfully",
                    "job", "monthlyRankingCalculation"
            ));
        } catch (Exception e) {
            log.error("Error during manual monthly ranking job", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Manually trigger both monthly jobs in sequence (sync + ranking)
     * Useful for complete monthly processing
     */
    @PostMapping("/jobs/monthly-complete")
    public ResponseEntity<?> triggerCompleteMonthlyJob() {
        log.info("Manual trigger: Complete monthly job (sync + ranking)");
        try {
            schedulerService.monthlyDataSync();
            schedulerService.monthlyRankingCalculation();
            return ResponseEntity.ok(Map.of(
                    "message", "Complete monthly job (sync + ranking) completed successfully",
                    "job", "monthlyComplete"
            ));
        } catch (Exception e) {
            log.error("Error during manual complete monthly job", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Custom Date Operations ====================

    /**
     * Manually trigger data sync for a specific month
     */
    @PostMapping("/sync")
    public ResponseEntity<?> syncData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        log.info("Manual sync triggered for {}", month);
        try {
            syncService.syncMonthlyData(month);
            return ResponseEntity.ok(Map.of(
                    "message", "Data sync completed successfully",
                    "month", month
            ));
        } catch (Exception e) {
            log.error("Error during manual sync", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Manually trigger ranking calculation for a specific month
     */
    @PostMapping("/calculate-rankings")
    public ResponseEntity<?> calculateRankings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        log.info("Manual ranking calculation triggered for {}", month);
        try {
            rankingService.calculateMonthlyRankings(month);
            return ResponseEntity.ok(Map.of(
                    "message", "Ranking calculation completed successfully",
                    "month", month
            ));
        } catch (Exception e) {
            log.error("Error during manual ranking calculation", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}