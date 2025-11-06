package lk.sampath.leaderboard.repository;

import lk.sampath.leaderboard.entity.MonthlyChampion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyChampionRepository extends JpaRepository<MonthlyChampion, Integer> {

    List<MonthlyChampion> findByPeriod(LocalDate period);

    Optional<MonthlyChampion> findByPeriodAndCategoryAndEntityType(
            LocalDate period,
            MonthlyChampion.ChampionCategory category,
            MonthlyChampion.EntityType entityType
    );

    @Query("SELECT mc FROM MonthlyChampion mc WHERE mc.period = :period " +
            "AND mc.entityType = 'INDIVIDUAL' ORDER BY mc.category")
    List<MonthlyChampion> findIndividualChampionsByPeriod(@Param("period") LocalDate period);

    @Query("SELECT mc FROM MonthlyChampion mc WHERE mc.period = :period " +
            "AND mc.entityType = 'PROJECT' ORDER BY mc.category")
    List<MonthlyChampion> findProjectChampionsByPeriod(@Param("period") LocalDate period);

    @Modifying
    @Query("DELETE FROM MonthlyChampion mc WHERE mc.period = :period")
    void deleteByPeriod(@Param("period") LocalDate period);
}
