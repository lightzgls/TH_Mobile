package com.example.studentmanager.dal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.BookingDraft;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.notifications.NotificationHelper;
import com.example.studentmanager.utils.AuthGuard;
import com.example.studentmanager.utils.BookingValidator;
import com.example.studentmanager.utils.DateTimeHelper;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Goi tren DbExecutor. Moi lan luu deu kiem tra quyen va lich trung trong transaction. */
public class BookingRepository {
    protected final Context context;
    protected final DBHelper dbHelper;
    protected final AuthGuard authGuard;

    public BookingRepository(Context context) {
        this(context, DBHelper.getInstance(context), new AuthGuard(context));
    }

    // Test cung package co the truyen database in-memory va phien gia.
    BookingRepository(Context context, DBHelper dbHelper, AuthGuard authGuard) {
        this.context = context.getApplicationContext();
        this.dbHelper = dbHelper;
        this.authGuard = authGuard;
    }

    // scheduleId = -1 khi tao; studentId lay tu AuthGuard, khong lay tu draft hay Intent.
    public ActionResult save(long scheduleId, BookingDraft draft) {
        Session session;
        ActionResult result;
        try {
            session = authGuard.requireStudent();
            if (session == null || session.studentId <= 0 || !DbContract.STUDENT.equals(session.role)) {
                return ActionResult.error(ActionResult.FORBIDDEN, "Vui lòng đăng nhập bằng tài khoản sinh viên.");
            }
            if (scheduleId != -1 && scheduleId <= 0) {
                return ActionResult.error(ActionResult.VALIDATION, "Mã lịch học không hợp lệ.");
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                result = saveInTransaction(db, session.studentId, scheduleId, draft);
                if (result.ok) db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (SQLiteException | DateTimeException error) {
            Log.e("BookingRepository", "Cannot save schedule", error);
            return ActionResult.error(ActionResult.DB_ERROR, "Không thể lưu lịch học. Vui lòng thử lại.");
        }

        if (result.ok) {
            // Thong bao chi chay sau commit; loi thong bao khong doi ket qua luu.
            try {
                notifySaved(session.studentId, result.id, scheduleId != -1);
            } catch (RuntimeException error) {
                Log.w("BookingRepository", "Schedule saved but notifications could not be updated", error);
            }
        }
        return result;
    }

    private ActionResult saveInTransaction(SQLiteDatabase db, long studentId, long scheduleId,
                                           BookingDraft draft) {
        ActionResult validation = BookingValidator.validate(draft);
        if (!validation.ok) return validation;

        if (scheduleId != -1) {
            ActionResult editable = checkEditable(db, studentId, scheduleId);
            if (!editable.ok) return editable;
        }
        try (Cursor cursor = db.rawQuery("SELECT id FROM Subject WHERE id = ?",
                new String[]{String.valueOf(draft.subjectId)})) {
            if (!cursor.moveToFirst()) {
                return ActionResult.error(ActionResult.NOT_FOUND, "Môn học không còn tồn tại. Vui lòng chọn lại.");
            }
        }

        String date = draft.studyDate.trim();
        String start = draft.startTime.trim();
        String end = draft.endTime.trim();
        String conflictSql = "SELECT id FROM StudySchedule WHERE studentId = ? AND studyDate = ? "
                + "AND status IN ('PENDING','CONFIRMED') AND startTime < ? AND endTime > ? "
                + "AND id <> ? LIMIT 1";
        String[] conflictArgs = {String.valueOf(studentId), date, end, start, String.valueOf(scheduleId)};
        try (Cursor cursor = db.rawQuery(conflictSql, conflictArgs)) {
            if (cursor.moveToFirst()) {
                return ActionResult.error(ActionResult.CONFLICT, "Khoảng giờ này trùng với một lịch học của bạn.");
            }
        }

        ContentValues values = new ContentValues();
        values.put(DbContract.Schedules.SUBJECT_ID, draft.subjectId);
        values.put(DbContract.Schedules.DATE, date);
        values.put(DbContract.Schedules.START, start);
        values.put(DbContract.Schedules.END, end);
        values.put(DbContract.Schedules.LOCATION, draft.location.trim());
        if (scheduleId == -1) {
            values.put(DbContract.Schedules.STUDENT_ID, studentId);
            values.put(DbContract.Schedules.STATUS, DbContract.PENDING);
            long newId = db.insertOrThrow(DbContract.Schedules.TABLE, null, values);
            return ActionResult.success(newId);
        }

        // Khong ghi status khi sua, nen giu nguyen PENDING hoac CONFIRMED.
        int changed = db.update(DbContract.Schedules.TABLE, values, "id = ? AND studentId = ?",
                new String[]{String.valueOf(scheduleId), String.valueOf(studentId)});
        return changed == 1 ? ActionResult.success(scheduleId) : notFound();
    }

    private ActionResult checkEditable(SQLiteDatabase db, long studentId, long scheduleId) {
        try (Cursor cursor = db.rawQuery(
                "SELECT studyDate, startTime, status FROM StudySchedule WHERE id = ? AND studentId = ?",
                new String[]{String.valueOf(scheduleId), String.valueOf(studentId)})) {
            if (!cursor.moveToFirst()) return notFound();
            LocalDate date = LocalDate.parse(cursor.getString(0), DateTimeHelper.DB_DATE);
            LocalTime start = LocalTime.parse(cursor.getString(1), DateTimeHelper.TIME);
            if (DbContract.CANCELLED.equals(cursor.getString(2))
                    || !LocalDateTime.of(date, start).isAfter(LocalDateTime.now())) {
                return ActionResult.error(ActionResult.VALIDATION, "Chỉ được sửa lịch chưa bắt đầu và chưa hủy.");
            }
            return ActionResult.success(scheduleId);
        }
    }

    private ActionResult notFound() {
        return ActionResult.error(ActionResult.NOT_FOUND, "Không tìm thấy lịch học của bạn.");
    }

    protected void notifySaved(long studentId, long scheduleId, boolean updated) {
        NotificationHelper.refresh(context);
        if (updated) NotificationHelper.showChange(context, studentId, scheduleId);
    }
}
