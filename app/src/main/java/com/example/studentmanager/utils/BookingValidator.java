package com.example.studentmanager.utils;

import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.BookingDraft;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Kiem tra dau vao. Truy van lich trung nam trong BookingRepository. */
public final class BookingValidator {
    private BookingValidator() {}

    public static ActionResult validate(BookingDraft draft) {
        return validate(draft, LocalDateTime.now());
    }

    // Truyen thoi gian co dinh khi test, tranh test phu thuoc gio chay.
    static ActionResult validate(BookingDraft draft, LocalDateTime now) {
        if (draft == null || draft.subjectId <= 0) {
            return invalid("Vui lòng chọn môn học.");
        }
        if (draft.location == null || draft.location.trim().isEmpty()) {
            return invalid("Vui lòng nhập địa điểm học.");
        }

        String date = clean(draft.studyDate);
        String start = clean(draft.startTime);
        String end = clean(draft.endTime);
        if (!date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")
                || !start.matches("[0-9]{2}:[0-9]{2}")
                || !end.matches("[0-9]{2}:[0-9]{2}")) {
            return invalid("Vui lòng chọn đầy đủ ngày và giờ học.");
        }

        try {
            LocalDate day = LocalDate.parse(date, DateTimeHelper.DB_DATE);
            LocalTime startTime = LocalTime.parse(start, DateTimeHelper.TIME);
            LocalTime endTime = LocalTime.parse(end, DateTimeHelper.TIME);
            if (!startTime.isBefore(endTime)) {
                return invalid("Giờ bắt đầu phải nhỏ hơn giờ kết thúc trong cùng ngày.");
            }
            if (!LocalDateTime.of(day, startTime).isAfter(now)) {
                return invalid("Thời điểm bắt đầu phải sau thời điểm hiện tại.");
            }
            return ActionResult.success(-1);
        } catch (DateTimeException error) {
            return invalid("Ngày hoặc giờ học không hợp lệ.");
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static ActionResult invalid(String message) {
        return ActionResult.error(ActionResult.VALIDATION, message);
    }
}
