package lk.sampath.leaderboard.controller;

import lk.sampath.leaderboard.dto.DashboardDTO;
import lk.sampath.leaderboard.services.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class SonarController {

//    @Autowired
//    private DashboardService dashboardService;
//
//    @GetMapping("/dashboard")
//    public ResponseEntity<DashboardDTO> getDashboardData() {
//        log.info("API request for dashboard data");
//        try {
//            DashboardDTO dashboardData = dashboardService.getDashboardData();
//            return ResponseEntity.ok(dashboardData);
//        } catch (Exception e) {
//            log.error("Error fetching dashboard API", e);
//            return ResponseEntity.internalServerError().build();
//        }
//    }
//
//    @PostMapping("/refresh")
//    public ResponseEntity<String> refreshDashboard() {
//        log.info("Manual refresh request for dashboard");
//        try {
//            dashboardService.refreshDashboard();
//            return ResponseEntity.ok("Dashboard refreshed successfully");
//        } catch (Exception e) {
//            log.error("Error refreshing dashboard", e);
//            return ResponseEntity.internalServerError().body("Error refreshing dashboard");
//        }
//    }
//
//    @GetMapping("/health")
//    public ResponseEntity<String> health() {
//        return ResponseEntity.ok("Dashboard service is running");
//    }
}
