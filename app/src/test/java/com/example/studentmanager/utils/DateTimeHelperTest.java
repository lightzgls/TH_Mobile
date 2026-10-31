package com.example.studentmanager.utils;

import static org.junit.Assert.assertEquals;
import java.time.format.DateTimeParseException;
import org.junit.Test;

public class DateTimeHelperTest {
    @Test public void leapDateRoundTripsWithoutChangingFormat() {
        assertEquals("29/02/2028", DateTimeHelper.displayDate("2028-02-29"));
        assertEquals("2028-02-29", DateTimeHelper.databaseDate("29/02/2028"));
        assertEquals("09:05", DateTimeHelper.databaseTime(9,5));
    }
    @Test(expected = DateTimeParseException.class)
    public void rejectsInvalidLeapDayInsteadOfNormalizingIt() {
        DateTimeHelper.databaseDate("29/02/2027");
    }
}
