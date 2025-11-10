package lk.sampath.leaderboard.repository;

import lk.sampath.leaderboard.entity.Project;
import lk.sampath.leaderboard.entity.ProjectRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRankingRepository extends JpaRepository<ProjectRanking, Integer> {
    @Query("SELECT pr FROM ProjectRanking pr " +
            "WHERE pr.rankingPeriod = :period " +
            "AND pr.defectTerminatorRank <= 3 " +
            "ORDER BY pr.defectTerminatorRank ASC")
    List<ProjectRanking> findTop3DefectTerminators(@Param("period") LocalDate period);

    @Query("SELECT pr FROM ProjectRanking pr " +
            "WHERE pr.rankingPeriod = :period " +
            "AND pr.codeRockRank <= 3 " +
            "ORDER BY pr.codeRockRank ASC")
    List<ProjectRanking> findTop3CodeRock(@Param("period") LocalDate period);

    @Query("SELECT pr FROM ProjectRanking pr " +
            "WHERE pr.rankingPeriod = :period " +
            "AND pr.codeShieldRank <= 3 " +
            "ORDER BY pr.codeShieldRank ASC")
    List<ProjectRanking> findTop3CodeShield(@Param("period") LocalDate period);

    @Query("SELECT pr FROM ProjectRanking pr " +
            "WHERE pr.rankingPeriod = :period " +
            "AND pr.craftsmanRank <= 3 " +
            "ORDER BY pr.craftsmanRank ASC")
    List<ProjectRanking> findTop3Craftsman(@Param("period") LocalDate period);

    List<ProjectRanking> findByRankingPeriod(LocalDate period);
    Optional<ProjectRanking> findByProjectAndRankingPeriod(Project project, LocalDate rankingPeriod);

}


