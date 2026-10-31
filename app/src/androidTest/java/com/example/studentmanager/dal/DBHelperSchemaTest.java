package com.example.studentmanager.dal;

import static org.junit.Assert.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DBHelperSchemaTest {
    private DBHelper helper;
    private SQLiteDatabase db;

    @Before public void openIsolatedDatabase() {
        helper = new DBHelper(ApplicationProvider.getApplicationContext(), null);
        db = helper.getWritableDatabase();
    }
    @After public void closeDatabase() { helper.close(); }

    private long count(String sql) {
        try (Cursor cursor = db.rawQuery(sql, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getLong(0);
        }
    }

    @Test public void createsFourBusinessTablesAndSeedsOnce() {
        assertEquals(4, count("SELECT COUNT(*) FROM sqlite_master WHERE type='table' "
                + "AND name IN ('Account','Student','Subject','StudySchedule')"));
        assertEquals(3, count("SELECT COUNT(*) FROM Account"));
        helper.getWritableDatabase();
        assertEquals(3, count("SELECT COUNT(*) FROM Account"));
        assertEquals(1, count("PRAGMA foreign_keys"));
    }

    @Test public void deletingStudentAccountCascadesToProfileAndSchedules() {
        assertEquals(1, db.delete("Account", "id=?", new String[]{"2"}));
        assertEquals(0, count("SELECT COUNT(*) FROM Student WHERE accountId=2"));
        assertEquals(0, count("SELECT COUNT(*) FROM StudySchedule WHERE studentId=1"));
        assertEquals(1, count("SELECT COUNT(*) FROM StudySchedule WHERE studentId=2"));
    }

    @Test public void preventsDeletingSubjectUsedByASchedule() {
        try {
            db.delete("Subject", "id=?", new String[]{"1"});
            fail("Used subject must not be deleted");
        } catch (SQLiteConstraintException expected) {
            assertEquals(1, count("SELECT COUNT(*) FROM Subject WHERE id=1"));
        }
    }

    @Test public void invalidForeignKeyCannotCreateOrphanSchedule() {
        try {
            db.execSQL("INSERT INTO StudySchedule(studentId,subjectId,studyDate,startTime,endTime,location,status) "
                    + "VALUES(999,1,'2099-01-01','09:00','10:00','Test','PENDING')");
            fail("Orphan schedule must be rejected");
        } catch (SQLiteConstraintException expected) {
            assertEquals(2, count("SELECT COUNT(*) FROM StudySchedule"));
        }
    }
}
