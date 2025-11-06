package lk.sampath.leaderboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SonarIssuesSearchResponse {
    private Paging paging;
    private List<IssueDetail> issues;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Paging {
        private int pageIndex;
        private int pageSize;
        private int total;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IssueDetail {
        private String key;
        private String rule;
        private String severity;
        private String component;
        private String project;
        private Integer line;
        private String status;
        private String message;
        private String effort;  // in minutes, e.g., "10min"
        private String debt;
        private String author;
        private String type;  // BUG, VULNERABILITY, CODE_SMELL
        private String creationDate;
        private String updateDate;
        private String closeDate;

        @JsonProperty("textRange")
        private TextRange textRange;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TextRange {
            private int startLine;
            private int endLine;
            private int startOffset;
            private int endOffset;
        }
    }
}