package lk.sampath.leaderboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardDTO {
    private String achievementName;
    private RankingPositionDTO first;
    private RankingPositionDTO second;
    private RankingPositionDTO third;
}
