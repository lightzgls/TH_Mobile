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
import com.example.studentmanager.R;
import com.example.studentmanager.utils.AppConstants;
import com.example.studentmanager.utils.DbExecutor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Chưa có phiên: các màn hình của Sáng phải đóng quyền và vẫn chạy bình thường. */
@RunWith(AndroidJUnit4.class)
public class SubjectScreenTest {
    private void awaitDatabase() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        DbExecutor.execute(done::countDown);
        assertTrue("Database task timed out", done.await(5, TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @Test public void adminListRequiresLogin() throws Exception {
        try (ActivityScenario<SubjectListActivity> screen = ActivityScenario.launch(SubjectListActivity.class)) {
            awaitDatabase();
            screen.onActivity(activity -> {
                assertEquals(activity.getString(R.string.subject_admin_required),
                        ((TextView) activity.findViewById(R.id.subject_browser_message)).getText().toString());
                assertFalse(activity.findViewById(R.id.subject_browser_add).isEnabled());
            });
        }
    }

    @Test public void pickerRequiresStudentAndHidesAdminControls() throws Exception {
        try (ActivityScenario<SubjectPickerActivity> screen = ActivityScenario.launch(SubjectPickerActivity.class)) {
            awaitDatabase();
            screen.onActivity(activity -> {
                assertEquals(activity.getString(R.string.subject_student_required),
                        ((TextView) activity.findViewById(R.id.subject_browser_message)).getText().toString());
                assertEquals(android.view.View.GONE, activity.findViewById(R.id.subject_browser_add).getVisibility());
            });
        }
    }

    @Test public void editorRequiresAdminAndKeepsTypedValuesOnRecreation() throws Exception {
        try (ActivityScenario<SubjectFormActivity> screen = ActivityScenario.launch(SubjectFormActivity.class)) {
            awaitDatabase();
            screen.onActivity(activity -> {
                assertFalse(activity.findViewById(R.id.subject_form_save).isEnabled());
                ((EditText) activity.findViewById(R.id.subject_form_code)).setText("MY_CODE");
            });
            screen.recreate();
            awaitDatabase();
            screen.onActivity(activity -> assertEquals("MY_CODE",
                    ((EditText) activity.findViewById(R.id.subject_form_code)).getText().toString()));
        }
    }

    @Test public void monthNavigatesAcrossYearAndRestoresSelectionWithoutExposingData() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, MonthCalendarActivity.class)
                .putExtra(AppConstants.EXTRA_SELECTED_DATE, "2026-12-31");
        try (ActivityScenario<MonthCalendarActivity> screen = ActivityScenario.launch(intent)) {
            awaitDatabase();
            screen.onActivity(activity -> {
                assertEquals(activity.getString(R.string.month_student_required),
                        ((TextView) activity.findViewById(R.id.month_message)).getText().toString());
                assertFalse(activity.findViewById(R.id.month_book).isEnabled());
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
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, MonthCalendarActivity.class)
                .putExtra(AppConstants.EXTRA_SELECTED_DATE, "invalid");
        try (ActivityScenario<MonthCalendarActivity> screen = ActivityScenario.launch(intent)) {
            awaitDatabase();
            screen.onActivity(activity -> assertFalse(activity.findViewById(R.id.month_open_day).isEnabled()));
        }
    }
}
