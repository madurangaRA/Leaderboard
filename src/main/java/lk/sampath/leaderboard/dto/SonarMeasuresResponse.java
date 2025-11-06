package lk.sampath.leaderboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SonarMeasuresResponse {
    private Component component;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Component {
        private String key;
        private String name;
        private String qualifier;
        private List<Measure> measures;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Measure {
        private String metric;
        private String value;
        private boolean bestValue;
    }
}
