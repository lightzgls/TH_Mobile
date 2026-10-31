package com.example.studentmanager.dal;

import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.utils.AuthGuard;
import com.example.studentmanager.utils.SessionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Chay tren emulator test co du lieu demo. Khong sua/xoa bang du lieu. */
@RunWith(AndroidJUnit4.class)
public class AnLoginTest {
    private Context context;
    private AuthRepository auth;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        auth = new AuthRepository(context);
        auth.logout();
    }

    @After public void tearDown() { auth.logout(); }

    @Test public void validatesInputAndRejectsWrongCredentials() {
        assertEquals(ActionResult.VALIDATION, auth.login(null, "x").code);
        assertEquals(ActionResult.VALIDATION, auth.login(" ", "x").code);
        assertEquals(ActionResult.VALIDATION, auth.login("admin", " ").code);
        assertEquals(ActionResult.FORBIDDEN, auth.login("admin", "wrong").code);
        assertEquals(ActionResult.FORBIDDEN, auth.login("' OR 1=1 --", "wrong").code);
        assertNull(new SessionManager(context).getSession());
    }

    @Test public void adminLoginNormalizesUsernameAndLogoutRevokesAccess() {
        assertTrue(auth.login(" ADMIN ", "Admin@123").ok);
        AuthGuard guard = new AuthGuard(context);
        assertTrue(guard.requireAdmin());
        assertNull(guard.requireStudent());
        Session restored = new SessionManager(context).getSession();
        assertEquals("ADMIN", restored.role);
        assertEquals(-1, restored.studentId);
        auth.logout();
        assertFalse(guard.requireAdmin());
        assertNull(new SessionManager(context).getSession());
    }

    @Test public void studentLoginCannotUseAdminRole() {
        assertTrue(auth.login("sv01", "Student@123").ok);
        AuthGuard guard = new AuthGuard(context);
        assertFalse(guard.requireAdmin());
        assertEquals(1, guard.requireStudent().studentId);
    }
}
