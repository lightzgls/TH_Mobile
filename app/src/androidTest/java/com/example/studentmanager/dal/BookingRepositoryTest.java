package com.example.studentmanager.dal;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.BookingDraft;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.utils.AuthGuard;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BookingRepositoryTest {
    private DBHelper helper;
    private SQLiteDatabase db;
    private Session session;
    private BookingRepository repository;
    private String studyDate;
    private int notifications;
    private boolean notificationFailure;

    @Before public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        helper = new DBHelper(context, null);
        db = helper.getWritableDatabase();
        db.delete("StudySchedule", null, null);
        studyDate = LocalDate.now().plusDays(2).toString();
        session = new Session();
        session.accountId = 2;
        session.studentId = 1;
        session.role = DbContract.STUDENT;
        AuthGuard guard = new AuthGuard(context) {
            @Override public Session requireStudent() { return session; }
        };
        repository = new BookingRepository(context, helper, guard) {
            @Override protected void notifySaved(long studentId, long scheduleId, boolean updated) {
                assertFalse("Notifications must run after the transaction", db.inTransaction());
                assertEquals(1, studentId);
                assertEquals(scheduleId, rowId(scheduleId));
                notifications++;
                if (notificationFailure) throw new IllegalStateException("Notification unavailable");
            }
        };
    }

    @After public void tearDown() { helper.close(); }

    private BookingDraft draft(String start, String end) {
        BookingDraft draft = new BookingDraft();
        draft.subjectId = 1;
        draft.studyDate = studyDate;
        draft.startTime = start;
        draft.endTime = end;
        draft.location = " A101 ";
        return draft;
    }

    private long insert(long studentId, String date, String start, String end, String status) {
        db.execSQL("INSERT INTO StudySchedule(studentId,subjectId,studyDate,startTime,endTime,location,status) "
                        + "VALUES(?,1,?,?,?,'A101',?)", new Object[]{studentId, date, start, end, status});
        try (Cursor cursor = db.rawQuery("SELECT last_insert_rowid()", null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getLong(0);
        }
    }

    private long rowId(long id) {
        try (Cursor cursor = db.rawQuery("SELECT id FROM StudySchedule WHERE id = ?",
                new String[]{String.valueOf(id)})) {
            return cursor.moveToFirst() ? cursor.getLong(0) : -1;
        }
    }

    private int count() {
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM StudySchedule", null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }

    private void assertCode(String expected, ActionResult result) {
        assertFalse(result.ok);
        assertEquals(expected, result.code);
        assertEquals(-1, result.id);
        assertFalse(db.inTransaction());
    }

    @Test public void createsPendingScheduleWithTrimmedValues() {
        BookingDraft draft = draft("09:00", "10:00");
        draft.studyDate = " " + studyDate + " ";
        draft.startTime = " 09:00 ";
        ActionResult result = repository.save(-1, draft);
        assertTrue(result.message, result.ok);
        assertTrue(result.id > 0);
        try (Cursor cursor = db.rawQuery("SELECT studentId,status,location,studyDate,startTime FROM StudySchedule", null)) {
            assertTrue(cursor.moveToFirst());
            assertEquals(1, cursor.getLong(0));
            assertEquals(DbContract.PENDING, cursor.getString(1));
            assertEquals("A101", cursor.getString(2));
            assertEquals(studyDate, cursor.getString(3));
            assertEquals("09:00", cursor.getString(4));
        }
        assertEquals(1, notifications);
    }

    @Test public void blocksEveryOverlapIncludingContainingAndContainedRanges() {
        insert(1, studyDate, "09:00", "10:00", DbContract.CONFIRMED);
        String[][] overlaps = {{"09:30", "10:30"}, {"08:00", "11:00"},
                {"09:00", "10:00"}, {"09:15", "09:45"}, {"08:30", "09:30"}};
        for (String[] times : overlaps) {
            BookingDraft draft = draft(times[0], times[1]);
            draft.subjectId = 2;
            draft.location = "Phòng khác";
            assertCode(ActionResult.CONFLICT, repository.save(-1, draft));
        }
        assertEquals(1, count());
        assertEquals(0, notifications);
    }

    @Test public void allowsAdjacentSessionsOnEitherSide() {
        insert(1, studyDate, "09:00", "10:00", DbContract.PENDING);
        assertTrue(repository.save(-1, draft("08:00", "09:00")).ok);
        assertTrue(repository.save(-1, draft("10:00", "11:00")).ok);
        assertEquals(3, count());
    }

    @Test public void ignoresCancelledOtherStudentAndOtherDateSchedules() {
        insert(1, studyDate, "09:00", "10:00", DbContract.CANCELLED);
        insert(2, studyDate, "09:00", "10:00", DbContract.CONFIRMED);
        insert(1, LocalDate.parse(studyDate).plusDays(1).toString(), "09:00", "10:00", DbContract.PENDING);
        assertTrue(repository.save(-1, draft("09:15", "09:45")).ok);
        assertEquals(4, count());
    }

    @Test public void editExcludesItselfAndPreservesConfirmedStatus() {
        long id = insert(1, studyDate, "09:00", "10:00", DbContract.CONFIRMED);
        assertTrue(repository.save(id, draft("09:00", "10:00")).ok);
        assertEquals(1, count());
        try (Cursor cursor = db.rawQuery("SELECT status FROM StudySchedule WHERE id = ?",
                new String[]{String.valueOf(id)})) {
            assertTrue(cursor.moveToFirst());
            assertEquals(DbContract.CONFIRMED, cursor.getString(0));
        }
        assertEquals(1, notifications);
    }

    @Test public void conflictingEditLeavesOriginalScheduleUnchanged() {
        long id = insert(1, studyDate, "09:00", "10:00", DbContract.PENDING);
        insert(1, studyDate, "11:00", "12:00", DbContract.CONFIRMED);
        assertCode(ActionResult.CONFLICT, repository.save(id, draft("11:30", "12:30")));
        try (Cursor cursor = db.rawQuery("SELECT startTime,endTime FROM StudySchedule WHERE id = ?",
                new String[]{String.valueOf(id)})) {
            assertTrue(cursor.moveToFirst());
            assertEquals("09:00", cursor.getString(0));
            assertEquals("10:00", cursor.getString(1));
        }
        assertEquals(0, notifications);
    }

    @Test public void rejectsOtherStudentsAndDeletedSchedules() {
        long otherId = insert(2, studyDate, "09:00", "10:00", DbContract.PENDING);
        assertCode(ActionResult.NOT_FOUND, repository.save(otherId, draft("10:00", "11:00")));
        assertCode(ActionResult.NOT_FOUND, repository.save(99999, draft("10:00", "11:00")));
        assertEquals(1, count());
    }

    @Test public void rejectsEditingCancelledAndAlreadyStartedSchedules() {
        long cancelled = insert(1, studyDate, "09:00", "10:00", DbContract.CANCELLED);
        assertCode(ActionResult.VALIDATION, repository.save(cancelled, draft("10:00", "11:00")));
        LocalDateTime past = LocalDateTime.now().minusDays(1).withHour(9).withMinute(0);
        long started = insert(1, past.toLocalDate().toString(), "09:00", "10:00", DbContract.PENDING);
        assertCode(ActionResult.VALIDATION, repository.save(started, draft("10:00", "11:00")));
        assertEquals(2, count());
    }

    @Test public void rejectsInvalidInputsAndMissingSubjectWithoutWriting() {
        assertCode(ActionResult.VALIDATION, repository.save(-1, null));
        assertCode(ActionResult.VALIDATION, repository.save(0, draft("09:00", "10:00")));
        assertCode(ActionResult.VALIDATION, repository.save(-1, draft("10:00", "09:00")));
        BookingDraft draft = draft("09:00", "10:00");
        draft.studyDate = LocalDate.now().minusDays(1).toString();
        assertCode(ActionResult.VALIDATION, repository.save(-1, draft));
        draft = draft("09:00", "10:00");
        draft.subjectId = 99999;
        assertCode(ActionResult.NOT_FOUND, repository.save(-1, draft));
        assertEquals(0, count());
        assertEquals(0, notifications);
    }

    @Test public void requiresStudentSession() {
        session = null;
        assertCode(ActionResult.FORBIDDEN, repository.save(-1, draft("09:00", "10:00")));
        session = new Session();
        session.studentId = 1;
        session.role = DbContract.ADMIN;
        assertCode(ActionResult.FORBIDDEN, repository.save(-1, draft("09:00", "10:00")));
        assertEquals(0, count());
    }

    @Test public void notificationFailureDoesNotUndoSuccessfulSave() {
        notificationFailure = true;
        ActionResult result = repository.save(-1, draft("09:00", "10:00"));
        assertTrue(result.ok);
        assertEquals(result.id, rowId(result.id));
        assertEquals(1, count());
    }

    @Test public void simultaneousRequestsCannotCreateOverlappingSchedules() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<ActionResult> first = executor.submit(() -> {
                start.await();
                return repository.save(-1, draft("09:00", "10:00"));
            });
            Future<ActionResult> second = executor.submit(() -> {
                start.await();
                return repository.save(-1, draft("09:00", "10:00"));
            });
            start.countDown();
            ActionResult a = first.get(30, TimeUnit.SECONDS);
            ActionResult b = second.get(30, TimeUnit.SECONDS);
            assertTrue("Exactly one request must succeed", a.ok != b.ok);
            assertEquals(ActionResult.CONFLICT, a.ok ? b.code : a.code);
            assertEquals(1, count());
            assertEquals(1, notifications);
        } finally {
            executor.shutdownNow();
        }
    }
}
