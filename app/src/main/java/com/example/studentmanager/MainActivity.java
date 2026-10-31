package com.example.studentmanager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.example.studentmanager.activities.*;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.utils.DbExecutor;

/** Khung chung. Cac nut preview chi mo man hinh rong trong debug, khong tao phien gia. */
public class MainActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_main);
        findViewById(R.id.main_open_login).setOnClickListener(view ->
                startActivity(new Intent(this, LoginActivity.class)));
        findViewById(R.id.main_admin_preview).setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);
        findViewById(R.id.main_student_preview).setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);
        findViewById(R.id.main_admin_preview).setOnClickListener(view ->
                startActivity(new Intent(this, AdminHomeActivity.class)));
        findViewById(R.id.main_student_preview).setOnClickListener(view ->
                startActivity(new Intent(this, StudentHomeActivity.class)));
        DbExecutor.execute(() -> {
            int message = R.string.main_database_ready;
            try {
                DBHelper.getInstance(this).getWritableDatabase();
            } catch (RuntimeException error) {
                Log.e("StudySchedule", "Cannot initialize demo database", error);
                message = R.string.main_database_error;
            }
            final int statusMessage = message;
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    ((TextView) findViewById(R.id.main_status)).setText(statusMessage);
                }
            });
        });
    }
}
