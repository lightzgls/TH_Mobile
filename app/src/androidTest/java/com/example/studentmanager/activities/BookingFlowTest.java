package com.example.studentmanager.activities;

import static org.junit.Assert.*;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.AuthRepository;
import com.example.studentmanager.dal.BookingRepository;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.dal.StudentRepository;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.BookingDraft;
import com.example.studentmanager.models.StudentDraft;
import com.example.studentmanager.utils.AppConstants;
import com.example.studentmanager.utils.AuthGuard;
import com.example.studentmanager.utils.DbExecutor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Luong that An -> Sang -> Thu. Chi chay tren emulator danh rieng cho test. */
@RunWith(AndroidJUnit4.class)
public class BookingFlowTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private Context context;
    private SQLiteDatabase db;
    private String date;
    private String marker;

    @Before public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        db = DBHelper.getInstance(context).getWritableDatabase();
        date = LocalDate.now().plusYears(1).toString();
        marker = "THU_FLOW_" + System.nanoTime();
        new AuthRepository(context).logout();
        AccessibilityServiceInfo info = instrumentation.getUiAutomation().getServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        instrumentation.getUiAutomation().setServiceInfo(info);
        awaitTasks();
    }

    @After public void tearDown() throws Exception {
        // Chi xoa lich do chinh test nay tao, khong reset database ung dung.
        db.delete("StudySchedule", "location IN (?,?)", new String[]{marker, marker + "_existing"});
        db.delete("Account", "username = ?", new String[]{marker.toLowerCase(Locale.ROOT)});
        new AuthRepository(context).logout();
        awaitTasks();
        instrumentation.runOnMainSync(() -> {
            for (Activity activity : new ArrayList<>(ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED))) activity.finish();
        });
    }

    private void awaitTasks() throws Exception {
        // Mot callback UI co the goi them truy van, nen cho ca hai hang doi hoan tat.
        for (int i = 0; i < 3; i++) {
            CountDownLatch done = new CountDownLatch(1);
            DbExecutor.execute(done::countDown);
            assertTrue("Database task timed out", done.await(15, TimeUnit.SECONDS));
            instrumentation.waitForIdleSync();
        }
    }

    private void awaitClosed(ActivityScenario<?> screen) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (screen.getState() != Lifecycle.State.DESTROYED && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(50);
        }
        assertEquals(Lifecycle.State.DESTROYED, screen.getState());
    }

    private <T extends Activity> T current(Class<T> type) {
        AtomicReference<T> found = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> {
            for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) {
                if (type.isInstance(activity)) found.set(type.cast(activity));
            }
        });
        assertNotNull("Expected resumed screen: " + type.getSimpleName(), found.get());
        return found.get();
    }

    private void chooseSubject(BookingFormActivity form) throws Exception {
        instrumentation.runOnMainSync(() -> form.findViewById(R.id.booking_form_subject).performClick());
        awaitTasks();
        SubjectPickerActivity picker = current(SubjectPickerActivity.class);
        instrumentation.runOnMainSync(() -> {
            ListView list = picker.findViewById(R.id.subject_browser_list);
            for (int i = 0; i < list.getCount(); i++) {
                if (list.getItemAtPosition(i).toString().contains("MOB101")) {
                    list.performItemClick(null, i, i);
                    return;
                }
            }
            fail("Seed subject MOB101 not found");
        });
        awaitTasks();
        instrumentation.runOnMainSync(() -> assertTrue(
                ((TextView) form.findViewById(R.id.booking_form_subject)).getText().toString().contains("MOB101")));
    }

    private AccessibilityNodeInfo node(String id) {
        UiAutomation automation = instrumentation.getUiAutomation();
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo root = automation.getRootInActiveWindow();
            if (root != null) {
                List<AccessibilityNodeInfo> result = root.findAccessibilityNodeInfosByViewId(id);
                if (!result.isEmpty()) return result.get(0);
            }
            SystemClock.sleep(50);
        }
        throw new AssertionError("Control not found: " + id);
    }

    private void chooseTime(BookingFormActivity form, int button, String hour, String minute) throws Exception {
        instrumentation.runOnMainSync(() -> form.findViewById(button).performClick());
        instrumentation.waitForIdleSync();
        assertTrue(node("android:id/toggle_mode").performAction(AccessibilityNodeInfo.ACTION_CLICK));
        setNodeText("android:id/input_hour", hour);
        setNodeText("android:id/input_minute", minute);
        assertTrue(node("android:id/button1").performAction(AccessibilityNodeInfo.ACTION_CLICK));
        awaitTasks();
    }

    private void setNodeText(String id, String value) {
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
        assertTrue(node(id).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments));
    }

    @Test public void loginPickSubjectRejectOverlapThenSaveAdjacentAndScheduleReminder() throws Exception {
        AuthRepository auth = new AuthRepository(context);
        assertTrue(auth.login("sv01", "Student@123").ok);
        BookingDraft existing = new BookingDraft();
        existing.subjectId = 1;
        existing.studyDate = date;
        existing.startTime = "09:00";
        existing.endTime = "10:00";
        existing.location = marker + "_existing";
        assertTrue(new BookingRepository(context).save(-1, existing).ok);
        auth.logout();
        awaitTasks();

        try (ActivityScenario<LoginActivity> screen = ActivityScenario.launch(LoginActivity.class)) {
            screen.onActivity(activity -> {
                ((EditText) activity.findViewById(R.id.auth_username)).setText("sv01");
                ((EditText) activity.findViewById(R.id.auth_password)).setText("Student@123");
                activity.findViewById(R.id.auth_submit).performClick();
            });
            awaitTasks();
            StudentHomeActivity home = current(StudentHomeActivity.class);
            instrumentation.runOnMainSync(() -> home.findViewById(R.id.student_open_booking).performClick());
            awaitTasks();
            BookingFormActivity initialForm = current(BookingFormActivity.class);
            // Ngay duoc truyen qua Intent trong test form ben duoi; luong Home mac dinh la hom nay.
            // Mo lai form voi ngay test de khong phu thuoc vao gio chay.
            instrumentation.runOnMainSync(() -> {
                initialForm.finish();
                home.startActivity(new Intent(home, BookingFormActivity.class)
                        .putExtra(AppConstants.EXTRA_SELECTED_DATE, date));
            });
            awaitTasks();
            BookingFormActivity form = current(BookingFormActivity.class);
            chooseSubject(form);
            chooseTime(form, R.id.booking_form_start, "9", "30");
            chooseTime(form, R.id.booking_form_end, "10", "30");
            BookingFormActivity editing = form;
            instrumentation.runOnMainSync(() -> {
                ((EditText) editing.findViewById(R.id.booking_form_location)).setText(marker);
                editing.findViewById(R.id.booking_form_save).performClick();
            });
            awaitTasks();
            instrumentation.runOnMainSync(() -> {
                assertTrue(((TextView) editing.findViewById(R.id.booking_form_error)).getText().toString().contains("trùng"));
                assertEquals(marker, ((EditText) editing.findViewById(R.id.booking_form_location)).getText().toString());
            });
            chooseTime(form, R.id.booking_form_start, "10", "00");
            chooseTime(form, R.id.booking_form_end, "11", "00");
            instrumentation.runOnMainSync(() -> editing.findViewById(R.id.booking_form_save).performClick());
            awaitTasks();
        }

        long savedId;
        try (Cursor cursor = db.rawQuery("SELECT id,status,startTime,endTime FROM StudySchedule WHERE location = ?",
                new String[]{marker})) {
            assertTrue(cursor.moveToFirst());
            savedId = cursor.getLong(0);
            assertEquals("PENDING", cursor.getString(1));
            assertEquals("10:00", cursor.getString(2));
            assertEquals("11:00", cursor.getString(3));
            assertFalse(cursor.moveToNext());
        }
        List<WorkInfo> work = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork("study_reminder_" + savedId).get(10, TimeUnit.SECONDS);
        boolean scheduled = false;
        for (WorkInfo item : work) {
            if (item.getState() == WorkInfo.State.ENQUEUED) scheduled = true;
        }
        assertTrue("Reminder must be scheduled after saving", scheduled);
    }

    @Test public void formKeepsInputsAndSelectedSubjectAfterRecreation() throws Exception {
        assertTrue(new AuthRepository(context).login("sv01", "Student@123").ok);
        Intent intent = new Intent(context, BookingFormActivity.class)
                .putExtra(AppConstants.EXTRA_SELECTED_DATE, date);
        try (ActivityScenario<BookingFormActivity> screen = ActivityScenario.launch(intent)) {
            awaitTasks();
            chooseSubject(current(BookingFormActivity.class));
            screen.onActivity(activity -> ((EditText) activity.findViewById(R.id.booking_form_location)).setText(marker));
            screen.recreate();
            awaitTasks();
            screen.onActivity(activity -> {
                assertEquals(marker, ((EditText) activity.findViewById(R.id.booking_form_location)).getText().toString());
                assertTrue(((TextView) activity.findViewById(R.id.booking_form_subject)).getText().toString().contains("MOB101"));
                assertTrue(activity.findViewById(R.id.booking_form_save).isEnabled());
            });
        }
    }

    @Test public void adminCannotOpenStudentBookingForm() throws Exception {
        assertTrue(new AuthRepository(context).login("admin", "Admin@123").ok);
        try (ActivityScenario<BookingFormActivity> screen = ActivityScenario.launch(BookingFormActivity.class)) {
            awaitTasks();
            awaitClosed(screen);
        }
    }

    @Test public void adminCreatesStudentThenNewStudentLogsInAndBooks() throws Exception {
        AuthRepository auth = new AuthRepository(context);
        assertTrue(auth.login("admin", "Admin@123").ok);
        String username = marker.toLowerCase(Locale.ROOT);
        try (ActivityScenario<StudentFormActivity> screen = ActivityScenario.launch(StudentFormActivity.class)) {
            awaitTasks();
            screen.onActivity(activity -> {
                ((EditText) activity.findViewById(R.id.student_form_code)).setText(marker);
                ((EditText) activity.findViewById(R.id.student_form_fullname)).setText("Sinh viên kiểm thử");
                ((EditText) activity.findViewById(R.id.student_form_email)).setText(username + "@example.com");
                ((EditText) activity.findViewById(R.id.student_form_phone)).setText("0900000003");
                ((EditText) activity.findViewById(R.id.student_form_username)).setText(username);
                ((EditText) activity.findViewById(R.id.student_form_password)).setText("Student@123");
                activity.findViewById(R.id.student_form_save).performClick();
            });
            awaitTasks();
            awaitClosed(screen);
        }
        auth.logout();
        assertTrue(auth.login(username, "Student@123").ok);
        long studentId = new AuthGuard(context).requireStudent().studentId;
        BookingDraft draft = new BookingDraft();
        draft.subjectId = 1;
        draft.studyDate = date;
        draft.startTime = "10:00";
        draft.endTime = "11:00";
        draft.location = marker;
        ActionResult saved = new BookingRepository(context).save(-1, draft);
        assertTrue(saved.message, saved.ok);
        try (Cursor cursor = db.rawQuery("SELECT studentId FROM StudySchedule WHERE id = ?",
                new String[]{String.valueOf(saved.id)})) {
            assertTrue(cursor.moveToFirst());
            assertEquals(studentId, cursor.getLong(0));
        }
        assertTrue(new StudentRepository(context).list("").isEmpty());
        auth.logout();
        assertTrue(auth.login("admin", "Admin@123").ok);
        assertTrue(new StudentRepository(context).delete(studentId).ok);
        try (Cursor cursor = db.rawQuery("SELECT id FROM StudySchedule WHERE id = ?",
                new String[]{String.valueOf(saved.id)})) {
            assertFalse("Deleting student must cascade to the booking", cursor.moveToFirst());
        }
    }

    @Test public void createdAccountsUseTheSameLengthLimitsAsLogin() {
        assertTrue(new AuthRepository(context).login("admin", "Admin@123").ok);
        StudentDraft draft = new StudentDraft();
        draft.studentCode = marker;
        draft.fullname = "Sinh viên kiểm thử";
        draft.email = "test@example.com";
        draft.phone = "0900000003";
        char[] longUsername = new char[101];
        Arrays.fill(longUsername, 'a');
        draft.username = new String(longUsername);
        draft.password = "Student@123";
        assertEquals(ActionResult.VALIDATION, new StudentRepository(context).create(draft).code);
        draft.username = marker.toLowerCase(Locale.ROOT);
        char[] longPassword = new char[257];
        Arrays.fill(longPassword, 'a');
        draft.password = new String(longPassword);
        assertEquals(ActionResult.VALIDATION, new StudentRepository(context).create(draft).code);
    }
}
