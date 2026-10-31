package com.example.studentmanager.dal;

import android.content.Context;

import com.example.studentmanager.models.ScheduleFilter;
import com.example.studentmanager.models.ScheduleItem;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.utils.AuthGuard;

import java.util.List;

/** Chỉ có trong src/debug. Database demo tách khỏi dữ liệu ứng dụng. */
public final class SubjectDemoData {
    private static final String DATABASE = "sang_visual_demo.db";
    private static DBHelper helper;
    private SubjectDemoData() {}

    private static synchronized DBHelper database(Context context) {
        if (helper == null) {
            helper = new DBHelper(context.getApplicationContext(), DATABASE);
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
        return new ScheduleRepository(context, database(context), guard(context, false)).listMine(filter);
    }

    public static synchronized void reset(Context context) {
        if (helper != null) { helper.close(); helper = null; }
        context.deleteDatabase(DATABASE);
        database(context).getWritableDatabase();
    }
}
