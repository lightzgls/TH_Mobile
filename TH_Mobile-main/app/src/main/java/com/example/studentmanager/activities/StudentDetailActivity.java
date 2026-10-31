package com.example.studentmanager.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.example.studentmanager.R;
import com.example.studentmanager.dal.StudentRepository;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.Student;
import com.example.studentmanager.utils.AppConstants;
import com.example.studentmanager.utils.AuthGuard;
import com.example.studentmanager.utils.DbExecutor;

/** Màn hình thông tin chi tiết và thao tác sửa/xóa sinh viên. */
public class StudentDetailActivity extends BaseActivity {
    private StudentRepository repository;
    private long studentId;
    private TextView codeView;
    private TextView fullnameView;
    private TextView emailView;
    private TextView phoneView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_student_detail);
        repository = new StudentRepository(this);
        studentId = getIntent().getLongExtra(AppConstants.EXTRA_STUDENT_ID, -1);

        codeView = findViewById(R.id.student_detail_code);
        fullnameView = findViewById(R.id.student_detail_fullname);
        emailView = findViewById(R.id.student_detail_email);
        phoneView = findViewById(R.id.student_detail_phone);

        findViewById(R.id.student_detail_back).setOnClickListener(view -> finish());
        findViewById(R.id.student_detail_edit).setOnClickListener(view -> openEdit());
        findViewById(R.id.student_detail_delete).setOnClickListener(view -> confirmDelete());

        if (!new AuthGuard(this).requireAdmin()) {
            Toast.makeText(this, R.string.student_access_denied, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (studentId <= 0) {
            Toast.makeText(this, R.string.student_not_found, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repository != null && studentId > 0 && new AuthGuard(this).requireAdmin()) {
            loadStudent();
        }
    }

    private void loadStudent() {
        DbExecutor.execute(() -> {
            Student student = repository.get(studentId);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (student == null) {
                    Toast.makeText(this, R.string.student_not_found, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                codeView.setText(student.studentCode);
                fullnameView.setText(student.fullname);
                emailView.setText(student.email);
                phoneView.setText(student.phone);
            });
        });
    }

    private void openEdit() {
        Intent intent = new Intent(this, StudentFormActivity.class);
        intent.putExtra(AppConstants.EXTRA_STUDENT_ID, studentId);
        startActivity(intent);
    }

    private void confirmDelete() {
        DbExecutor.execute(() -> {
            int scheduleCount = repository.countSchedules(studentId);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (scheduleCount < 0) {
                    Toast.makeText(this, R.string.student_cannot_count_schedules, Toast.LENGTH_LONG).show();
                    return;
                }
                new AlertDialog.Builder(this)
                        .setTitle(R.string.student_delete_title)
                        .setMessage(getString(R.string.student_delete_confirm,
                                fullnameView.getText().toString(), scheduleCount))
                        .setNegativeButton(R.string.student_cancel, null)
                        .setPositiveButton(R.string.student_action_delete,
                                (dialog, which) -> deleteStudent())
                        .show();
            });
        });
    }

    private void deleteStudent() {
        findViewById(R.id.student_detail_delete).setEnabled(false);
        DbExecutor.execute(() -> {
            ActionResult result = repository.delete(studentId);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.ok) {
                    Toast.makeText(this, R.string.student_delete_success, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    findViewById(R.id.student_detail_delete).setEnabled(true);
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
