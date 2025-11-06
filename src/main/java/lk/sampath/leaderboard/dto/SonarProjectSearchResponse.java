package lk.sampath.leaderboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SonarProjectSearchResponse {
    private Paging paging;
    private List<Component> components;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Paging {
        private int pageIndex;
        private int pageSize;
        private int total;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Component {
        private String key;
        private String name;
        private String qualifier;
        private String visibility;
    }
}
