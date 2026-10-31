package com.example.studentmanager.utils;

import static org.junit.Assert.*;
import com.example.studentmanager.models.ScheduleItem;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class MonthCalendarHelperTest {
    @Test public void rendersMonthsOfEveryLengthWithMondayFirstAndFullRows() {
        for (YearMonth month : Arrays.asList(YearMonth.of(2027, 2), YearMonth.of(2028, 2),
                YearMonth.of(2026, 4), YearMonth.of(2026, 10))) {
            List<LocalDate> cells = MonthCalendarHelper.cells(month);
            assertEquals(0, cells.size() % 7);
            int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
            assertEquals(month.atDay(1), cells.get(offset));
            assertEquals(month.atEndOfMonth(), cells.get(offset + month.lengthOfMonth() - 1));
            assertEquals(month.lengthOfMonth(), cells.stream().filter(day -> day != null).count());
            for (int i = 0; i < offset; i++) assertNull(cells.get(i));
            for (int i = offset + month.lengthOfMonth(); i < cells.size(); i++) assertNull(cells.get(i));
        }
    }

    @Test public void handlesFourFiveAndSixWeekGridsAndCenturyLeapRule() {
        assertEquals(28, MonthCalendarHelper.cells(YearMonth.of(2021, 2)).size());
        assertEquals(35, MonthCalendarHelper.cells(YearMonth.of(2026, 4)).size());
        assertEquals(42, MonthCalendarHelper.cells(YearMonth.of(2026, 11)).size());
        assertEquals(28, YearMonth.of(2100, 2).lengthOfMonth());
        assertEquals(LocalDate.of(2000, 2, 29),
                MonthCalendarHelper.cells(YearMonth.of(2000, 2)).stream()
                        .filter(day -> day != null).reduce((a, b) -> b).get());
    }

    private ScheduleItem schedule(long id, long studentId, String date, String time, String status) {
        ScheduleItem item = new ScheduleItem();
        item.id = id; item.studentId = studentId; item.studyDate = date;
        item.startTime = time; item.status = status;
        return item;
    }

    @Test public void excludesCancelledOtherStudentsOtherMonthsAndInvalidDates() {
        List<ScheduleItem> items = Arrays.asList(
                schedule(1, 1, "2026-10-31", "10:00", "PENDING"),
                schedule(2, 1, "2026-10-31", "09:00", "CONFIRMED"),
                schedule(3, 1, "2026-10-31", "08:00", "CANCELLED"),
                schedule(4, 2, "2026-10-31", "08:00", "PENDING"),
                schedule(5, 1, "2026-11-01", "08:00", "PENDING"),
                schedule(6, 1, "invalid", "08:00", "PENDING"),
                schedule(7, 1, null, "08:00", "PENDING"));
        List<ScheduleItem> active = MonthCalendarHelper.activeSchedules(items, YearMonth.of(2026, 10), 1);
        assertEquals(2, active.size());
        assertEquals(2, active.get(0).id);
        assertEquals(1, active.get(1).id);
        assertTrue(MonthCalendarHelper.activeSchedules(items, YearMonth.of(2026, 10), -1).isEmpty());
    }

    @Test public void sortsTiesByIdAndDoesNotMutateSource() {
        List<ScheduleItem> source = Arrays.asList(
                schedule(20, 1, "2026-10-01", "09:00", "PENDING"),
                schedule(10, 1, "2026-10-01", "09:00", "PENDING"));
        assertEquals(10, MonthCalendarHelper.activeSchedules(source, YearMonth.of(2026, 10), 1).get(0).id);
        assertEquals(20, source.get(0).id);
    }
}
