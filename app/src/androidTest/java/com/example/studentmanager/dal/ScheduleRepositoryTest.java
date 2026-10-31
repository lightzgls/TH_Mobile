package com.example.studentmanager.dal;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.ScheduleFilter;
import com.example.studentmanager.models.ScheduleItem;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.utils.AuthGuard;
import java.time.LocalDate;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ScheduleRepositoryTest {
    private DBHelper helper;
    private SQLiteDatabase db;
    private Context context;
    private TestAuthGuard authGuard;
    private ScheduleRepository repository;

    static class TestAuthGuard extends AuthGuard {
        private Session session;

        TestAuthGuard(Context context) {
            super(context);
        }

        void setSession(Session session) {
            this.session = session;
        }

        @Override
        public Session requireStudent() {
            return session;
        }
    }

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        helper = new DBHelper(context, null);
        db = helper.getWritableDatabase();
        // Bộ test repository dùng hai lịch cố định, độc lập với số lượng sample chung.
        db.delete(DbContract.Schedules.TABLE, null, null);
        String tomorrow = LocalDate.now().plusDays(1).toString();
        db.execSQL("INSERT INTO StudySchedule VALUES(1,1,1,?,'09:00','10:00','Phòng A101','PENDING')", new Object[]{tomorrow});
        db.execSQL("INSERT INTO StudySchedule VALUES(2,2,2,?,'10:00','11:00','Phòng A102','CONFIRMED')", new Object[]{tomorrow});
        authGuard = new TestAuthGuard(context);
        repository = new ScheduleRepository(context, helper, authGuard);
    }

    @After
    public void tearDown() {
        helper.close();
    }

    private Session studentSession(long studentId, String username) {
        Session s = new Session();
        s.accountId = studentId + 1;
        s.studentId = studentId;
        s.username = username;
        s.role = DbContract.STUDENT;
        return s;
    }

    @Test
    public void unauthenticatedCallsReturnEmptyOrForbidden() {
        authGuard.setSession(null);

        List<ScheduleItem> list = repository.listMine(null);
        assertNotNull(list);
        assertTrue(list.isEmpty());

        assertNull(repository.getMine(1));

        ActionResult setStatusRes = repository.setStatus(1, DbContract.CONFIRMED);
        assertFalse(setStatusRes.ok);
        assertEquals(ActionResult.FORBIDDEN, setStatusRes.code);

        ActionResult deleteRes = repository.deleteMine(1);
        assertFalse(deleteRes.ok);
        assertEquals(ActionResult.FORBIDDEN, deleteRes.code);
    }

    @Test
    public void studentSeesOnlyOwnSchedulesWithSubjectDetails() {
        authGuard.setSession(studentSession(1, "sv01"));

        List<ScheduleItem> list = repository.listMine(null);
        assertEquals(1, list.size());
        ScheduleItem item = list.get(0);
        assertEquals(1, item.id);
        assertEquals(1, item.studentId);
        assertEquals("MOB101", item.subjectCode);
        assertEquals("Lập trình di động", item.subjectName);
        assertEquals("Phòng A101", item.location);
        assertEquals(DbContract.PENDING, item.status);

        ScheduleItem fetched = repository.getMine(1);
        assertNotNull(fetched);
        assertEquals("MOB101", fetched.subjectCode);

        // Schedule 2 belongs to Student 2
        assertNull(repository.getMine(2));
    }

    @Test
    public void filterByKeywordDateRangeAndStatusWithCorrectOrder() {
        authGuard.setSession(studentSession(1, "sv01"));

        String day2 = LocalDate.now().plusDays(2).toString();
        String day3 = LocalDate.now().plusDays(3).toString();

        db.execSQL("INSERT INTO StudySchedule(studentId,subjectId,studyDate,startTime,endTime,location,status) "
                + "VALUES(1,2,?,'08:00','09:00','Phòng B201',?)",
                new Object[]{day2, DbContract.CONFIRMED});
        db.execSQL("INSERT INTO StudySchedule(studentId,subjectId,studyDate,startTime,endTime,location,status) "
                + "VALUES(1,3,?,'14:00','15:00','Phòng C301',?)",
                new Object[]{day3, DbContract.PENDING});
        db.execSQL("INSERT INTO StudySchedule(studentId,subjectId,studyDate,startTime,endTime,location,status) "
                + "VALUES(1,1,?,'10:00','11:00','Phòng A102',?)",
                new Object[]{day3, DbContract.CONFIRMED});

        // 1. Keyword search by subject name
        ScheduleFilter f1 = new ScheduleFilter();
        f1.keyword = "di động";
        List<ScheduleItem> res1 = repository.listMine(f1);
        assertEquals(2, res1.size());
        for (ScheduleItem item : res1) {
            assertEquals("MOB101", item.subjectCode);
        }

        // 2. Keyword search by location
        ScheduleFilter f2 = new ScheduleFilter();
        f2.keyword = "B201";
        List<ScheduleItem> res2 = repository.listMine(f2);
        assertEquals(1, res2.size());
        assertEquals("DB101", res2.get(0).subjectCode);

        // 3. Keyword search by subject code (case-insensitive)
        ScheduleFilter f3 = new ScheduleFilter();
        f3.keyword = "git";
        List<ScheduleItem> res3 = repository.listMine(f3);
        assertEquals(1, res3.size());
        assertEquals("GIT101", res3.get(0).subjectCode);

        // 4. Date range filter (inclusive)
        ScheduleFilter fDate = new ScheduleFilter();
        fDate.fromDate = day2;
        fDate.toDate = day2;
        List<ScheduleItem> resDate = repository.listMine(fDate);
        assertEquals(1, resDate.size());
        assertEquals("DB101", resDate.get(0).subjectCode);

        // 5. Status filter
        ScheduleFilter fStatus = new ScheduleFilter();
        fStatus.status = DbContract.CONFIRMED;
        List<ScheduleItem> resStatus = repository.listMine(fStatus);
        assertEquals(2, resStatus.size());
        for (ScheduleItem item : resStatus) {
            assertEquals(DbContract.CONFIRMED, item.status);
        }

        // 6. Ordering check (studyDate ASC, startTime ASC, id ASC)
        List<ScheduleItem> all = repository.listMine(null);
        assertEquals(4, all.size());
        for (int i = 0; i < all.size() - 1; i++) {
            ScheduleItem curr = all.get(i);
            ScheduleItem next = all.get(i + 1);
            int dateCmp = curr.studyDate.compareTo(next.studyDate);
            assertTrue(dateCmp <= 0);
            if (dateCmp == 0) {
                int timeCmp = curr.startTime.compareTo(next.startTime);
                assertTrue(timeCmp <= 0);
                if (timeCmp == 0) {
                    assertTrue(curr.id < next.id);
                }
            }
        }
    }

    @Test
    public void crossStudentAccessIsBlocked() {
        authGuard.setSession(studentSession(1, "sv01"));

        // Schedule 2 belongs to Student 2
        assertNull(repository.getMine(2));

        ActionResult setStatusRes = repository.setStatus(2, DbContract.CANCELLED);
        assertFalse(setStatusRes.ok);
        assertEquals(ActionResult.NOT_FOUND, setStatusRes.code);

        ActionResult deleteRes = repository.deleteMine(2);
        assertFalse(deleteRes.ok);
        assertEquals(ActionResult.NOT_FOUND, deleteRes.code);

        // Verify Student 2's schedule is completely intact
        authGuard.setSession(studentSession(2, "sv02"));
        ScheduleItem item2 = repository.getMine(2);
        assertNotNull(item2);
        assertEquals(DbContract.CONFIRMED, item2.status);
    }

    @Test
    public void statusTransitionsAndStartedScheduleValidation() {
        authGuard.setSession(studentSession(1, "sv01"));

        // Seeded schedule 1 is in future and currently PENDING
        // Valid: PENDING -> CONFIRMED
        ActionResult r1 = repository.setStatus(1, DbContract.CONFIRMED);
        assertTrue(r1.ok);
        assertEquals(DbContract.CONFIRMED, repository.getMine(1).status);

        // Valid: CONFIRMED -> CANCELLED
        ActionResult r2 = repository.setStatus(1, DbContract.CANCELLED);
        assertTrue(r2.ok);
        assertEquals(DbContract.CANCELLED, repository.getMine(1).status);

        // Invalid: CANCELLED -> CONFIRMED
        ActionResult r3 = repository.setStatus(1, DbContract.CONFIRMED);
        assertFalse(r3.ok);
        assertEquals(ActionResult.VALIDATION, r3.code);

        // Invalid: CANCELLED -> PENDING
        ActionResult r4 = repository.setStatus(1, DbContract.PENDING);
        assertFalse(r4.ok);
        assertEquals(ActionResult.VALIDATION, r4.code);

        // Test past schedule rejection
        String yesterday = LocalDate.now().minusDays(1).toString();
        db.execSQL("INSERT INTO StudySchedule(studentId,subjectId,studyDate,startTime,endTime,location,status) "
                + "VALUES(1,1,?,'09:00','10:00','Phòng A101',?)",
                new Object[]{yesterday, DbContract.PENDING});

        List<ScheduleItem> mine = repository.listMine(null);
        long pastScheduleId = -1;
        for (ScheduleItem it : mine) {
            if (yesterday.equals(it.studyDate)) {
                pastScheduleId = it.id;
                break;
            }
        }
        assertTrue(pastScheduleId > 0);

        ActionResult rPast = repository.setStatus(pastScheduleId, DbContract.CONFIRMED);
        assertFalse(rPast.ok);
        assertEquals(ActionResult.VALIDATION, rPast.code);
        assertEquals("Không thể đổi trạng thái lịch học đã bắt đầu", rPast.message);
    }

    @Test
    public void deleteMineRemovesRecordAndSecondDeleteFails() {
        authGuard.setSession(studentSession(1, "sv01"));

        String futureDate = LocalDate.now().plusDays(5).toString();
        db.execSQL("INSERT INTO StudySchedule(studentId,subjectId,studyDate,startTime,endTime,location,status) "
                + "VALUES(1,1,?,'08:00','09:00','Phòng X',?)",
                new Object[]{futureDate, DbContract.PENDING});

        List<ScheduleItem> list = repository.listMine(null);
        long idToDelete = -1;
        for (ScheduleItem it : list) {
            if (futureDate.equals(it.studyDate)) {
                idToDelete = it.id;
                break;
            }
        }
        assertTrue(idToDelete > 0);

        // First delete -> success
        ActionResult del1 = repository.deleteMine(idToDelete);
        assertTrue(del1.ok);
        assertNull(repository.getMine(idToDelete));

        // Second delete on same id -> NOT_FOUND
        ActionResult del2 = repository.deleteMine(idToDelete);
        assertFalse(del2.ok);
        assertEquals(ActionResult.NOT_FOUND, del2.code);
    }
}
