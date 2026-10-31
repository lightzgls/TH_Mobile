package com.example.studentmanager.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.SubjectRepository;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.Subject;
import com.example.studentmanager.utils.AppConstants;
import com.example.studentmanager.utils.AuthGuard;
import com.example.studentmanager.utils.DbExecutor;
import java.util.ArrayList;
import java.util.List;

/** Sáng: danh sách Admin; màn hình chọn môn dùng lại việc tải và tìm kiếm. */
public class SubjectListActivity extends BaseActivity {
    private SubjectRepository repository;
    private EditText search;
    private TextView message;
    private ArrayAdapter<String> adapter;
    private final List<Subject> subjects = new ArrayList<>();
    private int request;
    protected boolean isPicker() { return false; }
    protected int pageLayout() { return R.layout.activity_subject_list; }

    // Bản debug có thể dùng database riêng để thử đúng các màn hình này.
    protected SubjectRepository createRepository() { return new SubjectRepository(this); }
    protected AuthGuard createAuthGuard() { return new AuthGuard(this); }
    protected Class<?> formActivity() { return SubjectFormActivity.class; }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setPage(pageLayout());
        repository = createRepository();
        search = findViewById(R.id.subject_browser_search);
        message = findViewById(R.id.subject_browser_message);
        ((TextView) findViewById(R.id.subject_browser_title)).setText(
                isPicker() ? R.string.subject_picker_title : R.string.subject_list_title);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        ListView list = findViewById(R.id.subject_browser_list);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            if (position < subjects.size()) selectSubject(subjects.get(position));
        });
        findViewById(R.id.subject_browser_add).setVisibility(isPicker() ? View.GONE : View.VISIBLE);
        findViewById(R.id.subject_browser_add).setOnClickListener(view ->
                startActivity(new Intent(this, formActivity())));
        findViewById(R.id.subject_browser_find).setOnClickListener(view -> load());
        search.setOnEditorActionListener((view, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) { load(); return true; }
            return false;
        });
        findViewById(R.id.subject_browser_back).setOnClickListener(view -> finish());
    }

    @Override protected void onResume() { super.onResume(); load(); }
    @Override protected void onPause() {
        super.onPause();
        request++;
        subjects.clear();
        adapter.clear();
        findViewById(R.id.subject_browser_add).setEnabled(false);
    }

    private void load() {
        int ticket = ++request;
        String keyword = search.getText().toString();
        subjects.clear();
        adapter.clear();
        message.setText(R.string.subject_loading);
        findViewById(R.id.subject_browser_add).setEnabled(false);
        DbExecutor.execute(() -> {
            try {
                AuthGuard guard = createAuthGuard();
                boolean allowed = isPicker() ? guard.requireStudent() != null : guard.requireAdmin();
                List<Subject> result = allowed ? repository.list(keyword) : new ArrayList<>();
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || ticket != request) return;
                    findViewById(R.id.subject_browser_add).setEnabled(allowed && !isPicker());
                    subjects.addAll(result);
                    for (Subject subject : result) adapter.add(getString(R.string.subject_row,
                            subject.subjectCode, subject.subjectName, subject.credits));
                    if (allowed && !result.isEmpty()) message.setText("");
                    else message.setText(!allowed ? (isPicker() ? R.string.subject_student_required
                            : R.string.subject_admin_required) : R.string.subject_empty);
                });
            } catch (RuntimeException exception) {
                runOnUiThread(() -> {
                    if (!isDestroyed() && ticket == request) message.setText(R.string.subject_error);
                });
            }
        });
    }

    protected void selectSubject(Subject selected) {
        DbExecutor.execute(() -> {
            try {
                boolean allowed = createAuthGuard().requireAdmin();
                Subject subject = allowed ? repository.get(selected.id) : null;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (subject == null) { message.setText(R.string.subject_missing); load(); return; }
                    new AlertDialog.Builder(this).setTitle(subject.subjectName)
                            .setMessage(getString(R.string.subject_detail, subject.subjectCode,
                                    subject.subjectName, subject.credits, subject.description))
                            .setPositiveButton(R.string.subject_edit, (dialog, which) ->
                                    startActivity(new Intent(this, formActivity())
                                            .putExtra(AppConstants.EXTRA_SUBJECT_ID, subject.id)))
                            .setNeutralButton(R.string.subject_delete, (dialog, which) -> confirmDelete(subject))
                            .setNegativeButton(R.string.subject_close, null).show();
                });
            } catch (RuntimeException exception) {
                runOnUiThread(() -> { if (!isDestroyed()) message.setText(R.string.subject_error); });
            }
        });
    }

    private void confirmDelete(Subject subject) {
        new AlertDialog.Builder(this).setTitle(R.string.subject_delete)
                .setMessage(getString(R.string.subject_delete_confirm, subject.subjectCode))
                .setNegativeButton(R.string.subject_cancel, null)
                .setPositiveButton(R.string.subject_delete, (dialog, which) -> DbExecutor.execute(() -> {
                    ActionResult result = repository.delete(subject.id);
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(this, result.ok ? getString(R.string.subject_deleted)
                                : result.message, Toast.LENGTH_LONG).show();
                        load();
                    });
                })).show();
    }
}
