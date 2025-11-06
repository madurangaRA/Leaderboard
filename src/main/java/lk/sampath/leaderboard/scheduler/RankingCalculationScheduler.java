package lk.sampath.leaderboard.scheduler;

import lk.sampath.leaderboard.services.RankingCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduler for monthly ranking calculations
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sonarqube.ranking.enabled", havingValue = "true", matchIfMissing = true)
public class RankingCalculationScheduler {

    private final RankingCalculationService rankingCalculationService;

    /**
     * Calculate rankings on the 1st day of each month at 3 AM
     * Cron: "0 0 3 1 * ?" = At 03:00 on day 1 of every month
     */
    @Scheduled(cron = "${sonarqube.ranking.cron:0 0 3 1 * ?}")
    public void scheduledMonthlyRankingCalculation() {
        log.info("=== Starting scheduled monthly ranking calculation ===");

        try {
            // Calculate for previous month
            LocalDate previousMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);

            rankingCalculationService.calculateMonthlyRankings(previousMonth);

            log.info("✓ Scheduled monthly ranking calculation completed successfully");

        } catch (Exception e) {
            log.error("✗ Error during scheduled ranking calculation", e);
        }

        log.info("=== Scheduled monthly ranking calculation completed ===");
    }

    /**
     * Optional: Run on startup for development/testing
     * Uncomment @Scheduled annotation to enable
     */
    // @Scheduled(initialDelay = 60000) // 60 seconds after startup
    public void initialRankingCalculation() {
        log.info("Running initial ranking calculation on startup");
        scheduledMonthlyRankingCalculation();
    }
}