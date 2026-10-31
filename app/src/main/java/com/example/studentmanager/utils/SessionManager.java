package com.example.studentmanager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.dal.DbContract;
import com.example.studentmanager.models.Session;
import java.util.UUID;

/** Goi tren DbExecutor: phien luon duoc doi chieu voi SQLite. */
public class SessionManager {
    private final SharedPreferences preferences;
    private final DBHelper dbHelper;
    public SessionManager(Context context) {
        this(context, DBHelper.getInstance(context));
    }

    /** Cho test truyen database in-memory, khong cham vao du lieu dang dung. */
    public SessionManager(Context context, DBHelper helper) {
        preferences = context.getSharedPreferences(AppConstants.SESSION_PREFS, Context.MODE_PRIVATE);
        dbHelper = helper;
    }

    public void saveSession(Session session) {
        if (session == null || session.accountId <= 0 || session.username == null
                || (!DbContract.ADMIN.equals(session.role) && !DbContract.STUDENT.equals(session.role))
                || (DbContract.STUDENT.equals(session.role) && session.studentId <= 0)
                || (DbContract.ADMIN.equals(session.role) && session.studentId != -1)) {
            throw new IllegalArgumentException("Phiên đăng nhập không hợp lệ.");
        }
        boolean saved = preferences.edit().clear()
                .putLong("accountId", session.accountId).putLong("studentId", session.studentId)
                .putString("username", session.username).putString("role", session.role)
                .putString("token", UUID.randomUUID().toString()).commit();
        if (!saved) {
            clearSession();
            throw new IllegalStateException("Không thể lưu phiên đăng nhập.");
        }
    }

    public Session getSession() {
        try {
            long accountId = preferences.getLong("accountId", -1);
            if (accountId <= 0) return null;
            try (Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT a.username,a.role,s.id FROM Account a LEFT JOIN Student s ON s.accountId=a.id WHERE a.id=?",
                    new String[]{String.valueOf(accountId)})) {
                if (cursor.moveToFirst()) {
                    String username = cursor.getString(0);
                    String role = cursor.getString(1);
                    long studentId = cursor.isNull(2) ? -1 : cursor.getLong(2);
                    if (DbContract.ADMIN.equals(role)) studentId = -1;
                    boolean validRole = DbContract.ADMIN.equals(role)
                            || (DbContract.STUDENT.equals(role) && studentId > 0);
                    if (validRole && username.equals(preferences.getString("username", ""))
                            && role.equals(preferences.getString("role", ""))
                            && studentId == preferences.getLong("studentId", -1)
                            && !getSessionToken().isEmpty()) {
                        Session session = new Session();
                        session.accountId = accountId;
                        session.studentId = studentId;
                        session.username = username;
                        session.role = role;
                        return session;
                    }
                }
            }
        } catch (ClassCastException error) {
            // Preferences cu hoac hong: yeu cau dang nhap lai.
        }
        clearSession();
        return null;
    }

    public String getSessionToken() { return preferences.getString("token", ""); }
    // apply cap nhat ngay trong bo nho va ghi dia bat dong bo.
    public void clearSession() { preferences.edit().clear().apply(); }
}
