package com.example.studentmanager.activities;

import android.os.Bundle;
import com.example.studentmanager.R;

/** TODO Quang: man hinh rong, chua co chuc nang. Giu nguyen ten class va extras da chot. */
public class StudentFormActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_student_form);
        findViewById(R.id.student_form_back).setOnClickListener(view -> finish());
    }
}
