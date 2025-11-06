package lk.sampath.leaderboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDTO {
    private ChampionDTO defectTerminator;
    private ChampionDTO codeRock;
    private ChampionDTO codeShield;
    private ChampionDTO craftsman;
    private ChampionDTO climber;
    private java.util.List<LeaderboardDTO> individualAchievements;
    private java.util.List<LeaderboardDTO> projectAchievements;
    private String lastUpdated;
    private Integer totalDevelopers;
    private Integer totalProjects;
}
