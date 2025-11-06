package lk.sampath.leaderboard.util;

import java.time.LocalDate;
import java.time.YearMonth;

public class DateUtils {
    public static LocalDate getCurrentMonthStart() {
        YearMonth yearMonth = YearMonth.now();
        return yearMonth.atDay(1);
    }

    public static LocalDate getMonthEnd(LocalDate date) {
        return date.plusMonths(1).minusDays(1);
    }
}
