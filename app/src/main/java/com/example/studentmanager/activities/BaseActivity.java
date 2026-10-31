package com.example.studentmanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.studentmanager.MainActivity;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.AuthRepository;
import com.example.studentmanager.dal.DbContract;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.notifications.NotificationHelper;
import com.example.studentmanager.utils.DbExecutor;
import com.example.studentmanager.utils.SessionManager;

public abstract class BaseActivity extends ComponentActivity {
    private int checkNumber;

    protected void setPage(int layout) {
        setContentView(layout);
        View root = findViewById(R.id.common_root);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
            ViewCompat.requestApplyInsets(root);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this instanceof MainActivity || this instanceof LoginActivity) return;
        // Cac trang quan ly thuoc Admin; cac trang con lai thuoc Student.
        String role = this instanceof AdminHomeActivity || this instanceof StudentListActivity
                || this instanceof StudentFormActivity || this instanceof StudentDetailActivity
                || this instanceof SubjectListActivity || this instanceof SubjectFormActivity
                ? DbContract.ADMIN : DbContract.STUDENT;
        View root = findViewById(R.id.common_root);
        if (root != null) root.setVisibility(View.INVISIBLE);
        int request = ++checkNumber;
        DbExecutor.execute(() -> {
            Session session = null;
            try {
                session = new SessionManager(this).getSession();
            } catch (RuntimeException error) {
                // Loi DB se ve trang login, khong de lo trang rieng.
            }
            Session checked = session;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || request != checkNumber) return;
                if (checked == null || !role.equals(checked.role)) {
                    NotificationHelper.refresh(this);
                    openLogin();
                    return;
                }
                if (root != null) root.setVisibility(View.VISIBLE);
                onSessionReady(checked);
            });
        });
    }

    @Override
    protected void onPause() {
        checkNumber++;
        super.onPause();
    }

    protected void onSessionReady(Session session) {}

    protected void logout() {
        View root = findViewById(R.id.common_root);
        if (root != null) root.setVisibility(View.INVISIBLE);
        DbExecutor.execute(() -> {
            new AuthRepository(this).logout();
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(this, R.string.auth_logged_out, Toast.LENGTH_SHORT).show();
                    openLogin();
                }
            });
        });
    }

    protected void openLogin() {
        startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
