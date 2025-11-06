package lk.sampath.leaderboard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "issues", indexes = {
        @Index(name = "idx_issue_key", columnList = "issue_key"),
        @Index(name = "idx_project_type", columnList = "project_id, issue_type"),
        @Index(name = "idx_developer_type", columnList = "developer_id, issue_type"),
        @Index(name = "idx_created_date", columnList = "created_date"),
        @Index(name = "idx_resolved_date", columnList = "resolved_date"),
        @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "issue_key", unique = true, nullable = false)
    private String issueKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developer_id")
    private Developer developer;

    @Column(name = "rule_key")
    private String ruleKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private Severity severity = Severity.MAJOR;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false)
    private IssueType issueType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private IssueStatus status = IssueStatus.OPEN;

    @Column(name = "component_path", length = 500)
    private String componentPath;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "effort_minutes")
    private Integer effortMinutes = 0;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "resolved_date")
    private LocalDateTime resolvedDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "sonar_created_at", updatable = false)
    private LocalDateTime sonarCreatedAt;

    @Column(name = "sonar_updated_at")
    private LocalDateTime sonarUpdatedAt;

    @PrePersist
    protected void onCreate() {
        sonarCreatedAt = LocalDateTime.now();
        sonarUpdatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        sonarUpdatedAt = LocalDateTime.now();
    }

    public enum Severity {
        BLOCKER, CRITICAL, MAJOR, MINOR, INFO
    }

    public enum IssueType {
        BUG, VULNERABILITY, CODE_SMELL
    }

    public enum IssueStatus {
        OPEN, CONFIRMED, REOPENED, RESOLVED, CLOSED
    }
}
