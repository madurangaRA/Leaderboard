package lk.sampath.leaderboard.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.sampath.leaderboard.dto.ImportResponse;
import lk.sampath.leaderboard.dto.SonarDevelopersResponse;
import lk.sampath.leaderboard.dto.SonarIssuesSearchResponse;
import lk.sampath.leaderboard.dto.SonarProjectSearchResponse;
import lk.sampath.leaderboard.entity.Developer;
import lk.sampath.leaderboard.entity.Issue;
import lk.sampath.leaderboard.entity.Project;
import lk.sampath.leaderboard.repository.DeveloperRepository;
import lk.sampath.leaderboard.repository.IssueRepository;
import lk.sampath.leaderboard.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ImportFromJsonService {

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DeveloperRepository developerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public ImportResponse importIssuesFromJson(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try {
            SonarIssuesSearchResponse response = objectMapper.readValue(
                    file.getInputStream(),
                    SonarIssuesSearchResponse.class
            );

            if (response.getIssues() == null || response.getIssues().isEmpty()) {
                return new ImportResponse(false, "No issues found in JSON file", 0, null);
            }

            for (SonarIssuesSearchResponse.IssueDetail detail : response.getIssues()) {
                try {
                    Issue issue = new Issue();
                    issue.setIssueKey(detail.getKey());

                    // handle Optional<Project>
                    Optional<Project> optProject = projectRepository.findByProjectKey(detail.getProject());
                    Project project;
                    if (optProject.isPresent()) {
                        project = optProject.get();
                    } else {
                        project = Project.builder()
                                .projectKey(detail.getProject())
                                .projectName(detail.getProject())
                                .isActive(true)
                                .build();
                        project = projectRepository.save(project);
                    }
                    issue.setProject(project);

                    // handle Optional<Developer>
                    if (detail.getAuthor() != null && !detail.getAuthor().isEmpty()) {
                        Optional<Developer> optDev = developerRepository.findByAuthorKey(detail.getAuthor());
                        Developer dev;
                        if (optDev.isPresent()) {
                            dev = optDev.get();
                        } else {
                            dev = Developer.builder()
                                    .authorKey(detail.getAuthor())
                                    .displayName(detail.getAuthor())
                                    .isActive(true)
                                    .build();
                            dev = developerRepository.save(dev);
                        }
                        issue.setDeveloper(dev);
                    }

                    issue.setRuleKey(detail.getRule());
                    issue.setSeverity(Issue.Severity.valueOf(detail.getSeverity()));
                    issue.setIssueType(Issue.IssueType.valueOf(detail.getType()));
                    issue.setStatus(Issue.IssueStatus.valueOf(detail.getStatus()));
                    issue.setComponentPath(detail.getComponent());
                    issue.setLineNumber(detail.getLine());
                    issue.setMessage(detail.getMessage());
                    issue.setEffortMinutes(parseEffort(detail.getEffort()));

                    // parse creation/update as OffsetDateTime then convert to LocalDateTime on entity
                    OffsetDateTime odtCreated = tryParseOffsetDateTime(detail.getCreationDate());
                    if (odtCreated != null) {
                        issue.setCreatedDate(odtCreated.toLocalDateTime());
                    }
                    OffsetDateTime odtUpdated = tryParseOffsetDateTime(detail.getUpdateDate());
                    if (odtUpdated != null) {
                        issue.setUpdatedDate(odtUpdated.toLocalDateTime());
                    }

                    issueRepository.save(issue);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Issue " + detail.getKey() + ": " + e.getMessage());
                }
            }

            return new ImportResponse(
                    successCount > 0,
                    successCount + " issues imported",
                    successCount,
                    errors.isEmpty() ? null : errors
            );

        } catch (Exception e) {
            return new ImportResponse(false, "Import failed: " + e.getMessage(), 0, null);
        }
    }

    @Transactional
    public ImportResponse importIssuesFromJson(InputStream inputStream) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try {
            SonarIssuesSearchResponse response = objectMapper.readValue(
                    inputStream,
                    SonarIssuesSearchResponse.class
            );

            if (response.getIssues() == null || response.getIssues().isEmpty()) {
                return new ImportResponse(false, "No issues found in JSON content", 0, null);
            }

            for (SonarIssuesSearchResponse.IssueDetail detail : response.getIssues()) {
                try {
                    Issue issue = new Issue();
                    issue.setIssueKey(detail.getKey());

                    // handle Optional<Project>
                    Optional<Project> optProject = projectRepository.findByProjectKey(detail.getProject());
                    Project project;
                    if (optProject.isPresent()) {
                        project = optProject.get();
                    } else {
                        project = Project.builder()
                                .projectKey(detail.getProject())
                                .projectName(detail.getProject())
                                .isActive(true)
                                .build();
                        project = projectRepository.save(project);
                    }
                    issue.setProject(project);

                    // handle Optional<Developer>
                    if (detail.getAuthor() != null && !detail.getAuthor().isEmpty()) {
                        Optional<Developer> optDev = developerRepository.findByAuthorKey(detail.getAuthor());
                        Developer dev;
                        if (optDev.isPresent()) {
                            dev = optDev.get();
                        } else {
                            dev = Developer.builder()
                                    .authorKey(detail.getAuthor())
                                    .displayName(detail.getAuthor())
                                    .isActive(true)
                                    .build();
                            dev = developerRepository.save(dev);
                        }
                        issue.setDeveloper(dev);
                    }

                    issue.setRuleKey(detail.getRule());
                    issue.setSeverity(Issue.Severity.valueOf(detail.getSeverity()));
                    issue.setIssueType(Issue.IssueType.valueOf(detail.getType()));
                    issue.setStatus(Issue.IssueStatus.valueOf(detail.getStatus()));
                    issue.setComponentPath(detail.getComponent());
                    issue.setLineNumber(detail.getLine());
                    issue.setMessage(detail.getMessage());

                    Integer effortMinutes = detail.getEffort() != null
                            ? Integer.valueOf(detail.getEffort())
                            : parseEffort(detail.getEffort());
                    issue.setEffortMinutes(effortMinutes);

                    OffsetDateTime odtCreated = tryParseOffsetDateTime(
                            firstNonNull(detail.getCreationDate(), detail.getCreationDate(), detail.getUpdateDate())
                    );
                    if (odtCreated != null) {
                        issue.setCreatedDate(odtCreated.toLocalDateTime());
                    }

                    OffsetDateTime odtUpdated = tryParseOffsetDateTime(
                            firstNonNull(detail.getUpdateDate(), detail.getUpdateDate(), detail.getUpdateDate())
                    );
                    if (odtUpdated != null) {
                        issue.setUpdatedDate(odtUpdated.toLocalDateTime());
                    }

                    OffsetDateTime odtResolved = tryParseOffsetDateTime(
                            firstNonNull(detail.getCloseDate(), detail.getCloseDate())
                    );
                    if (odtResolved != null) {
                        issue.setResolvedDate(odtResolved.toLocalDateTime());
                    }

                    OffsetDateTime odtSonarCreated = tryParseOffsetDateTime(detail.getCreationDate());
                    if (odtSonarCreated != null) {
                        issue.setSonarCreatedAt(odtSonarCreated.toLocalDateTime());
                    }
                    OffsetDateTime odtSonarUpdated = tryParseOffsetDateTime(detail.getUpdateDate());
                    if (odtSonarUpdated != null) {
                        issue.setSonarUpdatedAt(odtSonarUpdated.toLocalDateTime());
                    }

                    issueRepository.save(issue);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Issue " + detail.getKey() + ": " + e.getMessage());
                }
            }

            return new ImportResponse(
                    successCount > 0,
                    successCount + " issues imported",
                    successCount,
                    errors.isEmpty() ? null : errors
            );

        } catch (Exception e) {
            return new ImportResponse(false, "Import failed: " + e.getMessage(), 0, null);
        }
    }

    @Transactional
    public ImportResponse importDevelopersFromJson(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try {
            SonarDevelopersResponse response = objectMapper.readValue(
                    file.getInputStream(),
                    SonarDevelopersResponse.class
            );

            if (response.getUsers() == null || response.getUsers().isEmpty()) {
                return new ImportResponse(false, "No developers found in JSON file", 0, null);
            }

            for (SonarDevelopersResponse.Developer dev : response.getUsers()) {
                try {
                    Developer developer = Developer.builder()
                            .authorKey(dev.getLogin())
                            .displayName(dev.getName())
                            .email(dev.getEmail())
                            .isActive(dev.isActive())
                            .build();

                    developerRepository.save(developer);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Developer " + dev.getLogin() + ": " + e.getMessage());
                }
            }

            return new ImportResponse(
                    successCount > 0,
                    successCount + " developers imported",
                    successCount,
                    errors.isEmpty() ? null : errors
            );

        } catch (Exception e) {
            return new ImportResponse(false, "Import failed: " + e.getMessage(), 0, null);
        }
    }

    @Transactional
    public ImportResponse importDevelopersFromJson(InputStream inputStream) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        try {
            SonarDevelopersResponse response = objectMapper.readValue(inputStream, SonarDevelopersResponse.class);
            if (response.getUsers() == null || response.getUsers().isEmpty()) {
                return new ImportResponse(false, "No developers found in JSON content", 0, null);
            }
            for (SonarDevelopersResponse.Developer dev : response.getUsers()) {
                try {
                    Developer developer = Developer.builder()
                            .authorKey(dev.getLogin())
                            .displayName(dev.getName())
                            .email(dev.getEmail())
                            .isActive(dev.isActive())
                            .build();
                    developerRepository.save(developer);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Developer " + dev.getLogin() + ": " + e.getMessage());
                }
            }
            return new ImportResponse(successCount > 0, successCount + " developers imported", successCount, errors.isEmpty() ? null : errors);
        } catch (Exception e) {
            return new ImportResponse(false, "Import failed: " + e.getMessage(), 0, null);
        }
    }

    @Transactional
    public ImportResponse importProjectsFromJson(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try {
            SonarProjectSearchResponse response = objectMapper.readValue(
                    file.getInputStream(),
                    SonarProjectSearchResponse.class
            );

            if (response.getComponents() == null || response.getComponents().isEmpty()) {
                return new ImportResponse(false, "No projects found in JSON file", 0, null);
            }

            for (SonarProjectSearchResponse.Component component : response.getComponents()) {
                try {
                    Project project = Project.builder()
                            .projectKey(component.getKey())
                            .projectName(component.getName())
                            .isActive(true)
                            .build();

                    projectRepository.save(project);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Project " + component.getKey() + ": " + e.getMessage());
                }
            }

            return new ImportResponse(
                    successCount > 0,
                    successCount + " projects imported",
                    successCount,
                    errors.isEmpty() ? null : errors
            );

        } catch (Exception e) {
            return new ImportResponse(false, "Import failed: " + e.getMessage(), 0, null);
        }
    }

    @Transactional
    public ImportResponse importProjectsFromJson(InputStream inputStream) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        try {
            SonarProjectSearchResponse response = objectMapper.readValue(inputStream, SonarProjectSearchResponse.class);
            if (response.getComponents() == null || response.getComponents().isEmpty()) {
                return new ImportResponse(false, "No projects found in JSON content", 0, null);
            }
            for (SonarProjectSearchResponse.Component component : response.getComponents()) {
                try {
                    Project project = Project.builder()
                            .projectKey(component.getKey())
                            .projectName(component.getName())
                            .isActive(true)
                            .build();

                    projectRepository.save(project);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Project " + component.getKey() + ": " + e.getMessage());
                }
            }
            return new ImportResponse(successCount > 0, successCount + " projects imported", successCount, errors.isEmpty() ? null : errors);
        } catch (Exception e) {
            return new ImportResponse(false, "Import failed: " + e.getMessage(), 0, null);
        }
    }

    private OffsetDateTime tryParseOffsetDateTime(String text) {
        if (text == null || text.isEmpty()) return null;
        DateTimeFormatter[] fmts = new DateTimeFormatter[] {
                DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        };
        for (DateTimeFormatter fmt : fmts) {
            try {
                return OffsetDateTime.parse(text, fmt);
            } catch (Exception ignored) {
            }
        }
        // try inserting colon into offset if missing (e.g. -0500 -> -05:00)
        if (text.matches(".*[+-]\\d{4}$")) {
            String t = text.substring(0, text.length() - 2) + ":" + text.substring(text.length() - 2);
            try {
                return OffsetDateTime.parse(t, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String firstNonNull(String... vals) {
        if (vals == null) return null;
        for (String v : vals) if (v != null && !v.isEmpty()) return v;
        return null;
    }

    private Integer parseEffort(String effort) {
        if (effort == null || effort.isEmpty()) return 0;
        int minutes = 0;
        String[] parts = effort.split(" ");
        for (String part : parts) {
            if (part.endsWith("min")) {
                minutes += Integer.parseInt(part.replace("min", ""));
            } else if (part.endsWith("h")) {
                minutes += Integer.parseInt(part.replace("h", "")) * 60;
            }
        }
        return minutes;
    }}
