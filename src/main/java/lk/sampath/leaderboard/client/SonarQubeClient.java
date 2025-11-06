package lk.sampath.leaderboard.client;

import lk.sampath.leaderboard.dto.SonarIssuesSearchResponse;
import lk.sampath.leaderboard.dto.SonarMeasuresHistoryResponse;
import lk.sampath.leaderboard.dto.SonarMeasuresResponse;
import lk.sampath.leaderboard.dto.SonarProjectSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;


@Slf4j
@Component
public class SonarQubeClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final int pageSize;

    public SonarQubeClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${sonarqube.base-url}") String baseUrl,
            @Value("${sonarqube.api-token:}") String apiToken,
            @Value("${sonarqube.username:}") String username,
            @Value("${sonarqube.password:}") String password,
            @Value("${sonarqube.sync.page-size:500}") int pageSize) {

        this.baseUrl = baseUrl;
        this.pageSize = pageSize;

        // Build RestTemplate with authentication
        RestTemplateBuilder builder = restTemplateBuilder
                .rootUri(baseUrl);

        // Use API token if provided, otherwise use basic auth
        if (apiToken != null && !apiToken.isEmpty()) {
            String encodedToken = Base64.getEncoder()
                    .encodeToString((apiToken + ":").getBytes());
            builder = builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedToken);
            log.info("SonarQube client configured with API token");
        } else if (username != null && !username.isEmpty()) {
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            builder = builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
            log.info("SonarQube client configured with basic auth");
        }

        this.restTemplate = builder.build();
        log.info("SonarQube client initialized for: {}", baseUrl);
    }

    /**
     * Fetch all projects from SonarQube
     */
    public SonarProjectSearchResponse searchProjects(int page) {
        log.debug("Fetching projects - page: {}", page);

        try {
            String url = UriComponentsBuilder.fromPath("/api/projects/search")
                    .queryParam("p", page)
                    .queryParam("ps", pageSize)
                    .toUriString();

            ResponseEntity<SonarProjectSearchResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    SonarProjectSearchResponse.class
            );

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Error fetching projects: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fetch issues for a specific project
     */
    public SonarIssuesSearchResponse searchIssues(String projectKey, int page, String createdAfter) {
        log.debug("Fetching issues for project: {} - page: {}", projectKey, page);

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/issues/search")
                    .queryParam("componentKeys", projectKey)
                    .queryParam("p", page)
                    .queryParam("ps", pageSize)
                    .queryParam("resolved", "false")
                    .queryParam("types", "BUG,VULNERABILITY,CODE_SMELL");

            if (createdAfter != null && !createdAfter.isEmpty()) {
                builder.queryParam("createdAfter", createdAfter);
            }

            String url = builder.toUriString();

            ResponseEntity<SonarIssuesSearchResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    SonarIssuesSearchResponse.class
            );

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Error fetching issues: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fetch all issues (including resolved) for metrics calculation
     */
    public SonarIssuesSearchResponse searchAllIssues(String projectKey, int page,
                                                     String createdAfter, String createdBefore) {
        log.debug("Fetching all issues for project: {} - page: {}", projectKey, page);

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/issues/search")
                    .queryParam("componentKeys", projectKey)
                    .queryParam("p", page)
                    .queryParam("ps", pageSize)
                    .queryParam("types", "BUG,VULNERABILITY,CODE_SMELL");

            if (createdAfter != null && !createdAfter.isEmpty()) {
                builder.queryParam("createdAfter", createdAfter);
            }

            if (createdBefore != null && !createdBefore.isEmpty()) {
                builder.queryParam("createdBefore", createdBefore);
            }

            String url = builder.toUriString();

            ResponseEntity<SonarIssuesSearchResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    SonarIssuesSearchResponse.class
            );

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Error fetching all issues: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fetch current measures for a project
     */
    public SonarMeasuresResponse getProjectMeasures(String projectKey) {
        log.debug("Fetching measures for project: {}", projectKey);

        try {
            String metrics = "ncloc,bugs,vulnerabilities,code_smells," +
                    "reliability_rating,security_rating,sqale_rating";

            String url = UriComponentsBuilder.fromPath("/api/measures/component")
                    .queryParam("component", projectKey)
                    .queryParam("metricKeys", metrics)
                    .toUriString();

            ResponseEntity<SonarMeasuresResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    SonarMeasuresResponse.class
            );

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Error fetching measures: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fetch historical measures for a project
     */
    public SonarMeasuresHistoryResponse getProjectMeasuresHistory(String projectKey,
                                                                  String from, String to) {
        log.debug("Fetching measures history for project: {} from {} to {}", projectKey, from, to);

        try {
            String metrics = "ncloc,bugs,vulnerabilities,code_smells," +
                    "reliability_rating,security_rating,sqale_rating";

            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/measures/search_history")
                    .queryParam("component", projectKey)
                    .queryParam("metrics", metrics)
                    .queryParam("ps", 1000);

            if (from != null && !from.isEmpty()) {
                builder.queryParam("from", from);
            }
            if (to != null && !to.isEmpty()) {
                builder.queryParam("to", to);
            }

            String url = builder.toUriString();

            ResponseEntity<SonarMeasuresHistoryResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    SonarMeasuresHistoryResponse.class
            );

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Error fetching measures history: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Test connection to SonarQube
     */
    public boolean testConnection() {
        try {
            log.info("Testing SonarQube connection...");
            SonarProjectSearchResponse response = searchProjects(1);
            boolean connected = response != null;
            log.info("SonarQube connection test: {}", connected ? "SUCCESS" : "FAILED");
            return connected;
        } catch (Exception e) {
            log.error("SonarQube connection test failed: {}", e.getMessage());
            return false;
        }
    }
}