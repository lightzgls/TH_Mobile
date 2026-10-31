package com.example.studentmanager.activities;

import android.content.Intent;
import android.os.Bundle;
import com.example.studentmanager.R;

/** Khung chung: dieu huong den man hinh rong. An bo sung kiem tra phien khi tich hop auth. */
public class AdminHomeActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_admin_home);
        findViewById(R.id.admin_open_students).setOnClickListener(view -> startActivity(new Intent(this, StudentListActivity.class)));
        findViewById(R.id.admin_open_subjects).setOnClickListener(view -> startActivity(new Intent(this, SubjectListActivity.class)));
        findViewById(R.id.admin_home_back).setOnClickListener(view -> finish());
    }
}
