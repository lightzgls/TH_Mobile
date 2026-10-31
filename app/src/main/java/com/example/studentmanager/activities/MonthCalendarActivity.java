package com.example.studentmanager.activities;

import android.os.Bundle;
import com.example.studentmanager.R;

/** TODO Sáng: man hinh rong, chua co chuc nang. Giu nguyen ten class va extras da chot. */
public class MonthCalendarActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_month_calendar);
        findViewById(R.id.month_calendar_back).setOnClickListener(view -> finish());
    }
}
