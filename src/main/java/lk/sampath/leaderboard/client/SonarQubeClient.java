package lk.sampath.leaderboard.client;

import lk.sampath.leaderboard.config.SonarQubeProperties;
import lk.sampath.leaderboard.dto.*;
import lk.sampath.leaderboard.services.SonarQubeConfigService;
import lk.sampath.leaderboard.entity.SonarQubeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Component
@RequiredArgsConstructor
@Slf4j
public class SonarQubeClient {

    private final SonarQubeProperties properties;
    private final RestTemplateBuilder restTemplateBuilder;
    private final SonarQubeConfigService configService;

    private RestTemplate restTemplate() {
        // Use RestTemplateBuilder's basicAuthentication so preemptive auth is configured when supported
        // If token is present, configure builder with token as username and empty password; otherwise builder stays default
        // Prefer token from DB config if available
        try {
            Optional<SonarQubeConfig> cfg = configService.getLatestConfig();
            if (cfg.isPresent() && cfg.get().getApiToken() != null && !cfg.get().getApiToken().isBlank()) {
                return restTemplateBuilder.basicAuthentication(cfg.get().getApiToken(), "").build();
            }
        } catch (Exception e) {
            log.debug("No DB sonar config available or error reading it: {}", e.getMessage());
        }

        if (properties.getToken() != null && !properties.getToken().isBlank()) {
            return restTemplateBuilder.basicAuthentication(properties.getToken(), "").build();
        }
        if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
            return restTemplateBuilder.basicAuthentication(properties.getUsername(), properties.getPassword() == null ? "" : properties.getPassword()).build();
        }
        return restTemplateBuilder.build();
    }

    private String getEffectiveBaseUrl() {
        try {
            Optional<SonarQubeConfig> cfg = configService.getLatestConfig();
            if (cfg.isPresent() && cfg.get().getBaseUrl() != null && !cfg.get().getBaseUrl().isBlank()) {
                return cfg.get().getBaseUrl();
            }
        } catch (Exception e) {
            log.debug("No DB sonar config available or error reading it: {}", e.getMessage());
        }
        return properties.getBaseUrl();
    }

    public List<SonarProjectSearchResponse.Component> fetchAllProjects() {
        List<SonarProjectSearchResponse.Component> allProjects = new ArrayList<>();
        int page = 1;

        while (page <= properties.getMaxPages()) {
            String url = UriComponentsBuilder.fromUriString(getEffectiveBaseUrl())
                    .path("/api/components/search")
                    .queryParam("qualifiers", "TRK")
                    .queryParam("p", page)
                    .queryParam("ps", properties.getPageSize())
                    .toUriString();

            try {
                ResponseEntity<SonarProjectSearchResponse> response = restTemplate().exchange(
                        url, HttpMethod.GET, createHttpEntity(), SonarProjectSearchResponse.class);

                if (response.getBody() != null) {
                    List<SonarProjectSearchResponse.Component> components = response.getBody().getComponents();
                    if (components.isEmpty()) {
                        break;
                    }
                    allProjects.addAll(components);

                    if (components.size() < properties.getPageSize()) {
                        break;
                    }
                }
                page++;
            } catch (Exception e) {
                logRequestError(e, "Error fetching projects at page " + page);
                break;
            }
        }

        log.info("Fetched {} projects from SonarQube", allProjects.size());
        return allProjects;
    }

    public List<SonarIssuesSearchResponse.IssueDetail> fetchIssuesForProject(String projectKey, LocalDate fromDate, LocalDate toDate) {
        List<SonarIssuesSearchResponse.IssueDetail> allIssues = new ArrayList<>();
        int page = 1;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        while (page <= properties.getMaxPages()) {
            String url = UriComponentsBuilder.fromUriString(getEffectiveBaseUrl())
                    .path("/api/issues/search")
                    .queryParam("componentKeys", projectKey)
                    .queryParam("createdAfter", fromDate.format(formatter))
                    .queryParam("createdBefore", toDate.format(formatter))
                    .queryParam("p", page)
                    .queryParam("ps", properties.getPageSize())
                    .queryParam("additionalFields", "comments")
                    .toUriString();

            try {
                ResponseEntity<SonarIssuesSearchResponse> response = restTemplate().exchange(
                        url, HttpMethod.GET, createHttpEntity(), SonarIssuesSearchResponse.class);

                if (response.getBody() != null) {
                    List<SonarIssuesSearchResponse.IssueDetail> issues = response.getBody().getIssues();
                    if (issues.isEmpty()) {
                        break;
                    }
                    allIssues.addAll(issues);

                    if (issues.size() < properties.getPageSize()) {
                        break;
                    }
                }
                page++;
            } catch (Exception e) {
                logRequestError(e, "Error fetching issues for project " + projectKey + " at page " + page);
                break;
            }
        }

        log.info("Fetched {} issues for project {}", allIssues.size(), projectKey);
        return allIssues;
    }

    public Map<String, String> fetchProjectMetrics(String projectKey) {
        String url = UriComponentsBuilder.fromUriString(getEffectiveBaseUrl())
                .path("/api/measures/component")
                .queryParam("component", projectKey)
                .queryParam("metricKeys", "ncloc,bugs,vulnerabilities,code_smells")
                .toUriString();

        try {
            ResponseEntity<SonarMeasuresResponse> response = restTemplate().exchange(
                    url, HttpMethod.GET, createHttpEntity(), SonarMeasuresResponse.class);

            Map<String, String> metrics = new HashMap<>();
            if (response.getBody() != null && response.getBody().getComponent() != null) {
                response.getBody().getComponent().getMeasures().forEach(measure ->
                        metrics.put(measure.getMetric(), measure.getValue())
                );
            }
            return metrics;
        } catch (Exception e) {
            logRequestError(e, "Error fetching metrics for project " + projectKey);
            return Collections.emptyMap();
        }
    }

    public Set<String> fetchAuthorsForProject(String projectKey) {
        String url = UriComponentsBuilder.fromUriString(getEffectiveBaseUrl())
                .path("/api/issues/authors")
                .queryParam("project", projectKey)
                .queryParam("ps", 500)
                .toUriString();

        try {
            ResponseEntity<SonarAuthorsResponse> response = restTemplate().exchange(
                    url, HttpMethod.GET, createHttpEntity(), SonarAuthorsResponse.class);

            if (response.getBody() != null && response.getBody().getAuthors() != null) {
                return new HashSet<>(response.getBody().getAuthors());
            }
        } catch (Exception e) {
            logRequestError(e, "Error fetching authors for project " + projectKey);
        }
        return Collections.emptySet();
    }

    /**
     * Fetch user details (display name) from SonarQube for a given login/author key.
     * Uses /api/users/search?login={login}
     */
    public Optional<String> fetchUserDisplayName(String login) {
        if (login == null || login.isBlank()) {
            return Optional.empty();
        }

        // Use 'q' parameter which is supported by SonarQube users search API and filter results
        String url = UriComponentsBuilder.fromUriString(getEffectiveBaseUrl())
                .path("/api/users/search")
                .queryParam("q", login)
                .toUriString();

        try {
            ResponseEntity<SonarUserSearchResponse> response = restTemplate().exchange(
                    url, HttpMethod.GET, createHttpEntity(), SonarUserSearchResponse.class);

            if (response.getBody() != null && response.getBody().getUsers() != null) {
                // Try to find exact login match first
                for (SonarUserSearchResponse.User user : response.getBody().getUsers()) {
                    if (user.getLogin() != null && user.getLogin().equalsIgnoreCase(login)) {
                        if (user.getName() != null && !user.getName().isBlank()) {
                            return Optional.of(user.getName());
                        }
                        return Optional.of(user.getLogin());
                    }
                }

                // If no exact login match, but there is at least one user whose name contains the login fragment, prefer that
                for (SonarUserSearchResponse.User user : response.getBody().getUsers()) {
                    if (user.getName() != null && user.getName().toLowerCase().contains(login.toLowerCase())) {
                        return Optional.of(user.getName());
                    }
                }
            }
        } catch (Exception e) {
            logRequestError(e, "Error fetching user details for login " + login);
        }
        // No reliable display name found; return empty so caller can fall back to formatted key
        return Optional.empty();
    }

    /**
     * Fetch user details (login, name, email) from SonarQube for a given login/author key.
     * Uses /api/users/search?q={login}
     */
    public Optional<SonarUserSearchResponse.User> fetchUserDetails(String login) {
        if (login == null || login.isBlank()) {
            return Optional.empty();
        }

        String url = UriComponentsBuilder.fromUriString(getEffectiveBaseUrl())
                .path("/api/users/search")
                .queryParam("q", login)
                .toUriString();

        try {
            ResponseEntity<SonarUserSearchResponse> response = restTemplate().exchange(
                    url, HttpMethod.GET, createHttpEntity(), SonarUserSearchResponse.class);

            if (response.getBody() != null && response.getBody().getUsers() != null) {
                // Log response size and first user for debugging why same name appears for many authors
                log.debug("fetchUserDetails('{}') returned {} users", login, response.getBody().getUsers().size());
                if (!response.getBody().getUsers().isEmpty()) {
                    SonarUserSearchResponse.User first = response.getBody().getUsers().get(0);
                    log.debug("First user for '{}': login='{}' name='{}' email='{}'", login, first.getLogin(), first.getName(), first.getEmail());
                }
                for (SonarUserSearchResponse.User user : response.getBody().getUsers()) {
                    if (user.getLogin() != null && user.getLogin().equalsIgnoreCase(login)) {
                        return Optional.of(user);
                    }
                }
                // Do not return a first/partial match to avoid incorrect mapping; only exact login matches are reliable
                return Optional.empty();
            }
        } catch (Exception e) {
            logRequestError(e, "Error fetching user details for login " + login);
        }
        return Optional.empty();
    }

    private HttpEntity<?> createHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        // Prefer API token; if not present, fall back to username/password
        if (properties.getToken() != null && !properties.getToken().isBlank()) {
            headers.setBasicAuth(properties.getToken(), "");
        } else if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
            headers.setBasicAuth(properties.getUsername(), properties.getPassword() == null ? "" : properties.getPassword());
        } else {
            String msg = "SonarQube credentials not configured. Set sonarqube.api-token or sonarqube.username/sonarqube.password in application properties.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(headers);
    }

    private void logRequestError(Exception e, String message) {
        if (e instanceof HttpClientErrorException) {
            HttpClientErrorException hce = (HttpClientErrorException) e;
            String body = "";
            try {
                body = hce.getResponseBodyAsString();
            } catch (Exception ignored) {
            }
            log.error("{} - status: {} body: {}", message, hce.getStatusCode(), body);
        } else {
            log.error("{} - {}", message, e.getMessage());
        }
    }

//    /**
//     * Test connection to SonarQube
//     */
//    public boolean testConnection() {
//        try {
//            log.info("Testing SonarQube connection...");
//            SonarProjectSearchResponse response = searchProjects(1);
//            boolean connected = response != null;
//            log.info("SonarQube connection test: {}", connected ? "SUCCESS" : "FAILED");
//            return connected;
//        } catch (Exception e) {
//            log.error("SonarQube connection test failed: {}", e.getMessage());
//            return false;
//        }
//    }
}