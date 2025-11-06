package lk.sampath.leaderboard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "project_rankings", indexes = {
        @Index(name = "idx_ranking_period", columnList = "ranking_period"),
        @Index(name = "idx_defect_terminator", columnList = "defect_terminator_rank, ranking_period")
})
public class ProjectRanking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "ranking_period", nullable = false)
    private LocalDate rankingPeriod;

    @Column(name = "defect_terminator_score")
    private Integer defectTerminatorScore = 0;

    @Column(name = "defect_terminator_rank")
    private Integer defectTerminatorRank = 999;

    @Column(name = "violations_resolved")
    private Integer violationsResolved = 0;

    @Column(name = "code_rock_score")
    private BigDecimal codeRockScore = BigDecimal.ZERO;

    @Column(name = "code_rock_rank")
    private Integer codeRockRank = 999;

    @Column(name = "bugs_per_kloc")
    private BigDecimal bugsPerKloc = BigDecimal.ZERO;

    @Column(name = "code_shield_score")
    private BigDecimal codeShieldScore = BigDecimal.ZERO;

    @Column(name = "code_shield_rank")
    private Integer codeShieldRank = 999;

    @Column(name = "vulnerabilities_per_kloc")
    private BigDecimal vulnerabilitiesPerKloc = BigDecimal.ZERO;

    @Column(name = "craftsman_score")
    private BigDecimal craftsmanScore = BigDecimal.ZERO;

    @Column(name = "craftsman_rank")
    private Integer craftsmanRank = 999;

    @Column(name = "code_smells_per_kloc")
    private BigDecimal codeSmellsPerKloc = BigDecimal.ZERO;

    @Column(name = "total_kloc")
    private BigDecimal totalKloc = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

