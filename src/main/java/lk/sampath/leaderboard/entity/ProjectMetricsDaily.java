package lk.sampath.leaderboard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_metrics_daily")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMetricsDaily {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "date_recorded", nullable = false)
    private LocalDate dateRecorded;

    @Column(name = "bugs_count")
    private Integer bugsCount = 0;

    @Column(name = "vulnerabilities_count")
    private Integer vulnerabilitiesCount = 0;

    @Column(name = "code_smells_count")
    private Integer codeSmellsCount = 0;

    @Column(name = "lines_of_code")
    private Integer linesOfCode = 0;

    @Column(name = "reliability_rating", precision = 3, scale = 2)
    private BigDecimal reliabilityRating = BigDecimal.ZERO;

    @Column(name = "security_rating", precision = 3, scale = 2)
    private BigDecimal securityRating = BigDecimal.ZERO;

    @Column(name = "maintainability_rating", precision = 3, scale = 2)
    private BigDecimal maintainabilityRating = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
