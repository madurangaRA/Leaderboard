package lk.sampath.leaderboard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "developer_metrics_daily", indexes = {
        @Index(name = "idx_date_recorded", columnList = "date_recorded"),
        @Index(name = "idx_developer_date", columnList = "developer_id, date_recorded"),
        @Index(name = "idx_project_date", columnList = "project_id, date_recorded")
}, uniqueConstraints = {
        @UniqueConstraint(name = "unique_dev_project_date",
                columnNames = {"developer_id", "project_id", "date_recorded"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeveloperMetricsDaily {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developer_id", nullable = false)
    private Developer developer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "date_recorded", nullable = false)
    private LocalDate dateRecorded;

    @Column(name = "violations_introduced")
    private Integer violationsIntroduced = 0;

    @Column(name = "violations_resolved")
    private Integer violationsResolved = 0;

    @Column(name = "bugs_introduced")
    private Integer bugsIntroduced = 0;

    @Column(name = "vulnerabilities_introduced")
    private Integer vulnerabilitiesIntroduced = 0;

    @Column(name = "code_smells_introduced")
    private Integer codeSmellsIntroduced = 0;

    @Column(name = "lines_of_code_contributed")
    private Integer linesOfCodeContributed = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

