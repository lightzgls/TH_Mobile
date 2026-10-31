package com.example.studentmanager.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.SubjectRepository;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.Subject;
import com.example.studentmanager.models.SubjectDraft;
import com.example.studentmanager.utils.AppConstants;
import com.example.studentmanager.utils.AuthGuard;
import com.example.studentmanager.utils.DbExecutor;

public class SubjectFormActivity extends BaseActivity {
    private SubjectRepository repository;
    private EditText code, name, credits, description;
    private TextView message;
    private Button save;
    private long subjectId;
    private boolean populated, saving;
    private int request;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setPage(R.layout.activity_subject_form);
        repository = new SubjectRepository(this);
        subjectId = getIntent().getLongExtra(AppConstants.EXTRA_SUBJECT_ID, -1);
        populated = state != null && state.getBoolean("subject_populated");
        code = findViewById(R.id.subject_form_code);
        name = findViewById(R.id.subject_form_name);
        credits = findViewById(R.id.subject_form_credits);
        description = findViewById(R.id.subject_form_description);
        message = findViewById(R.id.subject_form_message);
        save = findViewById(R.id.subject_form_save);
        save.setOnClickListener(view -> saveSubject());
        findViewById(R.id.subject_form_back).setOnClickListener(view -> finish());
    }

    @Override protected void onResume() {
        super.onResume();
        int ticket = ++request;
        save.setEnabled(false);
        DbExecutor.execute(() -> {
            try {
                boolean allowed = new AuthGuard(this).requireAdmin();
                Subject subject = allowed && subjectId > 0 ? repository.get(subjectId) : null;
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing() || request != ticket) return;
                    if (!allowed) { message.setText(R.string.subject_admin_required); return; }
                    if (subjectId != -1 && subject == null) { message.setText(R.string.subject_missing); return; }
                    if (!populated && subject != null) {
                        code.setText(subject.subjectCode);
                        name.setText(subject.subjectName);
                        credits.setText(Integer.toString(subject.credits));
                        description.setText(subject.description);
                    }
                    populated = true;
                    save.setEnabled(!saving);
                });
            } catch (RuntimeException exception) {
                runOnUiThread(() -> {
                    if (!isDestroyed() && request == ticket) message.setText(R.string.subject_error);
                });
            }
        });
    }

    @Override protected void onPause() { super.onPause(); request++; save.setEnabled(false); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("subject_populated", populated);
        super.onSaveInstanceState(state);
    }

    private void saveSubject() {
        if (saving) return;
        SubjectDraft draft = new SubjectDraft();
        draft.subjectCode = code.getText().toString().trim();
        draft.subjectName = name.getText().toString().trim();
        draft.description = description.getText().toString().trim();
        boolean valid = true;
        code.setError(null); name.setError(null); credits.setError(null);
        if (draft.subjectCode.isEmpty()) { code.setError(getString(R.string.subject_required)); valid = false; }
        if (draft.subjectName.isEmpty()) { name.setError(getString(R.string.subject_required)); valid = false; }
        try {
            draft.credits = Integer.parseInt(credits.getText().toString().trim());
            if (draft.credits <= 0) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            credits.setError(getString(R.string.subject_invalid_credits)); valid = false;
        }
        if (!valid) return;
        saving = true;
        save.setEnabled(false);
        message.setText(R.string.subject_saving);
        DbExecutor.execute(() -> {
            ActionResult result = subjectId == -1 ? repository.create(draft) : repository.update(subjectId, draft);
            runOnUiThread(() -> {
                if (isDestroyed() || isFinishing()) return;
                saving = false;
                if (result.ok) {
                    Toast.makeText(this, R.string.subject_saved, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    message.setText(result.message);
                    save.setEnabled(!ActionResult.FORBIDDEN.equals(result.code)
                            && !ActionResult.NOT_FOUND.equals(result.code));
                }
            });
        });
    }
}
