package com.example.studentmanager.activities;

import android.os.Bundle;
import com.example.studentmanager.R;

/** TODO Sáng: man hinh rong, chua co chuc nang. Giu nguyen ten class va extras da chot. */
public class SubjectPickerActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_subject_picker);
        findViewById(R.id.subject_picker_back).setOnClickListener(view -> finish());
    }
}
