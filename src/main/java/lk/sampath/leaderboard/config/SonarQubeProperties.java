package lk.sampath.leaderboard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sonarqube")
@Data
public class SonarQubeProperties {
    private String baseUrl;
    private String token;
    private String username;
    private String password;
    private int pageSize = 500;
    private int maxPages = 100;
}

