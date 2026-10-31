package com.example.studentmanager.activities;

import static org.junit.Assert.*;
import android.graphics.Bitmap;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.SubjectDemoData;
import com.example.studentmanager.debug.SubjectVisualDemoActivity;
import com.example.studentmanager.models.Subject;
import com.example.studentmanager.utils.DbExecutor;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Chạy trên emulator: kiểm tra demo có dữ liệu và lưu ảnh để xem bố cục. */
@RunWith(AndroidJUnit4.class)
public class SubjectVisualDemoTest {
    private void awaitDatabase() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        DbExecutor.execute(done::countDown);
        assertTrue(done.await(5, TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @Before public void resetOnlyDemoDatabase() throws Exception {
        DbExecutor.execute(() -> SubjectDemoData.reset(ApplicationProvider.getApplicationContext()));
        awaitDatabase();
    }

    private void screenshot(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // Chờ animation chuyển Activity và frame mới trước khi chụp ảnh QA.
        android.os.SystemClock.sleep(1000);
        File directory = ApplicationProvider.getApplicationContext().getExternalFilesDir("visual-qa");
        assertNotNull(directory);
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        try (FileOutputStream output = new FileOutputStream(new File(directory, name + ".png"))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally { bitmap.recycle(); }
    }

    @Test public void populatedSubjectListAllowsAdminActions() throws Exception {
        try (ActivityScenario<SubjectVisualDemoActivity.Subjects> screen =
                     ActivityScenario.launch(SubjectVisualDemoActivity.Subjects.class)) {
            awaitDatabase();
            screen.onActivity(activity -> {
                assertEquals(30, ((ListView) activity.findViewById(R.id.subject_browser_list)).getCount());
                assertTrue(activity.findViewById(R.id.subject_browser_add).isEnabled());
            });
            screenshot("subjects");
        }
    }

    @Test public void editorShowsErrorsThenSavesToDemoDatabase() throws Exception {
        try (ActivityScenario<SubjectVisualDemoActivity.Editor> screen =
                     ActivityScenario.launch(SubjectVisualDemoActivity.Editor.class)) {
            awaitDatabase();
            screen.onActivity(activity -> {
                activity.findViewById(R.id.subject_form_save).performClick();
                assertNotNull(((EditText) activity.findViewById(R.id.subject_form_code)).getError());
                assertNotNull(((EditText) activity.findViewById(R.id.subject_form_credits)).getError());
            });
            screenshot("editor-errors");
            screen.onActivity(activity -> {
                ((EditText) activity.findViewById(R.id.subject_form_code)).setText("visual01");
                ((EditText) activity.findViewById(R.id.subject_form_name)).setText("Môn thử giao diện");
                ((EditText) activity.findViewById(R.id.subject_form_credits)).setText("3");
                activity.findViewById(R.id.subject_form_save).performClick();
            });
            awaitDatabase();
            AtomicReference<List<Subject>> saved = new AtomicReference<>();
            DbExecutor.execute(() -> saved.set(SubjectDemoData.subjects(
                    ApplicationProvider.getApplicationContext(), true).list("VISUAL01")));
            awaitDatabase();
            assertEquals(1, saved.get().size());
            assertEquals("Môn thử giao diện", saved.get().get(0).subjectName);
        }
    }

    @Test public void pickerReturnsSelectedIdAndNameToDemoMenu() throws Exception {
        try (ActivityScenario<SubjectVisualDemoActivity> menu =
                     ActivityScenario.launch(SubjectVisualDemoActivity.class)) {
            menu.onActivity(activity -> activity.findViewById(R.id.subject_demo_picker).performClick());
            awaitDatabase();
            screenshot("picker");
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                SubjectVisualDemoActivity.Picker picker = (SubjectVisualDemoActivity.Picker)
                        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                                .iterator().next();
                ListView list = picker.findViewById(R.id.subject_browser_list);
                assertEquals(30, list.getCount());
                list.performItemClick(list.getChildAt(0), 0, list.getAdapter().getItemId(0));
            });
            awaitDatabase();
            // Kết quả Activity Result tiếp tục xếp một lần đọc tên môn lên DbExecutor.
            awaitDatabase();
            menu.onActivity(activity -> assertTrue(
                    ((TextView) activity.findViewById(R.id.subject_demo_result))
                            .getText().toString().startsWith("Đã nhận subjectId = ")));
        }
    }

    @Test public void monthShowsTwoActiveSchedulesAndEnablesNavigation() throws Exception {
        try (ActivityScenario<SubjectVisualDemoActivity.Month> screen =
                     ActivityScenario.launch(SubjectVisualDemoActivity.Month.class)) {
            awaitDatabase();
            screen.onActivity(activity -> {
                assertEquals(2, ((LinearLayout) activity.findViewById(R.id.month_schedules)).getChildCount());
                assertTrue(activity.findViewById(R.id.month_open_day).isEnabled());
                assertTrue(activity.findViewById(R.id.month_book).isEnabled());
            });
            screenshot("month");
        }
    }
}
