package com.example.studentmanager.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.DbContract;
import com.example.studentmanager.dal.ScheduleRepository;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.ScheduleItem;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.utils.AppConstants;
import com.example.studentmanager.utils.DateTimeHelper;
import com.example.studentmanager.utils.DbExecutor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Màn hình xem chi tiết lịch học, đổi trạng thái (Xác nhận, Hủy) và xóa lịch (Huy phụ trách).
 */
public class ScheduleDetailActivity extends BaseActivity {
    private ScheduleRepository repository;
    private long scheduleId = -1L;

    private LinearLayout cardContainer;
    private LinearLayout actionsContainer;
    private TextView subjectView;
    private TextView dateView;
    private TextView timeView;
    private TextView locationView;
    private TextView statusView;
    private TextView messageView;
    private ProgressBar progressView;

    private Button editBtn;
    private Button confirmBtn;
    private Button cancelBtn;
    private Button deleteBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_schedule_detail);

        repository = new ScheduleRepository(this);
        scheduleId = getIntent().getLongExtra(AppConstants.EXTRA_SCHEDULE_ID, -1L);

        initViews();
        setupEvents();
    }

    private void initViews() {
        cardContainer = findViewById(R.id.schedule_detail_card_container);
        actionsContainer = findViewById(R.id.schedule_detail_actions_container);
        subjectView = findViewById(R.id.schedule_detail_subject);
        dateView = findViewById(R.id.schedule_detail_date);
        timeView = findViewById(R.id.schedule_detail_time);
        locationView = findViewById(R.id.schedule_detail_location);
        statusView = findViewById(R.id.schedule_detail_status);
        messageView = findViewById(R.id.schedule_detail_message);
        progressView = findViewById(R.id.schedule_detail_progress);

        editBtn = findViewById(R.id.schedule_detail_edit_btn);
        confirmBtn = findViewById(R.id.schedule_detail_confirm_btn);
        cancelBtn = findViewById(R.id.schedule_detail_cancel_btn);
        deleteBtn = findViewById(R.id.schedule_detail_delete_btn);
    }

    private void setupEvents() {
        findViewById(R.id.schedule_detail_back).setOnClickListener(v -> finish());

        editBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookingFormActivity.class);
            intent.putExtra(AppConstants.EXTRA_SCHEDULE_ID, scheduleId);
            startActivity(intent);
        });

        confirmBtn.setOnClickListener(v -> performChangeStatus(DbContract.CONFIRMED));
        cancelBtn.setOnClickListener(v -> showCancelConfirmDialog());
        deleteBtn.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    @Override
    protected void onSessionReady(Session session) {
        loadScheduleDetail();
    }

    private void loadScheduleDetail() {
        if (scheduleId <= 0) {
            showErrorState(getString(R.string.schedule_detail_not_found));
            return;
        }

        if (progressView != null) progressView.setVisibility(View.VISIBLE);

        DbExecutor.execute(() -> {
            ScheduleItem item = repository.getMine(scheduleId);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (progressView != null) progressView.setVisibility(View.GONE);

                if (item == null) {
                    showErrorState(getString(R.string.schedule_detail_not_found));
                } else {
                    renderSchedule(item);
                }
            });
        });
    }

    private void renderSchedule(ScheduleItem item) {
        cardContainer.setVisibility(View.VISIBLE);
        actionsContainer.setVisibility(View.VISIBLE);

        String subjectText = (item.subjectCode != null ? item.subjectCode : "")
                + " - " + (item.subjectName != null ? item.subjectName : "");
        subjectView.setText(subjectText);

        String displayDateStr = item.studyDate;
        try {
            displayDateStr = DateTimeHelper.displayDate(item.studyDate);
        } catch (Exception ignored) {
        }
        dateView.setText(displayDateStr);
        timeView.setText(item.startTime + " - " + item.endTime);
        locationView.setText(item.location != null ? item.location : "");

        // Hiển thị trạng thái rõ ràng
        if (DbContract.PENDING.equals(item.status)) {
            statusView.setText("PENDING - Chờ xác nhận");
            statusView.setTextColor(Color.parseColor("#E65100"));
        } else if (DbContract.CONFIRMED.equals(item.status)) {
            statusView.setText("CONFIRMED - Đã xác nhận");
            statusView.setTextColor(Color.parseColor("#2E7D32"));
        } else if (DbContract.CANCELLED.equals(item.status)) {
            statusView.setText("CANCELLED - Đã hủy");
            statusView.setTextColor(Color.parseColor("#C62828"));
        } else {
            statusView.setText(item.status);
            statusView.setTextColor(Color.DKGRAY);
        }

        boolean isFuture = isFutureSchedule(item.studyDate, item.startTime);

        if (!isFuture) {
            // Lịch đã bắt đầu hoặc trong quá khứ
            editBtn.setVisibility(View.GONE);
            confirmBtn.setVisibility(View.GONE);
            cancelBtn.setVisibility(View.GONE);
            deleteBtn.setVisibility(View.VISIBLE);

            messageView.setVisibility(View.VISIBLE);
            messageView.setText(R.string.schedule_detail_past_msg);
        } else {
            // Lịch tương lai
            if (DbContract.PENDING.equals(item.status)) {
                editBtn.setVisibility(View.VISIBLE);
                confirmBtn.setVisibility(View.VISIBLE);
                cancelBtn.setVisibility(View.VISIBLE);
                deleteBtn.setVisibility(View.VISIBLE);
                messageView.setVisibility(View.GONE);
            } else if (DbContract.CONFIRMED.equals(item.status)) {
                editBtn.setVisibility(View.VISIBLE);
                confirmBtn.setVisibility(View.GONE);
                cancelBtn.setVisibility(View.VISIBLE);
                deleteBtn.setVisibility(View.VISIBLE);
                messageView.setVisibility(View.GONE);
            } else if (DbContract.CANCELLED.equals(item.status)) {
                editBtn.setVisibility(View.GONE);
                confirmBtn.setVisibility(View.GONE);
                cancelBtn.setVisibility(View.GONE);
                deleteBtn.setVisibility(View.VISIBLE);

                messageView.setVisibility(View.VISIBLE);
                messageView.setText(R.string.schedule_detail_cancelled_msg);
            }
        }
    }

    private boolean isFutureSchedule(String studyDate, String startTime) {
        try {
            LocalDate d = LocalDate.parse(studyDate, DateTimeHelper.DB_DATE);
            LocalTime t = LocalTime.parse(startTime, DateTimeHelper.TIME);
            return LocalDateTime.of(d, t).isAfter(LocalDateTime.now());
        } catch (Exception e) {
            return false;
        }
    }

    private void showErrorState(String message) {
        cardContainer.setVisibility(View.GONE);
        actionsContainer.setVisibility(View.GONE);
        messageView.setVisibility(View.VISIBLE);
        messageView.setText(message);
    }

    private void showCancelConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.schedule_detail_cancel_dialog_title)
                .setMessage(R.string.schedule_detail_cancel_dialog_msg)
                .setPositiveButton(R.string.schedule_common_confirm, (dialog, which) ->
                        performChangeStatus(DbContract.CANCELLED))
                .setNegativeButton(R.string.schedule_common_cancel, null)
                .show();
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.schedule_detail_delete_dialog_title)
                .setMessage(R.string.schedule_detail_delete_dialog_msg)
                .setPositiveButton(R.string.schedule_common_confirm, (dialog, which) ->
                        performDelete())
                .setNegativeButton(R.string.schedule_common_cancel, null)
                .show();
    }

    private void performChangeStatus(String targetStatus) {
        setButtonsEnabled(false);
        if (progressView != null) progressView.setVisibility(View.VISIBLE);

        DbExecutor.execute(() -> {
            ActionResult result = repository.setStatus(scheduleId, targetStatus);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                setButtonsEnabled(true);
                if (progressView != null) progressView.setVisibility(View.GONE);

                if (result.ok) {
                    int msgRes = DbContract.CONFIRMED.equals(targetStatus)
                            ? R.string.schedule_detail_confirm_success
                            : R.string.schedule_detail_cancel_success;
                    Toast.makeText(this, msgRes, Toast.LENGTH_SHORT).show();
                    loadScheduleDetail();
                } else {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void performDelete() {
        setButtonsEnabled(false);
        if (progressView != null) progressView.setVisibility(View.VISIBLE);

        DbExecutor.execute(() -> {
            ActionResult result = repository.deleteMine(scheduleId);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                setButtonsEnabled(true);
                if (progressView != null) progressView.setVisibility(View.GONE);

                if (result.ok) {
                    Toast.makeText(this, R.string.schedule_detail_delete_success, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        editBtn.setEnabled(enabled);
        confirmBtn.setEnabled(enabled);
        cancelBtn.setEnabled(enabled);
        deleteBtn.setEnabled(enabled);
    }
}
