package com.example.studentmanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.AuthRepository;
import com.example.studentmanager.dal.DbContract;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.utils.DbExecutor;
import com.example.studentmanager.utils.SessionManager;

public class LoginActivity extends BaseActivity {
    private EditText username;
    private EditText password;
    private Button login;
    private TextView message;
    private boolean busy;

    @Override
    protected void onResume() {
        super.onResume();
        // Khoi phuc sau xoay man hinh khi tac vu login cu vua hoan tat.
        if (busy) return;
        DbExecutor.execute(() -> {
            try {
                Session session = new SessionManager(this).getSession();
                if (session != null) runOnUiThread(() -> openHome(session));
            } catch (RuntimeException error) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) message.setText(R.string.auth_session_error);
                });
            }
        });
    }

    private void openHome(Session session) {
        if (isFinishing() || isDestroyed()) return;
        Class<?> target = DbContract.ADMIN.equals(session.role) ? AdminHomeActivity.class : StudentHomeActivity.class;
        startActivity(new Intent(this, target)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_auth_login);
        username = findViewById(R.id.auth_username);
        password = findViewById(R.id.auth_password);
        login = findViewById(R.id.auth_submit);
        message = findViewById(R.id.auth_message);
        login.setOnClickListener(view -> submit());
        findViewById(R.id.auth_login_back).setOnClickListener(view -> finish());
    }

    private void submit() {
        if (busy) return;
        String name = username.getText().toString();
        String secret = password.getText().toString();
        username.setError(null);
        password.setError(null);
        if (name.trim().isEmpty()) {
            username.setError(getString(R.string.auth_username_required));
            username.requestFocus();
            return;
        }
        if (secret.trim().isEmpty()) {
            password.setError(getString(R.string.auth_password_required));
            password.requestFocus();
            return;
        }
        busy = true;
        login.setEnabled(false);
        username.setEnabled(false);
        password.setEnabled(false);
        message.setText(R.string.auth_loading);
        DbExecutor.execute(() -> {
            ActionResult result = new AuthRepository(this).login(name, secret);
            Session session;
            try {
                session = result.ok ? new SessionManager(this).getSession() : null;
            } catch (RuntimeException error) {
                session = null;
            }
            Session checked = session;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                busy = false;
                login.setEnabled(true);
                username.setEnabled(true);
                password.setEnabled(true);
                password.setText("");
                if (!result.ok || checked == null) {
                    message.setText(result.ok ? getString(R.string.auth_session_error) : result.message);
                    return;
                }
                openHome(checked);
            });
        });
    }
}
