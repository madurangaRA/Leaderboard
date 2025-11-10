package lk.sampath.leaderboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SonarUserSearchResponse {
    private List<User> users;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        private String login;
        private String name; // display name
        private String email;
    }
}

