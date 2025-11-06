package lk.sampath.leaderboard.services;

import lk.sampath.leaderboard.dto.DashboardDTO;

public interface DashboardService {
    DashboardDTO getDashboardData();
    void refreshDashboard();
}
