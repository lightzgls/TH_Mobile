package com.example.studentmanager.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.studentmanager.R;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.notifications.NotificationHelper;

public class StudentHomeActivity extends BaseActivity {
    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) NotificationHelper.refresh(this);
                else Toast.makeText(this, R.string.notify_permission_denied, Toast.LENGTH_LONG).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_student_home);
        findViewById(R.id.student_open_schedules).setOnClickListener(view -> startActivity(new Intent(this, ScheduleListActivity.class)));
        findViewById(R.id.student_open_booking).setOnClickListener(view -> startActivity(new Intent(this, BookingFormActivity.class)));
        findViewById(R.id.student_open_agenda).setOnClickListener(view -> startActivity(new Intent(this, DayWeekCalendarActivity.class)));
        findViewById(R.id.student_open_month).setOnClickListener(view -> startActivity(new Intent(this, MonthCalendarActivity.class)));
        findViewById(R.id.student_home_back).setOnClickListener(view -> logout());
        findViewById(R.id.notify_enable).setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    && shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
            }
        });
    }

    @Override
    protected void onSessionReady(Session session) {
        NotificationHelper.refresh(this);
        // Chi tu dong hoi mot lan; lan sau nguoi dung chu dong bam nut bat thong bao.
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !getPreferences(MODE_PRIVATE).getBoolean("permissionAsked", false)) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("permissionAsked", true).apply();
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}
