package com.example.studentmanager.utils;

import com.example.studentmanager.dal.DbContract;
import com.example.studentmanager.models.ScheduleItem;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Sáng: lịch tháng bắt đầu từ thứ Hai, không truy vấn hay ghi database. */
public final class MonthCalendarHelper {
    private MonthCalendarHelper() {}

    /** Ô trống là null; số ô luôn đủ các hàng bảy ngày. */
    public static List<LocalDate> cells(YearMonth month) {
        List<LocalDate> days = new ArrayList<>();
        int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < offset; i++) days.add(null);
        for (int day = 1; day <= month.lengthOfMonth(); day++) days.add(month.atDay(day));
        while (days.size() % 7 != 0) days.add(null);
        return days;
    }

    /** Chỉ đếm lịch hiệu lực của phiên hiện tại, trong đúng tháng đang xem. */
    public static List<ScheduleItem> activeSchedules(List<ScheduleItem> items,
                                                    YearMonth month, long studentId) {
        List<ScheduleItem> result = new ArrayList<>();
        if (studentId <= 0) return result;
        for (ScheduleItem item : items) {
            if (item == null || item.studentId != studentId
                    || (!DbContract.PENDING.equals(item.status) && !DbContract.CONFIRMED.equals(item.status))
                    || item.studyDate == null) continue;
            try {
                if (YearMonth.from(LocalDate.parse(item.studyDate)).equals(month)) result.add(item);
            } catch (DateTimeParseException ignored) {
                // Không để một bản ghi ngày sai làm hỏng cả lưới.
            }
        }
        result.sort(Comparator.comparing((ScheduleItem item) -> item.studyDate)
                .thenComparing(item -> item.startTime == null ? "" : item.startTime)
                .thenComparingLong(item -> item.id));
        return result;
    }
}
