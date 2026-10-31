package com.example.studentmanager.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.BookingRepository;
import com.example.studentmanager.dal.DbContract;
import com.example.studentmanager.dal.ScheduleRepository;
import com.example.studentmanager.dal.SubjectRepository;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.BookingDraft;
import com.example.studentmanager.models.ScheduleItem;
import com.example.studentmanager.models.Subject;
import com.example.studentmanager.utils.AppConstants;
import com.example.studentmanager.utils.AuthGuard;
import com.example.studentmanager.utils.BookingValidator;
import com.example.studentmanager.utils.DateTimeHelper;
import com.example.studentmanager.utils.DbExecutor;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Thu: dung chung mot form cho ca tao moi va sua lich. */
public class BookingFormActivity extends BaseActivity {
    private long scheduleId;
    private long subjectId = -1;
    private String studyDate;
    private String startTime = "";
    private String endTime = "";
    private String subjectLabel = "";
    private Button subjectButton, dateButton, startButton, endButton, saveButton;
    private EditText locationInput;
    private TextView errorText;

    private final ActivityResultLauncher<Intent> subjectPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    long id = result.getData().getLongExtra(AppConstants.EXTRA_SUBJECT_ID, -1);
                    if (id > 0) loadSubject(id);
                    else showError(getString(R.string.booking_subject_missing));
                }
            });

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setPage(R.layout.activity_booking_form);
        subjectButton = findViewById(R.id.booking_form_subject);
        dateButton = findViewById(R.id.booking_form_date);
        startButton = findViewById(R.id.booking_form_start);
        endButton = findViewById(R.id.booking_form_end);
        saveButton = findViewById(R.id.booking_form_save);
        locationInput = findViewById(R.id.booking_form_location);
        errorText = findViewById(R.id.booking_form_error);
        scheduleId = getIntent().getLongExtra(AppConstants.EXTRA_SCHEDULE_ID, -1);
        studyDate = validDateOrToday(getIntent().getStringExtra(AppConstants.EXTRA_SELECTED_DATE));
        if (state != null) {
            subjectId = state.getLong("booking_subject_id", -1);
            subjectLabel = state.getString("booking_subject_label", "");
            studyDate = state.getString("booking_date", studyDate);
            startTime = state.getString("booking_start", "");
            endTime = state.getString("booking_end", "");
            locationInput.setText(state.getString("booking_location", ""));
            showError(state.getString("booking_error", ""));
        }
        ((TextView) findViewById(R.id.booking_form_title)).setText(scheduleId == -1
                ? R.string.booking_create_title : R.string.booking_edit_title);
        subjectButton.setOnClickListener(view -> subjectPicker.launch(new Intent(this, SubjectPickerActivity.class)));
        dateButton.setOnClickListener(view -> chooseDate());
        startButton.setOnClickListener(view -> chooseTime(true));
        endButton.setOnClickListener(view -> chooseTime(false));
        saveButton.setOnClickListener(view -> save());
        findViewById(R.id.booking_form_back).setOnClickListener(view -> finish());
        updateLabels();
        loadForm(state != null);
    }

    private void loadForm(boolean restored) {
        setBusy(true);
        DbExecutor.execute(() -> {
            try {
                if (new AuthGuard(this).requireStudent() == null) {
                    showOnScreen(() -> finishWithError(R.string.booking_login_required));
                    return;
                }
                ScheduleItem item = scheduleId == -1 ? null : new ScheduleRepository(this).getMine(scheduleId);
                if (scheduleId != -1 && item == null) {
                    showOnScreen(() -> finishWithError(R.string.booking_schedule_missing));
                    return;
                }
                if (item != null) {
                    LocalDateTime start = LocalDateTime.of(
                            LocalDate.parse(item.studyDate, DateTimeHelper.DB_DATE),
                            LocalTime.parse(item.startTime, DateTimeHelper.TIME));
                    if (DbContract.CANCELLED.equals(item.status) || !start.isAfter(LocalDateTime.now())) {
                        showOnScreen(() -> finishWithError(R.string.booking_not_editable));
                        return;
                    }
                }
                showOnScreen(() -> {
                    if (item != null && !restored) {
                        subjectId = item.subjectId;
                        studyDate = item.studyDate;
                        startTime = item.startTime;
                        endTime = item.endTime;
                        locationInput.setText(item.location);
                    }
                    updateLabels();
                    setBusy(false);
                    if (subjectId > 0) loadSubject(subjectId);
                });
            } catch (RuntimeException error) {
                Log.e("BookingForm", "Cannot load booking form", error);
                showOnScreen(() -> finishWithError(R.string.booking_load_error));
            }
        });
    }

    private void loadSubject(long id) {
        setBusy(true);
        DbExecutor.execute(() -> {
            try {
                Subject subject = new SubjectRepository(this).get(id);
                showOnScreen(() -> {
                    setBusy(false);
                    if (subject == null) {
                        subjectId = -1;
                        subjectLabel = "";
                        showError(getString(R.string.booking_subject_missing));
                    } else {
                        subjectId = subject.id;
                        subjectLabel = getString(R.string.booking_subject_value, subject.subjectCode, subject.subjectName);
                        showError("");
                    }
                    updateLabels();
                });
            } catch (RuntimeException error) {
                Log.e("BookingForm", "Cannot load subject", error);
                showOnScreen(() -> {
                    setBusy(false);
                    showError(getString(R.string.booking_load_error));
                });
            }
        });
    }

    private void chooseDate() {
        LocalDate date = LocalDate.parse(studyDate, DateTimeHelper.DB_DATE);
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
            studyDate = LocalDate.of(year, month + 1, day).toString();
            updateLabels();
        }, date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
        picker.getDatePicker().setMinDate(System.currentTimeMillis());
        picker.show();
    }

    private void chooseTime(boolean choosingStart) {
        String value = choosingStart ? startTime : endTime;
        LocalTime time = value.isEmpty() ? LocalTime.now() : LocalTime.parse(value, DateTimeHelper.TIME);
        new TimePickerDialog(this, (view, hour, minute) -> {
            String selected = DateTimeHelper.databaseTime(hour, minute);
            if (choosingStart) startTime = selected;
            else endTime = selected;
            updateLabels();
        }, time.getHour(), time.getMinute(), true).show();
    }

    private void save() {
        BookingDraft draft = new BookingDraft();
        draft.subjectId = subjectId;
        draft.studyDate = studyDate;
        draft.startTime = startTime;
        draft.endTime = endTime;
        draft.location = locationInput.getText().toString().trim();
        ActionResult validation = BookingValidator.validate(draft);
        if (!validation.ok) {
            showError(validation.message);
            return;
        }
        showError("");
        setBusy(true);
        DbExecutor.execute(() -> {
            try {
                ActionResult result = new BookingRepository(this).save(scheduleId, draft);
                showOnScreen(() -> {
                    setBusy(false);
                    if (!result.ok) {
                        showError(result.message);
                        return;
                    }
                    setResult(RESULT_OK, new Intent().putExtra(AppConstants.EXTRA_SCHEDULE_ID, result.id));
                    Toast.makeText(this, scheduleId == -1 ? R.string.booking_created : R.string.booking_updated,
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (RuntimeException error) {
                Log.e("BookingForm", "Cannot save booking", error);
                showOnScreen(() -> {
                    setBusy(false);
                    showError(getString(R.string.booking_save_error));
                });
            }
        });
    }

    private void updateLabels() {
        subjectButton.setText(subjectLabel.isEmpty() ? getString(R.string.booking_choose_subject) : subjectLabel);
        dateButton.setText(DateTimeHelper.displayDate(studyDate));
        startButton.setText(startTime.isEmpty() ? getString(R.string.booking_choose_time) : startTime);
        endButton.setText(endTime.isEmpty() ? getString(R.string.booking_choose_time) : endTime);
    }

    private void setBusy(boolean busy) {
        saveButton.setEnabled(!busy);
        subjectButton.setEnabled(!busy);
        dateButton.setEnabled(!busy);
        startButton.setEnabled(!busy);
        endButton.setEnabled(!busy);
        locationInput.setEnabled(!busy);
        findViewById(R.id.booking_form_progress).setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(message.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void finishWithError(int message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    private void showOnScreen(Runnable update) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) update.run();
        });
    }

    private String validDateOrToday(String value) {
        try {
            return LocalDate.parse(value == null ? "" : value, DateTimeHelper.DB_DATE).toString();
        } catch (DateTimeException error) {
            return LocalDate.now().toString();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putLong("booking_subject_id", subjectId);
        state.putString("booking_subject_label", subjectLabel);
        state.putString("booking_date", studyDate);
        state.putString("booking_start", startTime);
        state.putString("booking_end", endTime);
        state.putString("booking_location", locationInput.getText().toString());
        state.putString("booking_error", errorText.getText().toString());
        super.onSaveInstanceState(state);
    }
}
