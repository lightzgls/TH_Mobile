package com.example.studentmanager.activities;

import android.content.Intent;
import android.os.Bundle;
import com.example.studentmanager.R;

/** Khung chung: dieu huong den man hinh rong. An bo sung kiem tra phien khi tich hop auth. */
public class StudentHomeActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_student_home);
        findViewById(R.id.student_open_schedules).setOnClickListener(view -> startActivity(new Intent(this, ScheduleListActivity.class)));
        findViewById(R.id.student_open_booking).setOnClickListener(view -> startActivity(new Intent(this, BookingFormActivity.class)));
        findViewById(R.id.student_open_agenda).setOnClickListener(view -> startActivity(new Intent(this, DayWeekCalendarActivity.class)));
        findViewById(R.id.student_open_month).setOnClickListener(view -> startActivity(new Intent(this, MonthCalendarActivity.class)));
        findViewById(R.id.student_home_back).setOnClickListener(view -> finish());
    }
}
