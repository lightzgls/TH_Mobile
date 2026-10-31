package com.example.studentmanager.activities;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.lifecycle.Lifecycle;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.AuthRepository;
import com.example.studentmanager.utils.AppConstants;
import com.example.studentmanager.utils.DbExecutor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.runner.RunWith;

/** Kiem tra lai man hinh Sang sau khi ghep bo kiem tra phien cua An. */
@RunWith(AndroidJUnit4.class)
public class SubjectScreenTest {
    @Before public void clearSession() {
        new AuthRepository(ApplicationProvider.getApplicationContext()).logout();
    }

    @After public void logout() { clearSession(); }

    private void login(String username, String password) {
        assertTrue(new AuthRepository(ApplicationProvider.getApplicationContext()).login(username, password).ok);
    }

    private void awaitDatabase() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        DbExecutor.execute(done::countDown);
        assertTrue("Database task timed out", done.await(15, TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @Test public void adminListRequiresLogin() throws Exception {
        try (ActivityScenario<SubjectListActivity> screen = ActivityScenario.launch(SubjectListActivity.class)) {
            awaitDatabase();
            assertEquals(Lifecycle.State.DESTROYED, screen.getState());
        }
    }

    @Test public void studentCanOpenPickerAndAdminControlsStayHidden() throws Exception {
        login("sv01", "Student@123");
        try (ActivityScenario<SubjectPickerActivity> screen = ActivityScenario.launch(SubjectPickerActivity.class)) {
            awaitDatabase();
            screen.onActivity(activity -> {
                assertEquals(android.view.View.VISIBLE, activity.findViewById(R.id.common_root).getVisibility());
                assertEquals(android.view.View.GONE, activity.findViewById(R.id.subject_browser_add).getVisibility());
            });
        }
    }

    @Test public void adminEditorKeepsTypedValuesOnRecreation() throws Exception {
        login("admin", "Admin@123");
        try (ActivityScenario<SubjectFormActivity> screen = ActivityScenario.launch(SubjectFormActivity.class)) {
            awaitDatabase();
            screen.onActivity(activity -> {
                assertTrue(activity.findViewById(R.id.subject_form_save).isEnabled());
                ((EditText) activity.findViewById(R.id.subject_form_code)).setText("MY_CODE");
            });
            screen.recreate();
            awaitDatabase();
            screen.onActivity(activity -> assertEquals("MY_CODE",
                    ((EditText) activity.findViewById(R.id.subject_form_code)).getText().toString()));
        }
    }

    @Test public void monthNavigatesAcrossYearAndRestoresSelectionWithoutExposingData() throws Exception {
        login("sv01", "Student@123");
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, MonthCalendarActivity.class)
                .putExtra(AppConstants.EXTRA_SELECTED_DATE, "2026-12-31");
        try (ActivityScenario<MonthCalendarActivity> screen = ActivityScenario.launch(intent)) {
            awaitDatabase();
            screen.onActivity(activity -> {
                assertTrue(activity.findViewById(R.id.month_book).isEnabled());
                assertEquals(42, ((GridLayout) activity.findViewById(R.id.month_grid)).getChildCount());
                activity.findViewById(R.id.month_next).performClick();
            });
            awaitDatabase();
            screen.recreate();
            awaitDatabase();
            screen.onActivity(activity -> assertEquals(activity.getString(R.string.month_selected, "31/01/2027"),
                    ((TextView) activity.findViewById(R.id.month_selected)).getText().toString()));
        }
    }

    @Test public void monthIgnoresMalformedDateExtra() throws Exception {
        login("sv01", "Student@123");
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, MonthCalendarActivity.class)
                .putExtra(AppConstants.EXTRA_SELECTED_DATE, "invalid");
        try (ActivityScenario<MonthCalendarActivity> screen = ActivityScenario.launch(intent)) {
            awaitDatabase();
            screen.onActivity(activity -> assertTrue(activity.findViewById(R.id.month_open_day).isEnabled()));
        }
    }
}
