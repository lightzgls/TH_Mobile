package com.example.studentmanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import com.example.studentmanager.R;
import com.example.studentmanager.dal.StudentRepository;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.Student;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.utils.AppConstants;
import com.example.studentmanager.utils.AuthGuard;
import com.example.studentmanager.utils.DbExecutor;

import java.util.List;

/** Danh sách, tìm kiếm và điều hướng quản lý sinh viên dành cho Admin. */
public class StudentListActivity extends BaseActivity {
    private StudentRepository repository;
    private EditText searchInput;
    private LinearLayout studentContainer;
    private TextView statusView;
    private TextView emptyView;
    private Button addButton;
    private Button searchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_student_list);
        repository = new StudentRepository(this);
        searchInput = findViewById(R.id.student_list_search);
        studentContainer = findViewById(R.id.student_list_container);
        statusView = findViewById(R.id.student_list_status);
        emptyView = findViewById(R.id.student_list_empty);
        addButton = findViewById(R.id.student_list_add);
        searchButton = findViewById(R.id.student_list_search_button);

        addButton.setOnClickListener(view ->
                startActivity(new Intent(this, StudentFormActivity.class)));
        searchButton.setOnClickListener(view -> loadStudents(searchInput.getText().toString()));
        searchInput.setOnEditorActionListener((textView, actionId, event) -> {
            loadStudents(searchInput.getText().toString());
            return true;
        });
        findViewById(R.id.student_list_back).setOnClickListener(view -> finish());
    }

    @Override
    protected void onSessionReady(Session session) {
        loadStudents(searchInput == null ? "" : searchInput.getText().toString());
    }

    private void loadStudents(String keyword) {
        if (studentContainer == null) return;
        addButton.setEnabled(false);
        searchButton.setEnabled(false);
        statusView.setText(R.string.student_loading);
        DbExecutor.execute(() -> {
            boolean allowed = new AuthGuard(this).requireAdmin();
            List<Student> students = repository.list(keyword);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                studentContainer.removeAllViews();
                if (!allowed) {
                    emptyView.setVisibility(View.GONE);
                    statusView.setText(R.string.student_access_denied);
                    return;
                }
                addButton.setEnabled(true);
                searchButton.setEnabled(true);
                for (Student student : students) addStudentRow(student);
                boolean isEmpty = students.isEmpty();
                emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                statusView.setText(getString(R.string.student_result_count, students.size()));
            });
        });
    }

    private void addStudentRow(Student student) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackgroundColor(0xFFF2F5F9);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(10);
        studentContainer.addView(card, cardParams);

        TextView name = new TextView(this);
        name.setText(student.fullname);
        name.setTextSize(18);
        name.setTextColor(0xFF172B4D);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(name);

        TextView info = new TextView(this);
        info.setText(getString(R.string.student_row_info,
                student.studentCode, student.email, student.phone));
        info.setTextSize(14);
        info.setPadding(0, dp(4), 0, dp(8));
        card.addView(info);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        card.addView(actions);
        Button detail = actionButton(R.string.student_action_detail);
        Button edit = actionButton(R.string.student_action_edit);
        Button delete = actionButton(R.string.student_action_delete);
        actions.addView(detail, weightedParams());
        actions.addView(edit, weightedParams());
        actions.addView(delete, weightedParams());

        detail.setOnClickListener(view -> {
            Intent intent = new Intent(this, StudentDetailActivity.class);
            intent.putExtra(AppConstants.EXTRA_STUDENT_ID, student.id);
            startActivity(intent);
        });
        edit.setOnClickListener(view -> openEdit(student.id));
        delete.setOnClickListener(view -> confirmDelete(student));
    }

    private void openEdit(long studentId) {
        Intent intent = new Intent(this, StudentFormActivity.class);
        intent.putExtra(AppConstants.EXTRA_STUDENT_ID, studentId);
        startActivity(intent);
    }

    private void confirmDelete(Student student) {
        DbExecutor.execute(() -> {
            int scheduleCount = repository.countSchedules(student.id);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (scheduleCount < 0) {
                    Toast.makeText(this, R.string.student_cannot_count_schedules, Toast.LENGTH_LONG).show();
                    return;
                }
                new AlertDialog.Builder(this)
                        .setTitle(R.string.student_delete_title)
                        .setMessage(getString(R.string.student_delete_confirm,
                                student.fullname, scheduleCount))
                        .setNegativeButton(R.string.student_cancel, null)
                        .setPositiveButton(R.string.student_action_delete,
                                (dialog, which) -> deleteStudent(student.id))
                        .show();
            });
        });
    }

    private void deleteStudent(long studentId) {
        statusView.setText(R.string.student_deleting);
        DbExecutor.execute(() -> {
            ActionResult result = repository.delete(studentId);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.ok) {
                    Toast.makeText(this, R.string.student_delete_success, Toast.LENGTH_SHORT).show();
                    loadStudents(searchInput.getText().toString());
                } else {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
                    loadStudents(searchInput.getText().toString());
                }
            });
        });
    }

    private Button actionButton(int text) {
        Button button = new Button(this);
        button.setText(text);
        button.setMinWidth(0);
        button.setTextSize(12);
        return button;
    }

    private LinearLayout.LayoutParams weightedParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(2), 0, dp(2), 0);
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
