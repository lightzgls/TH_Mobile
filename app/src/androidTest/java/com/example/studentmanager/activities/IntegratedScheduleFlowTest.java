package com.example.studentmanager.activities;

import static org.junit.Assert.*;
import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Instrumentation;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.*;
import com.example.studentmanager.models.*;
import com.example.studentmanager.utils.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Chay tren emulator kiem thu rieng. Chi don du lieu do test tu tao. */
@RunWith(AndroidJUnit4.class)
public class IntegratedScheduleFlowTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private Context context;
    private AuthRepository auth;
    private long studentId = -1;
    private String marker;
    private String date;

    @Before public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        auth = new AuthRepository(context);
        marker = "INT_" + System.nanoTime();
        date = LocalDate.now().plusYears(1).toString();
        if (Build.VERSION.SDK_INT >= 33) instrumentation.getUiAutomation()
                .grantRuntimePermission(context.getPackageName(), Manifest.permission.POST_NOTIFICATIONS);
        AccessibilityServiceInfo info = instrumentation.getUiAutomation().getServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        instrumentation.getUiAutomation().setServiceInfo(info);
        assertTrue(auth.login("admin", "Admin@123").ok);
        StudentDraft student = new StudentDraft();
        student.studentCode = marker;
        student.fullname = "Sinh viên tích hợp";
        student.email = marker.toLowerCase(java.util.Locale.ROOT) + "@example.com";
        student.phone = "0900000099";
        student.username = marker.toLowerCase(java.util.Locale.ROOT);
        student.password = "Student@123";
        ActionResult created = new StudentRepository(context).create(student);
        assertTrue(created.message, created.ok);
        studentId = created.id;
        auth.logout();
        assertTrue(auth.login(student.username, student.password).ok);
        awaitTasks();
    }

    @After public void tearDown() throws Exception {
        instrumentation.runOnMainSync(() -> {
            for (Activity activity : new ArrayList<>(ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED))) activity.finish();
        });
        auth.logout();
        if (studentId > 0) {
            assertTrue(auth.login("admin", "Admin@123").ok);
            assertTrue(new StudentRepository(context).delete(studentId).ok);
            auth.logout();
        }
        awaitTasks();
    }

    private void awaitTasks() throws Exception {
        for (int i = 0; i < 3; i++) {
            CountDownLatch done = new CountDownLatch(1);
            DbExecutor.execute(done::countDown);
            assertTrue(done.await(30, TimeUnit.SECONDS));
            instrumentation.waitForIdleSync();
        }
    }

    private <T extends Activity> T current(Class<T> type) {
        AtomicReference<T> found = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> {
            for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)) {
                if (type.isInstance(activity)) found.set(type.cast(activity));
            }
        });
        assertNotNull("Expected " + type.getSimpleName(), found.get());
        return found.get();
    }

    private long book(String day, String start, String end) {
        BookingDraft draft = new BookingDraft();
        draft.subjectId = 1;
        draft.studyDate = day;
        draft.startTime = start;
        draft.endTime = end;
        draft.location = marker;
        ActionResult saved = new BookingRepository(context).save(-1, draft);
        assertTrue(saved.message, saved.ok);
        return saved.id;
    }

    private boolean contains(View view, String text) {
        if (view instanceof TextView && ((TextView) view).getText().toString().contains(text)) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (contains(group.getChildAt(i), text)) return true;
            }
        }
        return false;
    }

    private void acceptDialog() {
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo root = instrumentation.getUiAutomation().getRootInActiveWindow();
            if (root != null) {
                for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByViewId("android:id/button1")) {
                    if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return;
                }
            }
            SystemClock.sleep(50);
        }
        fail("Confirmation dialog not found");
    }

    @Test public void listDetailEditConfirmCalendarsCancelAndDeleteWorkTogether() throws Exception {
        long id = book(date, "10:00", "11:00");
        awaitTasks();
        try (ActivityScenario<ScheduleListActivity> screen = ActivityScenario.launch(ScheduleListActivity.class)) {
            awaitTasks();
            screen.onActivity(activity -> {
                ListView list = activity.findViewById(R.id.schedule_list_items);
                assertEquals(1, list.getCount());
                list.performItemClick(null, 0, id);
            });
            awaitTasks();
            ScheduleDetailActivity detail = current(ScheduleDetailActivity.class);
            instrumentation.runOnMainSync(() -> {
                assertTrue(contains(detail.findViewById(R.id.schedule_detail_location), marker));
                detail.findViewById(R.id.schedule_detail_edit_btn).performClick();
            });
            awaitTasks();
            BookingFormActivity form = current(BookingFormActivity.class);
            instrumentation.runOnMainSync(() -> {
                ((EditText) form.findViewById(R.id.booking_form_location)).setText(marker + "_edited");
                form.findViewById(R.id.booking_form_save).performClick();
            });
            awaitTasks();
            ScheduleDetailActivity edited = current(ScheduleDetailActivity.class);
            instrumentation.runOnMainSync(() -> {
                assertTrue(contains(edited.findViewById(R.id.schedule_detail_location), marker + "_edited"));
                edited.findViewById(R.id.schedule_detail_confirm_btn).performClick();
            });
            awaitTasks();
            assertEquals(DbContract.CONFIRMED, new ScheduleRepository(context).getMine(id).status);
        }
        Intent agenda = new Intent(context, DayWeekCalendarActivity.class)
                .putExtra(AppConstants.EXTRA_SELECTED_DATE, date);
        try (ActivityScenario<DayWeekCalendarActivity> screen = ActivityScenario.launch(agenda)) {
            awaitTasks();
            screen.onActivity(activity -> {
                assertTrue(contains(activity.findViewById(R.id.agenda_rows), marker + "_edited"));
                activity.findViewById(R.id.agenda_week).performClick();
            });
            awaitTasks();
            screen.recreate();
            awaitTasks();
            screen.onActivity(activity ->
                    assertTrue(contains(activity.findViewById(R.id.agenda_rows), marker + "_edited")));
        }
        Intent month = new Intent(context, MonthCalendarActivity.class)
                .putExtra(AppConstants.EXTRA_SELECTED_DATE, date);
        try (ActivityScenario<MonthCalendarActivity> screen = ActivityScenario.launch(month)) {
            awaitTasks();
            screen.onActivity(activity ->
                    assertTrue(contains(activity.findViewById(R.id.month_schedules), marker + "_edited")));
        }
        Intent detailIntent = new Intent(context, ScheduleDetailActivity.class)
                .putExtra(AppConstants.EXTRA_SCHEDULE_ID, id);
        try (ActivityScenario<ScheduleDetailActivity> screen = ActivityScenario.launch(detailIntent)) {
            awaitTasks();
            screen.onActivity(activity -> activity.findViewById(R.id.schedule_detail_cancel_btn).performClick());
            acceptDialog();
            awaitTasks();
            assertEquals(DbContract.CANCELLED, new ScheduleRepository(context).getMine(id).status);
            screen.onActivity(activity -> {
                assertEquals(View.GONE, activity.findViewById(R.id.schedule_detail_edit_btn).getVisibility());
                assertEquals(View.GONE, activity.findViewById(R.id.schedule_detail_confirm_btn).getVisibility());
                activity.findViewById(R.id.schedule_detail_delete_btn).performClick();
            });
            acceptDialog();
            awaitTasks();
            assertNull(new ScheduleRepository(context).getMine(id));
        }
    }

    @Test public void detailCannotDisplayAnotherStudentsSchedule() throws Exception {
        long otherId;
        try (Cursor cursor = DBHelper.getInstance(context).getReadableDatabase().rawQuery(
                "SELECT id FROM StudySchedule WHERE studentId=2 LIMIT 1", null)) {
            assertTrue(cursor.moveToFirst());
            otherId = cursor.getLong(0);
        }
        Intent intent = new Intent(context, ScheduleDetailActivity.class)
                .putExtra(AppConstants.EXTRA_SCHEDULE_ID, otherId);
        try (ActivityScenario<ScheduleDetailActivity> screen = ActivityScenario.launch(intent)) {
            awaitTasks();
            screen.onActivity(activity -> {
                assertEquals(context.getString(R.string.schedule_detail_not_found),
                        ((TextView) activity.findViewById(R.id.schedule_detail_message)).getText().toString());
                assertFalse(activity.findViewById(R.id.schedule_detail_delete_btn).isShown());
                assertEquals("", ((TextView) activity.findViewById(R.id.schedule_detail_location)).getText().toString());
            });
        }
    }

    @Test public void upcomingReminderAppearsAndCancelRemovesItAndShowsChange() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusMinutes(10).withSecond(0).withNano(0);
        if (!start.toLocalDate().equals(start.plusMinutes(1).toLocalDate())) start = start.plusMinutes(1);
        long id = book(start.toLocalDate().toString(),
                start.toLocalTime().format(DateTimeHelper.TIME),
                start.plusMinutes(1).toLocalTime().format(DateTimeHelper.TIME));
        awaitTasks();
        long deadline = SystemClock.uptimeMillis() + 45000;
        while (!hasNotification("reminder_" + id) && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(250);
        }
        assertTrue("Upcoming reminder must appear", hasNotification("reminder_" + id));
        assertTrue(new ScheduleRepository(context).setStatus(id, DbContract.CANCELLED).ok);
        awaitTasks();
        assertFalse(hasNotification("reminder_" + id));
        assertTrue("Schedule change notification must appear", hasNotification("change_" + id));
        auth.logout();
        awaitTasks();
        assertEquals(0, context.getSystemService(NotificationManager.class).getActiveNotifications().length);
    }

    private boolean hasNotification(String tag) {
        for (android.service.notification.StatusBarNotification item :
                context.getSystemService(NotificationManager.class).getActiveNotifications()) {
            if (tag.equals(item.getTag())) return true;
        }
        return false;
    }
}

