package com.example.studentmanager.activities;

import android.os.Bundle;
import com.example.studentmanager.R;

/** TODO Huy: man hinh rong, chua co chuc nang. Giu nguyen ten class va extras da chot. */
public class ScheduleListActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_schedule_list);
        findViewById(R.id.schedule_list_back).setOnClickListener(view -> finish());
    }
}
