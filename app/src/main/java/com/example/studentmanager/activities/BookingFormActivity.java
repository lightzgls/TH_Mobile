package com.example.studentmanager.activities;

import android.os.Bundle;
import com.example.studentmanager.R;

/** TODO Thu: man hinh rong, chua co chuc nang. Giu nguyen ten class va extras da chot. */
public class BookingFormActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_booking_form);
        findViewById(R.id.booking_form_back).setOnClickListener(view -> finish());
    }
}
