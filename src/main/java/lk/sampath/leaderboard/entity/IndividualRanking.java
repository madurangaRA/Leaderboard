package lk.sampath.leaderboard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "individual_rankings", indexes = {
        @Index(name = "idx_ranking_period", columnList = "ranking_period"),
        @Index(name = "idx_defect_terminator", columnList = "defect_terminator_rank, ranking_period")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndividualRanking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developer_id", nullable = false)
    private Developer developer;

    @Column(name = "ranking_period", nullable = false)
    private LocalDate rankingPeriod;

    // Defect Terminator
    @Column(name = "defect_terminator_score")
    private Integer defectTerminatorScore;

    @Column(name = "defect_terminator_rank")
    private Integer defectTerminatorRank;

    @Column(name = "violations_resolved")
    private Integer violationsResolved;

    // Code Rock
    @Column(name = "code_rock_score")
    private BigDecimal codeRockScore;

    @Column(name = "code_rock_rank")
    private Integer codeRockRank;

    @Column(name = "bugs_per_kloc")
    private BigDecimal bugsPerKloc;

    // Code Shield
    @Column(name = "code_shield_score")
    private BigDecimal codeShieldScore;

    @Column(name = "code_shield_rank")
    private Integer codeShieldRank;

    @Column(name = "vulnerabilities_per_kloc")
    private BigDecimal vulnerabilitiesPerKloc;

    // Craftsman
    @Column(name = "craftsman_score")
    private BigDecimal craftsmanScore;

    @Column(name = "craftsman_rank")
    private Integer craftsmanRank;

    @Column(name = "code_smells_per_kloc")
    private BigDecimal codeSmellsPerKloc;

    // Climber
    @Column(name = "climber_score")
    private BigDecimal climberScore;

    @Column(name = "climber_rank")
    private Integer climberRank;

    @Column(name = "avg_rank_improvement")
    private BigDecimal avgRankImprovement;

    @Column(name = "total_kloc")
    private BigDecimal totalKloc;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
