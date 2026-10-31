package com.example.studentmanager.activities;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.studentmanager.R;
import com.example.studentmanager.dal.StudentRepository;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.Student;
import com.example.studentmanager.models.StudentDraft;
import com.example.studentmanager.utils.AppConstants;
import com.example.studentmanager.utils.AuthGuard;
import com.example.studentmanager.utils.DbExecutor;

/** Form thêm mới hoặc cập nhật hồ sơ sinh viên. */
public class StudentFormActivity extends BaseActivity {
    private StudentRepository repository;
    private EditText codeInput;
    private EditText fullnameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private EditText usernameInput;
    private EditText passwordInput;
    private Button saveButton;
    private TextView titleView;
    private long studentId = -1;
    private boolean isEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_student_form);
        repository = new StudentRepository(this);
        studentId = getIntent().getLongExtra(AppConstants.EXTRA_STUDENT_ID, -1);
        isEdit = studentId > 0;

        titleView = findViewById(R.id.student_form_title);
        codeInput = findViewById(R.id.student_form_code);
        fullnameInput = findViewById(R.id.student_form_fullname);
        emailInput = findViewById(R.id.student_form_email);
        phoneInput = findViewById(R.id.student_form_phone);
        usernameInput = findViewById(R.id.student_form_username);
        passwordInput = findViewById(R.id.student_form_password);
        saveButton = findViewById(R.id.student_form_save);

        findViewById(R.id.student_form_back).setOnClickListener(view -> finish());
        saveButton.setOnClickListener(view -> saveStudent());

        if (!new AuthGuard(this).requireAdmin()) {
            Toast.makeText(this, R.string.student_access_denied, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (isEdit) {
            titleView.setText(R.string.student_form_edit_title);
            findViewById(R.id.student_form_account_group).setVisibility(View.GONE);
            loadStudent();
        } else {
            titleView.setText(R.string.student_form_add_title);
        }
    }

    private void loadStudent() {
        setFormEnabled(false);
        DbExecutor.execute(() -> {
            Student student = repository.get(studentId);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (student == null) {
                    Toast.makeText(this, R.string.student_not_found, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                codeInput.setText(student.studentCode);
                fullnameInput.setText(student.fullname);
                emailInput.setText(student.email);
                phoneInput.setText(student.phone);
                setFormEnabled(true);
            });
        });
    }

    private void saveStudent() {
        clearErrors();
        String code = value(codeInput);
        String fullname = value(fullnameInput);
        String email = value(emailInput);
        String phone = value(phoneInput);
        String username = value(usernameInput);
        String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString();

        boolean valid = true;
        if (code.isEmpty()) {
            codeInput.setError(getString(R.string.student_error_code));
            valid = false;
        }
        if (fullname.isEmpty()) {
            fullnameInput.setError(getString(R.string.student_error_fullname));
            valid = false;
        }
        if (email.isEmpty()) {
            emailInput.setError(getString(R.string.student_error_email));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError(getString(R.string.student_error_email_invalid));
            valid = false;
        }
        if (phone.isEmpty()) {
            phoneInput.setError(getString(R.string.student_error_phone));
            valid = false;
        }
        if (!isEdit && username.isEmpty()) {
            usernameInput.setError(getString(R.string.student_error_username));
            valid = false;
        }
        if (!isEdit && password.trim().isEmpty()) {
            passwordInput.setError(getString(R.string.student_error_password));
            valid = false;
        }
        if (!valid) return;

        StudentDraft draft = new StudentDraft();
        draft.studentCode = code;
        draft.fullname = fullname;
        draft.email = email;
        draft.phone = phone;
        draft.username = username;
        draft.password = password;

        setFormEnabled(false);
        DbExecutor.execute(() -> {
            ActionResult result = isEdit
                    ? repository.update(studentId, draft)
                    : repository.create(draft);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                setFormEnabled(true);
                if (result.ok) {
                    Toast.makeText(this,
                            isEdit ? R.string.student_update_success : R.string.student_create_success,
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
                    if (result.code.equals(ActionResult.DUPLICATE)) {
                        codeInput.setError(getString(R.string.student_error_duplicate_code_or_username));
                    }
                }
            });
        });
    }

    private void clearErrors() {
        codeInput.setError(null);
        fullnameInput.setError(null);
        emailInput.setError(null);
        phoneInput.setError(null);
        usernameInput.setError(null);
        passwordInput.setError(null);
    }

    private void setFormEnabled(boolean enabled) {
        codeInput.setEnabled(enabled);
        fullnameInput.setEnabled(enabled);
        emailInput.setEnabled(enabled);
        phoneInput.setEnabled(enabled);
        usernameInput.setEnabled(enabled && !isEdit);
        passwordInput.setEnabled(enabled && !isEdit);
        saveButton.setEnabled(enabled);
    }

    private String value(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}
