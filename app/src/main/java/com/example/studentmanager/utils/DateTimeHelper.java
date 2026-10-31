package com.example.studentmanager.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/** Chi doi dinh dang; quy tac nghiep vu dat lich thuoc BookingValidator cua Thu. */
public final class DateTimeHelper {
    private DateTimeHelper() {}
    public static final DateTimeFormatter DB_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    public static String displayDate(String dbDate) {
        return LocalDate.parse(dbDate, DB_DATE).format(DISPLAY_DATE);
    }
    public static String databaseDate(String displayDate) {
        return LocalDate.parse(displayDate, DISPLAY_DATE).format(DB_DATE);
    }
    public static String databaseTime(int hour, int minute) {
        return LocalTime.of(hour, minute).format(TIME);
    }
}
