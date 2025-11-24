package lk.sampath.leaderboard.services.impl;

import lk.sampath.leaderboard.dto.ChampionDTO;
import lk.sampath.leaderboard.dto.DashboardDTO;
import lk.sampath.leaderboard.dto.LeaderboardDTO;
import lk.sampath.leaderboard.dto.mapper.DashboardMapper;
import lk.sampath.leaderboard.entity.IndividualRanking;
import lk.sampath.leaderboard.entity.ProjectRanking;
import lk.sampath.leaderboard.repository.IndividualRankingRepository;
import lk.sampath.leaderboard.repository.ProjectRankingRepository;
import lk.sampath.leaderboard.services.DashboardService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;


@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final IndividualRankingRepository individualRankingRepository;

    private final ProjectRankingRepository projectRankingRepository;

    private final DashboardMapper dashboardMapper;

    @Override
    @Cacheable(value = "dashboard", unless = "#result == null")
    public DashboardDTO getDashboardData() {
        log.info("Fetching dashboard data");
        // use a date in the previous month (any day) as the period reference
        // normalize to first day of previous month to match stored rankingPeriod (month-based)
        LocalDate currentPeriod = LocalDate.now().minusMonths(1).withDayOfMonth(1);

        try {
            List<IndividualRanking> defectTerminators = individualRankingRepository.findTop3DefectTerminators(currentPeriod);
            List<IndividualRanking> codeRocks = individualRankingRepository.findTop3CodeRock(currentPeriod);
            List<IndividualRanking> codeShields = individualRankingRepository.findTop3CodeShield(currentPeriod);
            List<IndividualRanking> craftsmen = individualRankingRepository.findTop3Craftsman(currentPeriod);
            List<IndividualRanking> climbers = individualRankingRepository.findTop3Climber(currentPeriod);

            List<ProjectRanking> projectDefectTerminators = projectRankingRepository.findTop3DefectTerminators(currentPeriod);
            List<ProjectRanking> projectCodeRocks = projectRankingRepository.findTop3CodeRock(currentPeriod);
            List<ProjectRanking> projectCodeShields = projectRankingRepository.findTop3CodeShield(currentPeriod);
            List<ProjectRanking> projectCraftsmen = projectRankingRepository.findTop3Craftsman(currentPeriod);

            log.info(defectTerminators.toString());

            DashboardDTO dashboard = DashboardDTO.builder()
                    .defectTerminator(getFirstOrNull(defectTerminators, "defect_terminator", "🛡️"))
                    .codeRock(getFirstOrNull(codeRocks, "code_rock", "🪨"))
                    .codeShield(getFirstOrNull(codeShields, "code_shield", "🛡️"))
                    .craftsman(getFirstOrNull(craftsmen, "craftsman", "🔧"))
                    .climber(getFirstOrNull(climbers, "climber", "📈"))
                    .individualAchievements(buildIndividualLeaderboards(Arrays.asList(defectTerminators, codeRocks, codeShields, craftsmen, climbers)))
                    .projectAchievements(buildProjectLeaderboards(Arrays.asList(projectDefectTerminators, projectCodeRocks, projectCodeShields, projectCraftsmen)))
                    .lastUpdated(LocalDateTime.now().toString())
                    .build();

            log.info("Dashboard data retrieved successfully");
            return dashboard;

        } catch (Exception e) {
            log.error("Error fetching dashboard data", e);
            throw new RuntimeException("Failed to fetch dashboard data", e);
        }
    }

    @Override
    @Scheduled(cron = "0 0 * * * *")
    @CacheEvict(value = "dashboard", allEntries = true)
    public void refreshDashboard() {
        log.info("Refreshing dashboard cache (evicted). Will be repopulated on next request.");
    }

    private ChampionDTO getFirstOrNull(List<?> rankings, String metricType, String emoji) {
        if (rankings == null || rankings.isEmpty()) {
            return null;
        }

        Object first = rankings.get(0);
        if (first instanceof IndividualRanking) {
            return dashboardMapper.toChampionDTO((IndividualRanking) first, metricType, emoji);
        } else if (first instanceof ProjectRanking) {
            return dashboardMapper.toProjectChampionDTO((ProjectRanking) first, metricType, emoji);
        }
        return null;
    }

    private List<LeaderboardDTO> buildIndividualLeaderboards(List<List<IndividualRanking>> allRankings) {
        List<LeaderboardDTO> leaderboards = new ArrayList<>();
        String[] achievements = {"Defect Terminator", "Code Rock", "Code Shield", "Craftsman", "Climber"};
        String[] metrics = {"defect_terminator", "code_rock", "code_shield", "craftsman", "climber"};

        for (int i = 0; i < allRankings.size(); i++) {
            List<IndividualRanking> rankings = allRankings.get(i);
            LeaderboardDTO leaderboard = LeaderboardDTO.builder()
                    .achievementName(achievements[i])
                    .first(!rankings.isEmpty() ? dashboardMapper.toRankingPositionDTO(rankings.get(0), metrics[i]) : null)
                    .second(rankings.size() > 1 ? dashboardMapper.toRankingPositionDTO(rankings.get(1), metrics[i]) : null)
                    .third(rankings.size() > 2 ? dashboardMapper.toRankingPositionDTO(rankings.get(2), metrics[i]) : null)
                    .build();
            leaderboards.add(leaderboard);
        }

        return leaderboards;
    }

    private List<LeaderboardDTO> buildProjectLeaderboards(List<List<ProjectRanking>> allRankings) {
        List<LeaderboardDTO> leaderboards = new ArrayList<>();
        String[] achievements = {"Defect Terminator", "Code Rock", "Code Shield", "Craftsman"};
        String[] metrics = {"defect_terminator", "code_rock", "code_shield", "craftsman"};

        for (int i = 0; i < allRankings.size(); i++) {
            List<ProjectRanking> rankings = allRankings.get(i);
            LeaderboardDTO leaderboard = LeaderboardDTO.builder()
                    .achievementName(achievements[i])
                    .first(!rankings.isEmpty() ? dashboardMapper.toProjectRankingPositionDTO(rankings.get(0), metrics[i]) : null)
                    .second(rankings.size() > 1 ? dashboardMapper.toProjectRankingPositionDTO(rankings.get(1), metrics[i]) : null)
                    .third(rankings.size() > 2 ? dashboardMapper.toProjectRankingPositionDTO(rankings.get(2), metrics[i]) : null)
                    .build();
            leaderboards.add(leaderboard);
        }

        return leaderboards;
    }
}
