package lk.sampath.leaderboard.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardSchedulerService {

    private final SonarQubeSyncService syncService;
    private final RankingCalculationService rankingService;

    /**
     * Runs on the 1st day of every month at 2 AM
     * Syncs data from the previous month
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void monthlyDataSync() {
        LocalDate previousMonth = LocalDate.now().minusMonths(1);
        log.info("Starting scheduled monthly sync for {}", previousMonth);

        try {
            syncService.syncMonthlyData(previousMonth);
            log.info("Monthly sync completed successfully for {}", previousMonth);
        } catch (Exception e) {
            log.error("Error during monthly sync for {}: {}", previousMonth, e.getMessage(), e);
        }
    }

    /**
     * Runs on the 1st day of every month at 3 AM
     * Calculates rankings for the previous month
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    public void     monthlyRankingCalculation() {
        LocalDate previousMonth = LocalDate.now().minusMonths(1);
        log.info("Starting scheduled ranking calculation for {}", previousMonth);

        try {
            rankingService.calculateMonthlyRankings(previousMonth);
            log.info("Ranking calculation completed successfully for {}", previousMonth);
        } catch (Exception e) {
            log.error("Error during ranking calculation for {}: {}", previousMonth, e.getMessage(), e);
        }
    }

}
