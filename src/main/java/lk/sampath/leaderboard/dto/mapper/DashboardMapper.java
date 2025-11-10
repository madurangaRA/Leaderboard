package lk.sampath.leaderboard.dto.mapper;

import lk.sampath.leaderboard.dto.ChampionDTO;
import lk.sampath.leaderboard.dto.RankingPositionDTO;
import lk.sampath.leaderboard.entity.IndividualRanking;
import lk.sampath.leaderboard.entity.ProjectRanking;
import org.springframework.stereotype.Component;

@Component
public class DashboardMapper {

    public ChampionDTO toChampionDTO(IndividualRanking ranking, String metric, String iconEmoji) {
        if (ranking == null) {
            return null;
        }
        return ChampionDTO.builder()
                .name(ranking.getDeveloper().getDisplayName())
                .value(formatMetricValue(ranking, metric))
                .metric(metric)
                .rank(getRankByMetricType(ranking, metric))
                .iconEmoji(iconEmoji)
                .build();
    }

    public ChampionDTO toProjectChampionDTO(ProjectRanking ranking, String metric, String iconEmoji) {
         if (ranking == null) {
             return null;
         }
         return ChampionDTO.builder()
                 .name(ranking.getProject().getProjectName())
                 .value(formatProjectMetricValue(ranking, metric))
                 .metric(metric)
                 .rank(getProjectRankByMetricType(ranking, metric))
                 .iconEmoji(iconEmoji)
                 .build();
     }

    public RankingPositionDTO toRankingPositionDTO(IndividualRanking ranking, String metricType) {
        if (ranking == null) {
            return null;
        }
        return RankingPositionDTO.builder()
                .name(ranking.getDeveloper().getDisplayName())
                .score(formatMetricValue(ranking, metricType))
                .rank(getRankByMetricType(ranking, metricType))
                .build();
    }

    public RankingPositionDTO toProjectRankingPositionDTO(ProjectRanking ranking, String metricType) {
        if (ranking == null) {
            return null;
        }
        return RankingPositionDTO.builder()
                .name(ranking.getProject().getProjectName())
                .score(formatProjectMetricValue(ranking, metricType))
                .rank(getProjectRankByMetricType(ranking, metricType))
                .build();
    }

    private String formatMetricValue(IndividualRanking ranking, String metricType) {
        return switch (metricType.toLowerCase()) {
            case "defect_terminator" -> String.format("+%d", ranking.getDefectTerminatorScore() != null ? ranking.getDefectTerminatorScore() : 0);
            case "code_rock" -> String.format("%.1f", ranking.getCodeRockScore() != null ? ranking.getCodeRockScore().doubleValue() : 0);
            case "code_shield" -> String.format("%.1f", ranking.getCodeShieldScore() != null ? ranking.getCodeShieldScore().doubleValue() : 0);
            case "craftsman" -> String.format("%.1f", ranking.getCraftsmanScore() != null ? ranking.getCraftsmanScore().doubleValue() : 0);
            case "climber" -> String.format("+%.1f", ranking.getClimberScore() != null ? ranking.getClimberScore().doubleValue() : 0.0);
            default -> "N/A";
        };
    }

    private String formatProjectMetricValue(ProjectRanking ranking, String metricType) {
        return switch (metricType.toLowerCase()) {
            case "defect_terminator" -> String.format("+%d", ranking.getDefectTerminatorScore() != null ? ranking.getDefectTerminatorScore() : 0);
            case "code_rock" -> String.format("%.1f", ranking.getCodeRockScore() != null ? ranking.getCodeRockScore().doubleValue() : 0);
            case "code_shield" -> String.format("%.1f", ranking.getCodeShieldScore() != null ? ranking.getCodeShieldScore().doubleValue() : 0);
            case "craftsman" -> String.format("%.1f", ranking.getCraftsmanScore() != null ? ranking.getCraftsmanScore().doubleValue() : 0);
            default -> "N/A";
        };
    }

    private Integer getRankByMetricType(IndividualRanking ranking, String metricType) {
        return switch (metricType.toLowerCase()) {
            case "defect_terminator" -> ranking.getDefectTerminatorRank();
            case "code_rock" -> ranking.getCodeRockRank();
            case "code_shield" -> ranking.getCodeShieldRank();
            case "craftsman" -> ranking.getCraftsmanRank();
            case "climber" -> ranking.getClimberRank();
            default -> 999;
        };
    }

    private Integer getProjectRankByMetricType(ProjectRanking ranking, String metricType) {
        return switch (metricType.toLowerCase()) {
            case "defect_terminator" -> ranking.getDefectTerminatorRank();
            case "code_rock" -> ranking.getCodeRockRank();
            case "code_shield" -> ranking.getCodeShieldRank();
            case "craftsman" -> ranking.getCraftsmanRank();
            default -> 999;
        };
    }
}
