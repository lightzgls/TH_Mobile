package com.example.studentmanager.activities;

import android.os.Bundle;
import com.example.studentmanager.R;

/** TODO Huy: man hinh rong, chua co chuc nang. Giu nguyen ten class va extras da chot. */
public class ScheduleDetailActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_schedule_detail);
        findViewById(R.id.schedule_detail_back).setOnClickListener(view -> finish());
    }
}
