package com.example.studentmanager.dal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.ScheduleFilter;
import com.example.studentmanager.models.ScheduleItem;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.notifications.NotificationHelper;
import com.example.studentmanager.utils.AuthGuard;
import com.example.studentmanager.utils.DateTimeHelper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Triển khai tầng dữ liệu lịch học cho sinh viên (Huy phụ trách).
 * Chỉ đọc/ghi lịch của sinh viên đang đăng nhập; dùng SQL bind parameters.
 * Mọi thao tác ghi được thực hiện an toàn và gọi thông báo sau khi hoàn tất.
 */
public class ScheduleRepository {
    protected final Context context;
    protected final DBHelper dbHelper;
    protected final AuthGuard authGuard;

    public ScheduleRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = DBHelper.getInstance(context);
        this.authGuard = new AuthGuard(context);
    }

    ScheduleRepository(Context context, DBHelper dbHelper, AuthGuard authGuard) {
        this.context = context.getApplicationContext();
        this.dbHelper = dbHelper;
        this.authGuard = authGuard;
    }

    public List<ScheduleItem> listMine(ScheduleFilter filter) {
        Session session = authGuard.requireStudent();
        if (session == null || session.studentId <= 0) {
            return Collections.emptyList();
        }

        StringBuilder sql = new StringBuilder(
                "SELECT s.id, s.studentId, s.subjectId, s.studyDate, s.startTime, s.endTime, s.location, s.status, "
                        + "sub.subjectCode, sub.subjectName "
                        + "FROM StudySchedule s "
                        + "INNER JOIN Subject sub ON s.subjectId = sub.id "
                        + "WHERE s.studentId = ?"
        );
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(session.studentId));

        if (filter != null) {
            if (filter.keyword != null && !filter.keyword.trim().isEmpty()) {
                String kw = "%" + filter.keyword.trim() + "%";
                sql.append(" AND (sub.subjectCode LIKE ? COLLATE NOCASE OR sub.subjectName LIKE ? COLLATE NOCASE OR s.location LIKE ? COLLATE NOCASE)");
                args.add(kw);
                args.add(kw);
                args.add(kw);
            }
            if (filter.fromDate != null && !filter.fromDate.trim().isEmpty()) {
                sql.append(" AND s.studyDate >= ?");
                args.add(filter.fromDate.trim());
            }
            if (filter.toDate != null && !filter.toDate.trim().isEmpty()) {
                sql.append(" AND s.studyDate <= ?");
                args.add(filter.toDate.trim());
            }
            if (filter.status != null && !filter.status.trim().isEmpty()) {
                sql.append(" AND s.status = ?");
                args.add(filter.status.trim());
            }
        }

        sql.append(" ORDER BY s.studyDate ASC, s.startTime ASC, s.id ASC");

        List<ScheduleItem> list = new ArrayList<>();
        try (Cursor cursor = dbHelper.getReadableDatabase().rawQuery(sql.toString(), args.toArray(new String[0]))) {
            while (cursor.moveToNext()) {
                list.add(mapScheduleItem(cursor));
            }
        } catch (SQLiteException ignored) {
            return Collections.emptyList();
        }
        return list;
    }

    public ScheduleItem getMine(long id) {
        Session session = authGuard.requireStudent();
        if (session == null || session.studentId <= 0 || id <= 0) {
            return null;
        }

        String sql = "SELECT s.id, s.studentId, s.subjectId, s.studyDate, s.startTime, s.endTime, s.location, s.status, "
                + "sub.subjectCode, sub.subjectName "
                + "FROM StudySchedule s "
                + "INNER JOIN Subject sub ON s.subjectId = sub.id "
                + "WHERE s.id = ? AND s.studentId = ?";
        String[] args = new String[]{String.valueOf(id), String.valueOf(session.studentId)};

        try (Cursor cursor = dbHelper.getReadableDatabase().rawQuery(sql, args)) {
            if (cursor.moveToFirst()) {
                return mapScheduleItem(cursor);
            }
        } catch (SQLiteException ignored) {
            return null;
        }
        return null;
    }

    public ActionResult setStatus(long id, String status) {
        Session session = authGuard.requireStudent();
        if (session == null || session.studentId <= 0) {
            return ActionResult.error(ActionResult.FORBIDDEN, "Bạn cần đăng nhập tài khoản sinh viên");
        }
        if (id <= 0) {
            return ActionResult.error(ActionResult.VALIDATION, "ID lịch học không hợp lệ");
        }
        if (status == null || (!DbContract.CONFIRMED.equals(status) && !DbContract.CANCELLED.equals(status))) {
            return ActionResult.error(ActionResult.VALIDATION, "Trạng thái không hợp lệ");
        }

        SQLiteDatabase db;
        try {
            db = dbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            return ActionResult.error(ActionResult.DB_ERROR, "Không thể mở cơ sở dữ liệu");
        }

        db.beginTransaction();
        try {
            String currentStudyDate;
            String currentStartTime;
            String currentStatus;

            try (Cursor cursor = db.rawQuery(
                    "SELECT studyDate, startTime, status FROM StudySchedule WHERE id = ? AND studentId = ?",
                    new String[]{String.valueOf(id), String.valueOf(session.studentId)})) {
                if (!cursor.moveToFirst()) {
                    return ActionResult.error(ActionResult.NOT_FOUND, "Không tìm thấy lịch học");
                }
                currentStudyDate = cursor.getString(0);
                currentStartTime = cursor.getString(1);
                currentStatus = cursor.getString(2);
            }

            // Kiểm tra lịch chưa bắt đầu
            try {
                LocalDate date = LocalDate.parse(currentStudyDate, DateTimeHelper.DB_DATE);
                LocalTime time = LocalTime.parse(currentStartTime, DateTimeHelper.TIME);
                if (!LocalDateTime.of(date, time).isAfter(LocalDateTime.now())) {
                    return ActionResult.error(ActionResult.VALIDATION, "Không thể đổi trạng thái lịch học đã bắt đầu");
                }
            } catch (Exception e) {
                return ActionResult.error(ActionResult.VALIDATION, "Định dạng ngày giờ của lịch học không hợp lệ");
            }

            // Kiểm tra máy trạng thái (State Machine):
            // PENDING -> CONFIRMED hoặc CANCELLED
            // CONFIRMED -> CANCELLED
            boolean canTransition = false;
            if (DbContract.PENDING.equals(currentStatus)) {
                if (DbContract.CONFIRMED.equals(status) || DbContract.CANCELLED.equals(status)) {
                    canTransition = true;
                }
            } else if (DbContract.CONFIRMED.equals(currentStatus)) {
                if (DbContract.CANCELLED.equals(status)) {
                    canTransition = true;
                }
            }

            if (!canTransition) {
                return ActionResult.error(ActionResult.VALIDATION,
                        "Không thể chuyển từ trạng thái " + currentStatus + " sang " + status);
            }

            ContentValues cv = new ContentValues();
            cv.put(DbContract.Schedules.STATUS, status);
            int updated = db.update(
                    DbContract.Schedules.TABLE,
                    cv,
                    DbContract.Schedules.ID + " = ? AND " + DbContract.Schedules.STUDENT_ID + " = ?",
                    new String[]{String.valueOf(id), String.valueOf(session.studentId)}
            );

            if (updated <= 0) {
                return ActionResult.error(ActionResult.NOT_FOUND, "Không tìm thấy lịch học");
            }

            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            return ActionResult.error(ActionResult.DB_ERROR, "Không thể cập nhật lịch học. Vui lòng thử lại.");
        } finally {
            db.endTransaction();
        }

        try {
            NotificationHelper.refresh(context);
            NotificationHelper.showChange(context, session.studentId, id);
        } catch (RuntimeException ignored) {
            // Lỗi thông báo không làm rollback dữ liệu đã lưu
        }

        return ActionResult.success(id);
    }

    public ActionResult deleteMine(long id) {
        Session session = authGuard.requireStudent();
        if (session == null || session.studentId <= 0) {
            return ActionResult.error(ActionResult.FORBIDDEN, "Bạn cần đăng nhập tài khoản sinh viên");
        }
        if (id <= 0) {
            return ActionResult.error(ActionResult.NOT_FOUND, "Không tìm thấy lịch học cần xóa");
        }

        long studentId = session.studentId;
        SQLiteDatabase db;
        try {
            db = dbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            return ActionResult.error(ActionResult.DB_ERROR, "Không thể mở cơ sở dữ liệu");
        }

        try {
            int deleted = db.delete(
                    DbContract.Schedules.TABLE,
                    DbContract.Schedules.ID + " = ? AND " + DbContract.Schedules.STUDENT_ID + " = ?",
                    new String[]{String.valueOf(id), String.valueOf(studentId)}
            );

            if (deleted <= 0) {
                return ActionResult.error(ActionResult.NOT_FOUND, "Không tìm thấy lịch học cần xóa");
            }
        } catch (SQLiteException e) {
            return ActionResult.error(ActionResult.DB_ERROR, "Không thể xóa lịch học. Vui lòng thử lại.");
        }

        try {
            NotificationHelper.refresh(context);
            NotificationHelper.showChange(context, studentId, id);
        } catch (RuntimeException ignored) {
        }

        return ActionResult.success(id);
    }

    private ScheduleItem mapScheduleItem(Cursor cursor) {
        ScheduleItem item = new ScheduleItem();
        item.id = cursor.getLong(0);
        item.studentId = cursor.getLong(1);
        item.subjectId = cursor.getLong(2);
        item.studyDate = cursor.getString(3);
        item.startTime = cursor.getString(4);
        item.endTime = cursor.getString(5);
        item.location = cursor.getString(6);
        item.status = cursor.getString(7);
        item.subjectCode = cursor.getString(8);
        item.subjectName = cursor.getString(9);
        return item;
    }
}
