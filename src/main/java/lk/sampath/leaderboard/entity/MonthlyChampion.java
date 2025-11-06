package lk.sampath.leaderboard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_champions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyChampion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private LocalDate period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChampionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Integer entityId;

    @Column(name = "entity_name", nullable = false, length = 255)
    private String entityName;

    @Column(nullable = false)
    private BigDecimal score;

    @Column(columnDefinition = "JSON")
    private String metricDetails;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ChampionCategory {
        DEFECT_TERMINATOR, CODE_ROCK, CODE_SHIELD, CRAFTSMAN, CLIMBER
    }
    public enum EntityType {
        INDIVIDUAL, PROJECT
    }
}
