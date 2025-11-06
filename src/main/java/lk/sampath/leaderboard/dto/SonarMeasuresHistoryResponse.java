package lk.sampath.leaderboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SonarMeasuresHistoryResponse {
    private Paging paging;
    private List<Measure> measures;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Paging {
        private int pageIndex;
        private int pageSize;
        private int total;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Measure {
        private String metric;
        private List<History> history;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class History {
        private String date;
        private String value;
    }
}
