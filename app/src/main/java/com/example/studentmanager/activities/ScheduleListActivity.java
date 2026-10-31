package com.example.studentmanager.activities;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Màn hình danh sách lịch học của sinh viên (Huy phụ trách).
 * Hỗ trợ tìm kiếm từ khóa, lọc khoảng ngày, lọc trạng thái,
 * xem chi tiết, đặt lịch mới và chuyển sang lịch biểu ngày/tuần.
 */
public class ScheduleListActivity extends BaseActivity {
    private ScheduleRepository repository;
    private EditText keywordInput;
    private Button fromDateBtn;
    private Button toDateBtn;
    private Spinner statusSpinner;
    private ListView scheduleListView;
    private LinearLayout emptyContainer;
    private ProgressBar progressBar;
    private ScheduleAdapter adapter;

    private String selectedFromDateDb;
    private String selectedToDateDb;
    private String selectedStatus;
    private ScheduleFilter activeFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPage(R.layout.activity_schedule_list);

        repository = new ScheduleRepository(this);

        initViews();
        setupEvents();
    }

    private void initViews() {
        keywordInput = findViewById(R.id.schedule_list_keyword_input);
        fromDateBtn = findViewById(R.id.schedule_list_from_date_btn);
        toDateBtn = findViewById(R.id.schedule_list_to_date_btn);
        statusSpinner = findViewById(R.id.schedule_list_status_spinner);
        scheduleListView = findViewById(R.id.schedule_list_items);
        emptyContainer = findViewById(R.id.schedule_list_empty_container);
        progressBar = findViewById(R.id.schedule_list_progress);

        adapter = new ScheduleAdapter(this);
        scheduleListView.setAdapter(adapter);

        // Khởi tạo Spinner trạng thái
        String[] statusOptions = new String[]{
                getString(R.string.schedule_status_all),
                getString(R.string.schedule_status_pending),
                getString(R.string.schedule_status_confirmed),
                getString(R.string.schedule_status_cancelled)
        };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                statusOptions
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(spinnerAdapter);
    }

    private void setupEvents() {
        findViewById(R.id.schedule_list_back).setOnClickListener(v -> finish());

        findViewById(R.id.schedule_list_add_booking_btn).setOnClickListener(v ->
                startActivity(new Intent(this, BookingFormActivity.class)));
        findViewById(R.id.schedule_list_empty_add_btn).setOnClickListener(v ->
                startActivity(new Intent(this, BookingFormActivity.class)));

        findViewById(R.id.schedule_list_open_agenda_btn).setOnClickListener(v ->
                startActivity(new Intent(this, DayWeekCalendarActivity.class)));

        fromDateBtn.setOnClickListener(v -> showDatePicker(true));
        toDateBtn.setOnClickListener(v -> showDatePicker(false));

        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1:
                        selectedStatus = DbContract.PENDING;
                        break;
                    case 2:
                        selectedStatus = DbContract.CONFIRMED;
                        break;
                    case 3:
                        selectedStatus = DbContract.CANCELLED;
                        break;
                    default:
                        selectedStatus = null;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedStatus = null;
            }
        });

        findViewById(R.id.schedule_list_apply_filter_btn).setOnClickListener(v -> applyFilter());
        keywordInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                applyFilter();
                return true;
            }
            return false;
        });

        findViewById(R.id.schedule_list_clear_filter_btn).setOnClickListener(v -> clearFilter());
        findViewById(R.id.schedule_list_empty_clear_btn).setOnClickListener(v -> clearFilter());

        scheduleListView.setOnItemClickListener((parent, view, position, id) -> {
            ScheduleItem item = adapter.getItem(position);
            if (item != null) {
                Intent intent = new Intent(this, ScheduleDetailActivity.class);
                intent.putExtra(AppConstants.EXTRA_SCHEDULE_ID, item.id);
                startActivity(intent);
            }
        });
    }

    private void showDatePicker(boolean isFromDate) {
        String currentDbDate = isFromDate ? selectedFromDateDb : selectedToDateDb;
        LocalDate initialDate;
        if (currentDbDate != null && !currentDbDate.isEmpty()) {
            try {
                initialDate = LocalDate.parse(currentDbDate, DateTimeHelper.DB_DATE);
            } catch (Exception e) {
                initialDate = LocalDate.now();
            }
        } else {
            initialDate = LocalDate.now();
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String dateDb = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    String dateDisplay = DateTimeHelper.displayDate(dateDb);
                    if (isFromDate) {
                        selectedFromDateDb = dateDb;
                        fromDateBtn.setText(dateDisplay);
                    } else {
                        selectedToDateDb = dateDb;
                        toDateBtn.setText(dateDisplay);
                    }
                },
                initialDate.getYear(),
                initialDate.getMonthValue() - 1,
                initialDate.getDayOfMonth()
        );
        dialog.show();
    }

    private void applyFilter() {
        if (selectedFromDateDb != null && selectedToDateDb != null
                && selectedFromDateDb.compareTo(selectedToDateDb) > 0) {
            Toast.makeText(this, R.string.schedule_invalid_range, Toast.LENGTH_LONG).show();
            return;
        }
        ScheduleFilter filter = new ScheduleFilter();
        String keyword = keywordInput.getText().toString().trim();
        filter.keyword = keyword.isEmpty() ? null : keyword;
        filter.fromDate = selectedFromDateDb;
        filter.toDate = selectedToDateDb;
        filter.status = selectedStatus;

        activeFilter = filter;
        loadSchedules(activeFilter);
    }

    private void clearFilter() {
        keywordInput.setText("");
        selectedFromDateDb = null;
        selectedToDateDb = null;
        selectedStatus = null;

        fromDateBtn.setText(R.string.schedule_list_from_date);
        toDateBtn.setText(R.string.schedule_list_to_date);
        statusSpinner.setSelection(0);

        activeFilter = null;
        loadSchedules(null);
    }

    @Override
    protected void onSessionReady(Session session) {
        // Tự động làm mới danh sách khi vào màn hình hoặc khi quay lại
        loadSchedules(activeFilter);
    }

    private void loadSchedules(ScheduleFilter filter) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        DbExecutor.execute(() -> {
            List<ScheduleItem> list;
            try {
                list = repository.listMine(filter);
            } catch (RuntimeException error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.schedule_load_error, Toast.LENGTH_LONG).show();
                });
                return;
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                updateListUI(list);
            });
        });
    }

    private void updateListUI(List<ScheduleItem> list) {
        if (list == null || list.isEmpty()) {
            scheduleListView.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.VISIBLE);
        } else {
            emptyContainer.setVisibility(View.GONE);
            scheduleListView.setVisibility(View.VISIBLE);
            adapter.setData(list);
        }
    }

    private static class ScheduleAdapter extends BaseAdapter {
        private final Context context;
        private final LayoutInflater inflater;
        private final List<ScheduleItem> items = new ArrayList<>();

        ScheduleAdapter(Context context) {
            this.context = context;
            this.inflater = LayoutInflater.from(context);
        }

        void setData(List<ScheduleItem> newItems) {
            items.clear();
            if (newItems != null) {
                items.addAll(newItems);
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public ScheduleItem getItem(int position) {
            return position >= 0 && position < items.size() ? items.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            ScheduleItem item = getItem(position);
            return item != null ? item.id : position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.schedule_item_row, parent, false);
            }

            ScheduleItem item = getItem(position);
            if (item != null) {
                TextView subjectView = view.findViewById(R.id.schedule_row_subject);
                TextView timeView = view.findViewById(R.id.schedule_row_time);
                TextView locationView = view.findViewById(R.id.schedule_row_location);
                TextView statusView = view.findViewById(R.id.schedule_row_status);

                String subjectText = (item.subjectCode != null ? item.subjectCode : "")
                        + " - " + (item.subjectName != null ? item.subjectName : "");
                subjectView.setText(subjectText);

                String displayDateStr = item.studyDate;
                try {
                    displayDateStr = DateTimeHelper.displayDate(item.studyDate);
                } catch (Exception ignored) {
                }
                String timeText = "Ngày: " + displayDateStr + " | " + item.startTime + " - " + item.endTime;
                timeView.setText(timeText);

                locationView.setText("Địa điểm: " + (item.location != null ? item.location : ""));

                // Trạng thái kèm màu phân biệt
                if (DbContract.PENDING.equals(item.status)) {
                    statusView.setText("Trạng thái: PENDING - Chờ xác nhận");
                    statusView.setTextColor(Color.parseColor("#E65100")); // Deep Orange
                } else if (DbContract.CONFIRMED.equals(item.status)) {
                    statusView.setText("Trạng thái: CONFIRMED - Đã xác nhận");
                    statusView.setTextColor(Color.parseColor("#2E7D32")); // Dark Green
                } else if (DbContract.CANCELLED.equals(item.status)) {
                    statusView.setText("Trạng thái: CANCELLED - Đã hủy");
                    statusView.setTextColor(Color.parseColor("#C62828")); // Red
                } else {
                    statusView.setText("Trạng thái: " + item.status);
                    statusView.setTextColor(Color.DKGRAY);
                }
            }

            return view;
        }
    }
}
