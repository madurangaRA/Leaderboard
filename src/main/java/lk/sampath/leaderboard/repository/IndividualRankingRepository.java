package lk.sampath.leaderboard.repository;

import lk.sampath.leaderboard.entity.Developer;
import lk.sampath.leaderboard.entity.IndividualRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndividualRankingRepository extends JpaRepository<IndividualRanking, Integer> {
    @Query("SELECT ir FROM IndividualRanking ir " +
            "WHERE ir.rankingPeriod = :period " +
            "AND ir.defectTerminatorRank <= 3 " +
            "ORDER BY ir.defectTerminatorRank ASC")
    List<IndividualRanking> findTop3DefectTerminators(@Param("period") LocalDate period);

    @Query("SELECT ir FROM IndividualRanking ir " +
            "WHERE ir.rankingPeriod = :period " +
            "AND ir.codeRockRank <= 3 " +
            "ORDER BY ir.codeRockRank ASC")
    List<IndividualRanking> findTop3CodeRock(@Param("period") LocalDate period);

    @Query("SELECT ir FROM IndividualRanking ir " +
            "WHERE ir.rankingPeriod = :period " +
            "AND ir.codeShieldRank <= 3 " +
            "ORDER BY ir.codeShieldRank ASC")
    List<IndividualRanking> findTop3CodeShield(@Param("period") LocalDate period);

    @Query("SELECT ir FROM IndividualRanking ir " +
            "WHERE ir.rankingPeriod = :period " +
            "AND ir.craftsmanRank <= 3 " +
            "ORDER BY ir.craftsmanRank ASC")
    List<IndividualRanking> findTop3Craftsman(@Param("period") LocalDate period);

    @Query("SELECT ir FROM IndividualRanking ir " +
            "WHERE ir.rankingPeriod = :period " +
            "AND ir.climberRank <= 3 " +
            "ORDER BY ir.climberRank ASC")
    List<IndividualRanking> findTop3Climber(@Param("period") LocalDate period);

    Optional<IndividualRanking> findByDeveloperAndRankingPeriod(Developer developer, LocalDate period);
    List<IndividualRanking> findByRankingPeriod(LocalDate period);
}

