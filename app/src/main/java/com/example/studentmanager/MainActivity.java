package com.example.studentmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.example.studentmanager.activities.*;
import com.example.studentmanager.dal.DbContract;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.notifications.NotificationHelper;
import com.example.studentmanager.utils.DbExecutor;
import com.example.studentmanager.utils.SessionManager;

/** Diem vao: khoi tao DB va khoi phuc phien tren luong nen. */
public class MainActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_main);
        findViewById(R.id.main_open_login).setOnClickListener(view -> restoreSession());
        restoreSession();
    }

    private void restoreSession() {
        findViewById(R.id.main_open_login).setEnabled(false);
        DbExecutor.execute(() -> {
            try {
                Session session = new SessionManager(this).getSession();
                NotificationHelper.refresh(this);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Class<?> target = session == null ? LoginActivity.class
                            : DbContract.ADMIN.equals(session.role) ? AdminHomeActivity.class : StudentHomeActivity.class;
                    startActivity(new Intent(this, target)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                });
            } catch (RuntimeException error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    ((TextView) findViewById(R.id.main_status)).setText(R.string.main_database_error);
                    findViewById(R.id.main_open_login).setEnabled(true);
                });
            }
        });
    }
}
