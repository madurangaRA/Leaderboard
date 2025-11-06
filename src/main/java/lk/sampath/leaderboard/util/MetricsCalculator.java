package lk.sampath.leaderboard.util;


import java.math.BigDecimal;
import java.math.RoundingMode;

public class MetricsCalculator {

    private static final BigDecimal KLOC_DIVISOR = new BigDecimal(1000);
    private static final int SCALE = 4;

    /**
     * Calculates bugs per KLOC (Thousands of Lines of Code)
     * Lower is better
     */
    public static BigDecimal calculateBugsPerKloc(Integer bugCount, BigDecimal linesOfCode) {
        if (linesOfCode == null || linesOfCode.compareTo(BigDecimal.ZERO) <= 0 || bugCount == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(bugCount)
                .divide(linesOfCode.divide(KLOC_DIVISOR, SCALE, RoundingMode.HALF_UP), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculates vulnerabilities per KLOC
     * Lower is better
     */
    public static BigDecimal calculateVulnerabilitiesPerKloc(Integer vulnCount, BigDecimal linesOfCode) {
        if (linesOfCode == null || linesOfCode.compareTo(BigDecimal.ZERO) <= 0 || vulnCount == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(vulnCount)
                .divide(linesOfCode.divide(KLOC_DIVISOR, SCALE, RoundingMode.HALF_UP), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculates code smells per KLOC
     * Lower is better
     */
    public static BigDecimal calculateCodeSmellsPerKloc(Integer smellCount, BigDecimal linesOfCode) {
        if (linesOfCode == null || linesOfCode.compareTo(BigDecimal.ZERO) <= 0 || smellCount == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(smellCount)
                .divide(linesOfCode.divide(KLOC_DIVISOR, SCALE, RoundingMode.HALF_UP), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculates Defect Terminator score
     * Score = violations resolved - violations introduced
     * Higher is better (positive score means more fixed than created)
     */
    public static Integer calculateDefectTerminatorScore(Integer resolved, Integer introduced) {
        int resInt = resolved != null ? resolved : 0;
        int introInt = introduced != null ? introduced : 0;
        return resInt - introInt;
    }

    /**
     * Calculates Code Rock score (Reliability)
     * Lower bugs per KLOC = Higher score
     * Score formula: max(0, 10 - bugsPerKloc)
     */
    public static BigDecimal calculateCodeRockScore(BigDecimal bugsPerKloc) {
        if (bugsPerKloc == null || bugsPerKloc.compareTo(BigDecimal.ZERO) < 0) {
            return new BigDecimal(10);
        }
        BigDecimal score = new BigDecimal(10).subtract(bugsPerKloc);
        return score.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : score;
    }

    /**
     * Calculates Code Shield score (Security)
     * Lower vulnerabilities per KLOC = Higher score
     * Score formula: max(0, 10 - vulnerabilitiesPerKloc)
     */
    public static BigDecimal calculateCodeShieldScore(BigDecimal vulnerabilitiesPerKloc) {
        if (vulnerabilitiesPerKloc == null || vulnerabilitiesPerKloc.compareTo(BigDecimal.ZERO) < 0) {
            return new BigDecimal(10);
        }
        BigDecimal score = new BigDecimal(10).subtract(vulnerabilitiesPerKloc);
        return score.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : score;
    }

    /**
     * Calculates Craftsman score (Maintainability)
     * Lower code smells per KLOC = Higher score
     * Score formula: max(0, 10 - codeSmellsPerKloc)
     */
    public static BigDecimal calculateCraftsmanScore(BigDecimal codeSmellsPerKloc) {
        if (codeSmellsPerKloc == null || codeSmellsPerKloc.compareTo(BigDecimal.ZERO) < 0) {
            return new BigDecimal(10);
        }
        BigDecimal score = new BigDecimal(10).subtract(codeSmellsPerKloc);
        return score.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : score;
    }

    /**
     * Calculates rank improvement (Climber score)
     * Positive value means improvement (moved up in rankings)
     */
    public static BigDecimal calculateRankImprovement(Integer previousRank, Integer currentRank) {
        if (previousRank == null || currentRank == null) {
            return BigDecimal.ZERO;
        }
        // Positive = improved (was lower rank, now higher rank means lower number)
        return new BigDecimal(previousRank - currentRank);
    }

    /**
     * Calculates average rank improvement over a period
     */
    public static BigDecimal calculateAverageRankImprovement(BigDecimal totalImprovement, Integer periodCount) {
        if (totalImprovement == null || periodCount == null || periodCount == 0) {
            return BigDecimal.ZERO;
        }
        return totalImprovement.divide(new BigDecimal(periodCount), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Normalizes a score to 0-100 scale
     */
    public static BigDecimal normalizeScore(BigDecimal score, BigDecimal maxValue) {
        if (score == null || maxValue == null || maxValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return score.divide(maxValue, SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100));
    }

    /**
     * Converts KLOC to lines of code
     */
    public static BigDecimal klocToLines(BigDecimal kloc) {
        if (kloc == null) {
            return BigDecimal.ZERO;
        }
        return kloc.multiply(KLOC_DIVISOR);
    }

    /**
     * Converts lines of code to KLOC
     */
    public static BigDecimal linesToKloc(Long lines) {
        if (lines == null || lines <= 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(lines).divide(KLOC_DIVISOR, SCALE, RoundingMode.HALF_UP);
    }
}
