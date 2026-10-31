package com.example.studentmanager.activities;

import android.os.Bundle;
import com.example.studentmanager.R;

/** TODO An: man hinh rong, chua co chuc nang. Giu nguyen ten class va extras da chot. */
public class LoginActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_auth_login);
        findViewById(R.id.auth_login_back).setOnClickListener(view -> finish());
    }
}
