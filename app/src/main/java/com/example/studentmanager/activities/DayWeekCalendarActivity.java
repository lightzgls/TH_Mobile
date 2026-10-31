package com.example.studentmanager.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.DbContract;
import com.example.studentmanager.dal.ScheduleRepository;
import com.example.studentmanager.models.ScheduleFilter;
import com.example.studentmanager.models.ScheduleItem;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.utils.AppConstants;
import com.example.studentmanager.utils.DateTimeHelper;
import com.example.studentmanager.utils.DbExecutor;
import java.time.LocalDate;
import java.util.List;

/** Ngay la mot nhom; tuan gom bay nhom tu thu Hai den Chu nhat. */
public class DayWeekCalendarActivity extends BaseActivity {
    private LocalDate selectedDate;
    private boolean week;
    private int request;
    private TextView title, message;
    private LinearLayout rows;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setPage(R.layout.activity_agenda_calendar);
        String value = state != null ? state.getString("agenda_date")
                : getIntent().getStringExtra(AppConstants.EXTRA_SELECTED_DATE);
        try { selectedDate = LocalDate.parse(value == null ? "" : value, DateTimeHelper.DB_DATE); }
        catch (RuntimeException error) { selectedDate = LocalDate.now(); }
        week = state != null && state.getBoolean("agenda_week");
        title = findViewById(R.id.agenda_range);
        message = findViewById(R.id.agenda_message);
        rows = findViewById(R.id.agenda_rows);
        findViewById(R.id.agenda_day).setOnClickListener(view -> { week = false; load(); });
        findViewById(R.id.agenda_week).setOnClickListener(view -> { week = true; load(); });
        findViewById(R.id.agenda_previous).setOnClickListener(view -> move(-1));
        findViewById(R.id.agenda_next).setOnClickListener(view -> move(1));
        findViewById(R.id.agenda_today).setOnClickListener(view -> { selectedDate = LocalDate.now(); load(); });
        findViewById(R.id.agenda_choose).setOnClickListener(view -> new DatePickerDialog(this,
                (picker, year, month, day) -> { selectedDate = LocalDate.of(year, month + 1, day); load(); },
                selectedDate.getYear(), selectedDate.getMonthValue() - 1, selectedDate.getDayOfMonth()).show());
        findViewById(R.id.agenda_book).setOnClickListener(view -> openBooking(selectedDate));
        findViewById(R.id.agenda_month).setOnClickListener(view -> startActivity(
                new Intent(this, MonthCalendarActivity.class)
                        .putExtra(AppConstants.EXTRA_SELECTED_DATE, selectedDate.toString())));
        findViewById(R.id.agenda_calendar_back).setOnClickListener(view -> finish());
    }

    @Override protected void onSessionReady(Session session) { load(); }
    @Override protected void onPause() { request++; super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("agenda_date", selectedDate.toString());
        state.putBoolean("agenda_week", week);
        super.onSaveInstanceState(state);
    }

    private void move(int amount) {
        selectedDate = selectedDate.plusDays(week ? amount * 7L : amount);
        load();
    }

    private void openBooking(LocalDate date) {
        startActivity(new Intent(this, BookingFormActivity.class)
                .putExtra(AppConstants.EXTRA_SELECTED_DATE, date.toString()));
    }

    private void load() {
        int ticket = ++request;
        LocalDate first = week ? selectedDate.minusDays(selectedDate.getDayOfWeek().getValue() - 1L) : selectedDate;
        LocalDate last = week ? first.plusDays(6) : first;
        title.setText(first.equals(last) ? first.format(DateTimeHelper.DISPLAY_DATE)
                : getString(R.string.agenda_range, first.format(DateTimeHelper.DISPLAY_DATE),
                        last.format(DateTimeHelper.DISPLAY_DATE)));
        message.setText(R.string.schedule_list_loading);
        rows.removeAllViews();
        ScheduleFilter filter = new ScheduleFilter();
        filter.fromDate = first.toString();
        filter.toDate = last.toString();
        DbExecutor.execute(() -> {
            try {
                List<ScheduleItem> items = new ScheduleRepository(this).listMine(filter);
                runOnUiThread(() -> {
                    if (ticket != request || isFinishing() || isDestroyed()) return;
                    render(first, last, items);
                });
            } catch (RuntimeException error) {
                runOnUiThread(() -> {
                    if (ticket == request && !isFinishing() && !isDestroyed())
                        message.setText(R.string.schedule_load_error);
                });
            }
        });
    }

    private void render(LocalDate first, LocalDate last, List<ScheduleItem> items) {
        int total = 0;
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            LocalDate rowDate = day;
            TextView heading = new TextView(this);
            heading.setText(day.format(DateTimeHelper.DISPLAY_DATE));
            heading.setTextSize(18);
            heading.setPadding(0, 16, 0, 4);
            rows.addView(heading);
            int count = 0;
            for (ScheduleItem item : items) {
                if (!day.toString().equals(item.studyDate) || DbContract.CANCELLED.equals(item.status)) continue;
                count++;
                total++;
                Button row = new Button(this);
                row.setAllCaps(false);
                row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                row.setText(getString(R.string.agenda_row, item.startTime, item.endTime,
                        item.subjectCode, item.subjectName, item.location,
                        getString(DbContract.PENDING.equals(item.status)
                                ? R.string.schedule_status_pending : R.string.schedule_status_confirmed)));
                row.setOnClickListener(view -> startActivity(new Intent(this, ScheduleDetailActivity.class)
                        .putExtra(AppConstants.EXTRA_SCHEDULE_ID, item.id)));
                rows.addView(row);
            }
            if (count == 0) {
                TextView empty = new TextView(this);
                empty.setText(R.string.agenda_empty);
                rows.addView(empty);
            }
            Button add = new Button(this);
            add.setText(R.string.agenda_book_day);
            add.setOnClickListener(view -> openBooking(rowDate));
            rows.addView(add);
        }
        message.setText(total == 0 ? getString(R.string.agenda_empty)
                : getString(R.string.agenda_count, total));
    }
}
