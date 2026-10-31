package com.example.studentmanager.activities;

import android.os.Bundle;
import com.example.studentmanager.R;

/** TODO Huy: man hinh rong, chua co chuc nang. Giu nguyen ten class va extras da chot. */
public class DayWeekCalendarActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_agenda_calendar);
        findViewById(R.id.agenda_calendar_back).setOnClickListener(view -> finish());
    }
}
