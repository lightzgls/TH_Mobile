package com.example.studentmanager.activities;

import android.content.Intent;
import android.widget.Toast;
import com.example.studentmanager.R;
import com.example.studentmanager.models.Subject;
import com.example.studentmanager.utils.AppConstants;
import com.example.studentmanager.utils.DbExecutor;

/** Thu mở qua Activity Result API; mặc định hủy, chỉ trả ID còn hợp lệ. */
public class SubjectPickerActivity extends SubjectListActivity {
    private boolean selecting;
    @Override protected boolean isPicker() { return true; }
    @Override protected int pageLayout() { return R.layout.activity_subject_picker; }

    @Override protected void selectSubject(Subject selected) {
        if (selecting) return;
        selecting = true;
        DbExecutor.execute(() -> {
            try {
                boolean allowed = createAuthGuard().requireStudent() != null;
                Subject subject = allowed ? createRepository().get(selected.id) : null;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    selecting = false;
                    if (subject == null) {
                        Toast.makeText(this, R.string.subject_missing, Toast.LENGTH_LONG).show();
                        return;
                    }
                    setResult(RESULT_OK, new Intent().putExtra(AppConstants.EXTRA_SUBJECT_ID, subject.id));
                    finish();
                });
            } catch (RuntimeException exception) {
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    selecting = false;
                    Toast.makeText(this, R.string.subject_error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
