package com.example.studentmanager.debug;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.studentmanager.R;
import com.example.studentmanager.activities.*;
import com.example.studentmanager.dal.SubjectDemoData;
import com.example.studentmanager.dal.SubjectRepository;
import com.example.studentmanager.models.ScheduleFilter;
import com.example.studentmanager.models.ScheduleItem;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.models.Subject;
import com.example.studentmanager.utils.AppConstants;
import com.example.studentmanager.utils.AuthGuard;
import com.example.studentmanager.utils.DateTimeHelper;
import com.example.studentmanager.utils.DbExecutor;
import java.time.LocalDate;
import java.util.List;

/** Menu thử visual: dùng lại Activity thật, chỉ thay nguồn dữ liệu trong bản debug. */
public class SubjectVisualDemoActivity extends BaseActivity {
    private final ActivityResultLauncher<Intent> picker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                TextView output = findViewById(R.id.subject_demo_result);
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    output.setText(R.string.subject_demo_picker_cancelled);
                    return;
                }
                long id = result.getData().getLongExtra(AppConstants.EXTRA_SUBJECT_ID, -1);
                DbExecutor.execute(() -> {
                    Subject subject = SubjectDemoData.subjects(this, false).get(id);
                    runOnUiThread(() -> {
                        if (isDestroyed()) return;
                        output.setText(subject == null ? getString(R.string.subject_missing)
                                : getString(R.string.subject_demo_picked, subject.id,
                                        subject.subjectCode, subject.subjectName));
                    });
                });
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setPage(R.layout.activity_subject_visual_demo);
        findViewById(R.id.subject_demo_subjects).setOnClickListener(view ->
                startActivity(new Intent(this, Subjects.class)));
        findViewById(R.id.subject_demo_picker).setOnClickListener(view ->
                picker.launch(new Intent(this, Picker.class)));
        findViewById(R.id.subject_demo_month).setOnClickListener(view ->
                startActivity(new Intent(this, Month.class)));
        findViewById(R.id.subject_demo_reset).setOnClickListener(view ->
                new AlertDialog.Builder(this).setTitle(R.string.subject_demo_reset)
                        .setMessage(R.string.subject_demo_reset_confirm)
                        .setNegativeButton(R.string.subject_cancel, null)
                        .setPositiveButton(R.string.subject_demo_reset, (dialog, which) -> resetDemo()).show());
    }

    private void resetDemo() {
        findViewById(R.id.subject_demo_reset).setEnabled(false);
        DbExecutor.execute(() -> {
            int message;
            try { SubjectDemoData.reset(this); message = R.string.subject_demo_reset_done; }
            catch (RuntimeException exception) { message = R.string.subject_demo_reset_error; }
            final int result = message;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                findViewById(R.id.subject_demo_reset).setEnabled(true);
                ((TextView) findViewById(R.id.subject_demo_result)).setText(R.string.subject_demo_picker_hint);
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            });
        });
    }

    // Các lớp dưới chỉ có trong bản debug. Layout và xử lý nhập liệu dùng Activity thật.
    public static class Subjects extends SubjectListActivity {
        @Override protected SubjectRepository createRepository() { return SubjectDemoData.subjects(this, true); }
        @Override protected AuthGuard createAuthGuard() { return SubjectDemoData.guard(this, true); }
        @Override protected Class<?> formActivity() { return Editor.class; }
    }

    public static class Editor extends SubjectFormActivity {
        @Override protected SubjectRepository createRepository() { return SubjectDemoData.subjects(this, true); }
        @Override protected AuthGuard createAuthGuard() { return SubjectDemoData.guard(this, true); }
    }

    public static class Picker extends SubjectPickerActivity {
        @Override protected SubjectRepository createRepository() { return SubjectDemoData.subjects(this, false); }
        @Override protected AuthGuard createAuthGuard() { return SubjectDemoData.guard(this, false); }
    }

    public static class Month extends MonthCalendarActivity {
        @Override protected Session studentSession() { return SubjectDemoData.guard(this, false).requireStudent(); }
        @Override protected List<ScheduleItem> loadSchedules(ScheduleFilter filter) {
            return SubjectDemoData.schedules(this, filter);
        }
        @Override protected void openDay(Class<?> activity, LocalDate day) {
            String teammate = getString(activity == BookingFormActivity.class
                    ? R.string.subject_demo_thu : R.string.subject_demo_huy);
            new AlertDialog.Builder(this).setTitle(R.string.subject_demo_title)
                    .setMessage(getString(R.string.subject_demo_navigation,
                            day.format(DateTimeHelper.DB_DATE), teammate))
                    .setPositiveButton(R.string.subject_close, null).show();
        }
        @Override protected void openSchedule(ScheduleItem item) {
            new AlertDialog.Builder(this).setTitle(item.subjectName)
                    .setMessage(getString(R.string.month_schedule_row, item.subjectCode, item.subjectName,
                            item.startTime, item.endTime, item.location,
                            getString("PENDING".equals(item.status) ? R.string.month_pending : R.string.month_confirmed)))
                    .setPositiveButton(R.string.subject_close, null).show();
        }
    }
}
