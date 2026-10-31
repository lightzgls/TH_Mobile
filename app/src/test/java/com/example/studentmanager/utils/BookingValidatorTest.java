package com.example.studentmanager.utils;

import static org.junit.Assert.*;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.BookingDraft;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.Test;

public class BookingValidatorTest {
    private final LocalDateTime now = LocalDateTime.of(2030, 1, 15, 8, 0);

    private BookingDraft validDraft() {
        BookingDraft draft = new BookingDraft();
        draft.subjectId = 1;
        draft.studyDate = "2030-01-15";
        draft.startTime = "09:00";
        draft.endTime = "10:00";
        draft.location = "Phòng A101";
        return draft;
    }

    private void assertInvalid(BookingDraft draft) {
        ActionResult result = BookingValidator.validate(draft, now);
        assertFalse(result.ok);
        assertEquals(ActionResult.VALIDATION, result.code);
        assertFalse(result.message.isEmpty());
    }

    @Test public void acceptsFutureTimeTodayAndTrimsInput() {
        BookingDraft draft = validDraft();
        draft.studyDate = " 2030-01-15 ";
        draft.startTime = " 09:00 ";
        draft.location = " A101 ";
        assertTrue(BookingValidator.validate(draft, now).ok);
    }

    @Test public void publicMethodUsesCurrentTime() {
        BookingDraft draft = validDraft();
        draft.studyDate = LocalDate.now().plusDays(2).toString();
        assertTrue(BookingValidator.validate(draft).ok);
    }

    @Test public void requiresDraftAndSubject() {
        assertInvalid(null);
        BookingDraft draft = validDraft();
        draft.subjectId = 0;
        assertInvalid(draft);
        draft.subjectId = -1;
        assertInvalid(draft);
    }

    @Test public void requiresLocation() {
        BookingDraft draft = validDraft();
        draft.location = "  ";
        assertInvalid(draft);
        draft.location = null;
        assertInvalid(draft);
    }

    @Test public void rejectsMissingDateAndTimes() {
        BookingDraft draft = validDraft();
        draft.studyDate = null;
        assertInvalid(draft);
        draft = validDraft();
        draft.startTime = "";
        assertInvalid(draft);
        draft = validDraft();
        draft.endTime = null;
        assertInvalid(draft);
    }

    @Test public void requiresFixedDatabaseFormats() {
        BookingDraft draft = validDraft();
        draft.studyDate = "15/01/2030";
        assertInvalid(draft);
        draft.studyDate = "2030-1-15";
        assertInvalid(draft);
        draft = validDraft();
        draft.startTime = "9:00";
        assertInvalid(draft);
        draft.endTime = "10:00:00";
        assertInvalid(draft);
    }

    @Test public void rejectsImpossibleDatesAndHours() {
        BookingDraft draft = validDraft();
        draft.studyDate = "2030-02-29";
        assertInvalid(draft);
        draft.studyDate = "2030-04-31";
        assertInvalid(draft);
        draft = validDraft();
        draft.endTime = "24:00";
        assertInvalid(draft);
        draft.endTime = "10:60";
        assertInvalid(draft);
    }

    @Test public void acceptsRealLeapDay() {
        BookingDraft draft = validDraft();
        draft.studyDate = "2032-02-29";
        assertTrue(BookingValidator.validate(draft, now).ok);
    }

    @Test public void rejectsEqualOrReversedTimesAndOvernightSessions() {
        BookingDraft draft = validDraft();
        draft.endTime = "09:00";
        assertInvalid(draft);
        draft.endTime = "08:00";
        assertInvalid(draft);
        draft.startTime = "23:00";
        draft.endTime = "01:00";
        assertInvalid(draft);
    }

    @Test public void rejectsPastAndExactlyCurrentStart() {
        BookingDraft draft = validDraft();
        draft.studyDate = "2030-01-14";
        assertInvalid(draft);
        draft.studyDate = "2030-01-15";
        draft.startTime = "07:59";
        assertInvalid(draft);
        draft.startTime = "08:00";
        assertInvalid(draft);
    }
}
