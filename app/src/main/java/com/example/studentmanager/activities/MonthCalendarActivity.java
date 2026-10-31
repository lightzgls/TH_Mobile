package com.example.studentmanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.DbContract;
import com.example.studentmanager.dal.ScheduleRepository;
import com.example.studentmanager.models.ScheduleFilter;
import com.example.studentmanager.models.ScheduleItem;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.utils.AppConstants;
import com.example.studentmanager.utils.AuthGuard;
import com.example.studentmanager.utils.DateTimeHelper;
import com.example.studentmanager.utils.DbExecutor;
import com.example.studentmanager.utils.MonthCalendarHelper;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class MonthCalendarActivity extends BaseActivity {
    private LocalDate selectedDate;
    private GridLayout grid;
    private TextView message;
    private LinearLayout scheduleRows;
    private List<ScheduleItem> schedules = new ArrayList<>();
    private boolean loaded;
    private int request;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setPage(R.layout.activity_month_calendar);
        String date = state != null ? state.getString("month_selected_date")
                : getIntent().getStringExtra(AppConstants.EXTRA_SELECTED_DATE);
        selectedDate = LocalDate.now();
        if (date != null) {
            try { selectedDate = LocalDate.parse(date, DateTimeHelper.DB_DATE); }
            catch (DateTimeParseException ignored) { /* Extra sai: dùng hôm nay. */ }
        }
        grid = findViewById(R.id.month_grid);
        message = findViewById(R.id.month_message);
        scheduleRows = findViewById(R.id.month_schedules);
        findViewById(R.id.month_previous).setOnClickListener(view -> changeMonth(-1));
        findViewById(R.id.month_next).setOnClickListener(view -> changeMonth(1));
        findViewById(R.id.month_today).setOnClickListener(view -> {
            selectedDate = LocalDate.now(); loadMonth();
        });
        findViewById(R.id.month_open_day).setOnClickListener(view -> openDay(DayWeekCalendarActivity.class));
        findViewById(R.id.month_book).setOnClickListener(view -> openDay(BookingFormActivity.class));
        findViewById(R.id.month_calendar_back).setOnClickListener(view -> finish());
    }

    private void openDay(Class<?> activity) {
        startActivity(new Intent(this, activity).putExtra(
                AppConstants.EXTRA_SELECTED_DATE, selectedDate.format(DateTimeHelper.DB_DATE)));
    }

    private void changeMonth(int amount) {
        // plusMonths tự kẹp 31/01 sang ngày cuối tháng Hai.
        selectedDate = selectedDate.plusMonths(amount);
        loadMonth();
    }

    @Override protected void onResume() { super.onResume(); loadMonth(); }
    @Override protected void onPause() {
        super.onPause();
        request++; loaded = false; schedules.clear(); renderCalendar();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("month_selected_date", selectedDate.format(DateTimeHelper.DB_DATE));
        super.onSaveInstanceState(state);
    }

    private void loadMonth() {
        int ticket = ++request;
        YearMonth month = YearMonth.from(selectedDate);
        loaded = false; schedules.clear();
        renderCalendar();
        message.setText(R.string.month_loading);
        DbExecutor.execute(() -> {
            try {
                Session session = new AuthGuard(this).requireStudent();
                ScheduleFilter filter = new ScheduleFilter();
                filter.fromDate = month.atDay(1).format(DateTimeHelper.DB_DATE);
                filter.toDate = month.atEndOfMonth().format(DateTimeHelper.DB_DATE);
                List<ScheduleItem> result = session == null ? new ArrayList<>()
                        : MonthCalendarHelper.activeSchedules(
                                new ScheduleRepository(this).listMine(filter), month, session.studentId);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || ticket != request) return;
                    schedules = result;
                    loaded = session != null;
                    renderCalendar();
                    if (!loaded) message.setText(R.string.month_student_required);
                });
            } catch (RuntimeException exception) {
                runOnUiThread(() -> {
                    if (!isDestroyed() && ticket == request) message.setText(R.string.month_error);
                });
            }
        });
    }

    private void renderCalendar() {
        YearMonth month = YearMonth.from(selectedDate);
        ((TextView) findViewById(R.id.month_title)).setText(
                getString(R.string.month_label, month.getMonthValue(), month.getYear()));
        ((TextView) findViewById(R.id.month_selected)).setText(
                getString(R.string.month_selected, selectedDate.format(DateTimeHelper.DISPLAY_DATE)));
        findViewById(R.id.month_open_day).setEnabled(loaded);
        findViewById(R.id.month_book).setEnabled(loaded);
        grid.removeAllViews();
        int cell = 0;
        for (String weekday : getResources().getStringArray(R.array.month_weekdays)) {
            TextView heading = new TextView(this);
            heading.setText(weekday);
            heading.setGravity(Gravity.CENTER);
            grid.addView(heading, cellLayout(cell++, 32));
        }
        for (LocalDate day : MonthCalendarHelper.cells(month)) {
            TextView view = new TextView(this);
            view.setGravity(Gravity.CENTER);
            if (day != null) {
                int count = 0;
                for (ScheduleItem item : schedules) {
                    if (day.format(DateTimeHelper.DB_DATE).equals(item.studyDate)) count++;
                }
                view.setText(count == 0 ? Integer.toString(day.getDayOfMonth())
                        : getString(R.string.month_day_count, day.getDayOfMonth(), count));
                view.setContentDescription(getString(R.string.month_day_accessibility,
                        day.format(DateTimeHelper.DISPLAY_DATE), count));
                view.setBackgroundResource(R.drawable.month_day_background);
                view.setSelected(day.equals(selectedDate));
                view.setTextColor(android.graphics.Color.BLACK);
                view.setFocusable(true);
                view.setOnClickListener(clicked -> { selectedDate = day; renderCalendar(); });
            }
            grid.addView(view, cellLayout(cell++, 52));
        }
        renderSelectedDay();
    }

    private GridLayout.LayoutParams cellLayout(int index, int heightDp) {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(index / 7), GridLayout.spec(index % 7, 1f));
        params.width = 0;
        params.height = Math.round(heightDp * getResources().getDisplayMetrics().density);
        return params;
    }

    private void renderSelectedDay() {
        scheduleRows.removeAllViews();
        int count = 0;
        for (ScheduleItem item : schedules) {
            if (!selectedDate.format(DateTimeHelper.DB_DATE).equals(item.studyDate)) continue;
            count++;
            Button row = new Button(this);
            row.setAllCaps(false);
            row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            row.setText(getString(R.string.month_schedule_row, item.subjectCode, item.subjectName,
                    item.startTime, item.endTime, item.location,
                    getString(DbContract.PENDING.equals(item.status)
                            ? R.string.month_pending : R.string.month_confirmed)));
            row.setOnClickListener(view -> startActivity(new Intent(this, ScheduleDetailActivity.class)
                    .putExtra(AppConstants.EXTRA_SCHEDULE_ID, item.id)));
            scheduleRows.addView(row);
        }
        if (loaded) message.setText(count == 0 ? getString(R.string.month_empty) : "");
    }
}
