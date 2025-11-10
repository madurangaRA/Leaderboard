package lk.sampath.leaderboard.controller;

import lk.sampath.leaderboard.dto.DashboardDTO;
import lk.sampath.leaderboard.services.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

@Controller

@RequestMapping("/dashboard")
@Slf4j
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @GetMapping
    @Cacheable("dashboard")
    public String getDashboard(Model model) {
        log.info("Loading dashboard page");
        try {
            DashboardDTO dashboardData = dashboardService.getDashboardData();

            model.addAttribute("defectTerminator", dashboardData.getDefectTerminator());
            model.addAttribute("codeRock", dashboardData.getCodeRock());
            model.addAttribute("codeShield", dashboardData.getCodeShield());
            model.addAttribute("craftsman", dashboardData.getCraftsman());
            model.addAttribute("climber", dashboardData.getClimber());
            model.addAttribute("individualAchievements", dashboardData.getIndividualAchievements());
            model.addAttribute("projectAchievements", dashboardData.getProjectAchievements());
            model.addAttribute("lastUpdated", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            boolean hasChampions = dashboardData.getDefectTerminator() != null
                    || dashboardData.getCodeRock() != null
                    || dashboardData.getCodeShield() != null
                    || dashboardData.getCraftsman() != null
                    || dashboardData.getClimber() != null;

            boolean hasLeaderboards = (dashboardData.getIndividualAchievements() != null && !dashboardData.getIndividualAchievements().isEmpty())
                    || (dashboardData.getProjectAchievements() != null && !dashboardData.getProjectAchievements().isEmpty());

            model.addAttribute("hasChampions", hasChampions);
            model.addAttribute("hasLeaderboards", hasLeaderboards);

            log.info("Dashboard data loaded successfully");

            return "dashboard";
        } catch (Exception e) {
            log.error("Error loading dashboard", e);
            model.addAttribute("error", "Failed to load dashboard");
            return "error";
        }
    }
}