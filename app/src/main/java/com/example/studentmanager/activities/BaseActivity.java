package com.example.studentmanager.activities;

import android.view.View;
import androidx.activity.ComponentActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.studentmanager.R;

/** Khung chung: chi xu ly khoang trong system bars, khong chua nghiep vu. */
public abstract class BaseActivity extends ComponentActivity {
    protected void setPage(int layout) {
        setContentView(layout);
        View root = findViewById(R.id.common_root);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
            ViewCompat.requestApplyInsets(root);
        }
    }
}
