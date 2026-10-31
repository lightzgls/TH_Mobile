package com.example.studentmanager.dal;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.utils.SessionManager;
import com.example.studentmanager.utils.AppConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AnSessionTest {
    private DBHelper helper;
    private SQLiteDatabase db;

    @Before public void setUp() {
        helper = new DBHelper(ApplicationProvider.getApplicationContext(), null);
        db = helper.getWritableDatabase();
    }

    @After public void tearDown() { helper.close(); }
    @Test public void sessionIsInvalidAfterStudentAccountDeleted() {
        Context context = isolatedPreferences();
        SessionManager sessions =
                new SessionManager(context, helper);
        Session session = studentSession();
        sessions.saveSession(session);
        assertEquals(1, sessions.getSession().studentId);
        String firstToken = sessions.getSessionToken();
        sessions.saveSession(session);
        assertNotEquals(firstToken, sessions.getSessionToken());
        db.delete("Account", "id=?", new String[]{"2"});
        assertNull(sessions.getSession());
        assertEquals("", sessions.getSessionToken());
    }

    @Test public void forgedRoleAndMissingStudentProfileInvalidateSession() {
        Context context = isolatedPreferences();
        SessionManager sessions =
                new SessionManager(context, helper);
        sessions.saveSession(studentSession());
        context.getSharedPreferences(AppConstants.SESSION_PREFS, 0)
                .edit().putString("role", "ADMIN").commit();
        assertNull(sessions.getSession());
        sessions.saveSession(studentSession());
        db.delete("Student", "id=?", new String[]{"1"});
        assertNull(sessions.getSession());
    }

    @Test public void logoutClearsSessionAndCorruptPreferencesDoNotCrash() {
        Context context = isolatedPreferences();
        SessionManager sessions =
                new SessionManager(context, helper);
        sessions.saveSession(studentSession());
        sessions.clearSession();
        assertNull(sessions.getSession());
        context.getSharedPreferences(AppConstants.SESSION_PREFS, 0)
                .edit().putString("accountId", "bad data").commit();
        assertNull(sessions.getSession());
    }

    private Session studentSession() {
        Session session = new Session();
        session.accountId = 2;
        session.studentId = 1;
        session.username = "sv01";
        session.role = "STUDENT";
        return session;
    }

    private Context isolatedPreferences() {
        String name = "session_test_" + java.util.UUID.randomUUID();
        return new ContextWrapper(ApplicationProvider.getApplicationContext()) {
            @Override public SharedPreferences getSharedPreferences(String ignored, int mode) {
                return super.getSharedPreferences(name, mode);
            }
        };
    }
}
