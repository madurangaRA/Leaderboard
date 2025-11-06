package lk.sampath.leaderboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChampionDTO {
    private String name;
    private String value;
    private String metric;
    private Integer rank;
    private String iconEmoji;
}
