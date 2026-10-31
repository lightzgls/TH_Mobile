package com.example.studentmanager.dal;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.studentmanager.models.ScheduleFilter;
import com.example.studentmanager.models.ScheduleItem;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.utils.AuthGuard;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Chỉ có trong src/debug. Database demo tách khỏi dữ liệu ứng dụng. */
public final class SubjectDemoData {
    private static final String DATABASE = "sang_visual_demo.db";
    private static DBHelper helper;
    private SubjectDemoData() {}

    private static synchronized DBHelper database(Context context) {
        if (helper == null) {
            helper = new DBHelper(context.getApplicationContext(), DATABASE);
            SQLiteDatabase db = helper.getWritableDatabase();
            String today = LocalDate.now().toString();
            // ID riêng để không trùng lịch ngày mai có sẵn trong seed.
            db.execSQL("INSERT OR IGNORE INTO StudySchedule"
                    + "(id,studentId,subjectId,studyDate,startTime,endTime,location,status)"
                    + " VALUES(101,1,1,?,'09:00','10:00','Phòng A101','PENDING'),"
                    + "(102,1,2,?,'14:00','15:30','Thư viện','CONFIRMED'),"
                    + "(103,1,1,?,'16:00','17:00','Đã hủy','CANCELLED'),"
                    + "(104,2,2,?,'08:00','09:00','Sinh viên khác','PENDING')",
                    new Object[]{today, today, today, today});
        }
        return helper;
    }

    public static AuthGuard guard(Context context, boolean admin) {
        return new AuthGuard(context) {
            @Override public boolean requireAdmin() { return admin; }
            @Override public Session requireStudent() {
                if (admin) return null;
                Session session = new Session();
                session.accountId = 2;
                session.studentId = 1;
                session.username = "sv01";
                session.role = DbContract.STUDENT;
                return session;
            }
        };
    }

    public static SubjectRepository subjects(Context context, boolean admin) {
        return new SubjectRepository(context, database(context), guard(context, admin));
    }

    /** Dữ liệu mẫu cho lịch tháng; không triển khai thay ScheduleRepository của Huy. */
    public static List<ScheduleItem> schedules(Context context, ScheduleFilter filter) {
        List<ScheduleItem> items = new ArrayList<>();
        String sql = "SELECT s.*, m.subjectCode, m.subjectName FROM StudySchedule s"
                + " JOIN Subject m ON m.id=s.subjectId"
                + " WHERE s.studentId=? AND s.studyDate>=? AND s.studyDate<=?";
        try (Cursor cursor = database(context).getReadableDatabase().rawQuery(sql,
                new String[]{"1", filter.fromDate, filter.toDate})) {
            while (cursor.moveToNext()) {
                ScheduleItem item = new ScheduleItem();
                item.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                item.studentId = cursor.getLong(cursor.getColumnIndexOrThrow("studentId"));
                item.subjectId = cursor.getLong(cursor.getColumnIndexOrThrow("subjectId"));
                item.subjectCode = cursor.getString(cursor.getColumnIndexOrThrow("subjectCode"));
                item.subjectName = cursor.getString(cursor.getColumnIndexOrThrow("subjectName"));
                item.studyDate = cursor.getString(cursor.getColumnIndexOrThrow("studyDate"));
                item.startTime = cursor.getString(cursor.getColumnIndexOrThrow("startTime"));
                item.endTime = cursor.getString(cursor.getColumnIndexOrThrow("endTime"));
                item.location = cursor.getString(cursor.getColumnIndexOrThrow("location"));
                item.status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                items.add(item);
            }
        }
        return items;
    }

    public static synchronized void reset(Context context) {
        if (helper != null) { helper.close(); helper = null; }
        context.deleteDatabase(DATABASE);
        database(context);
    }
}
