package com.example.studentmanager.dal;

import android.content.Context;
import android.database.Cursor;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.notifications.NotificationHelper;
import com.example.studentmanager.utils.PasswordHelper;
import com.example.studentmanager.utils.SessionManager;
import java.util.Locale;

/** Goi tren DbExecutor; khong giu View hay Activity. */
public class AuthRepository {
    private final Context context;
    private final SessionManager sessions;
    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        sessions = new SessionManager(context);
    }

    public ActionResult login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ActionResult.error(ActionResult.VALIDATION, "Vui lòng nhập tài khoản và mật khẩu.");
        }
        username = username.trim().toLowerCase(Locale.ROOT);
        if (username.length() > 100 || password.length() > 256) {
            return ActionResult.error(ActionResult.VALIDATION, "Tài khoản tối đa 100 ký tự, mật khẩu tối đa 256 ký tự.");
        }
        try (Cursor cursor = DBHelper.getInstance(context).getReadableDatabase().rawQuery(
                "SELECT a.id,a.username,a.password,a.role,s.id FROM Account a "
                        + "LEFT JOIN Student s ON s.accountId=a.id WHERE a.username=?",
                new String[]{username})) {
            if (!cursor.moveToFirst() || !PasswordHelper.verify(password, cursor.getString(2))) {
                return ActionResult.error(ActionResult.FORBIDDEN, "Tài khoản hoặc mật khẩu không đúng.");
            }
            Session session = new Session();
            session.accountId = cursor.getLong(0);
            session.username = cursor.getString(1);
            session.role = cursor.getString(3);
            session.studentId = DbContract.ADMIN.equals(session.role) || cursor.isNull(4) ? -1 : cursor.getLong(4);
            if ((!DbContract.ADMIN.equals(session.role) && !DbContract.STUDENT.equals(session.role))
                    || (DbContract.STUDENT.equals(session.role) && session.studentId <= 0)) {
                return ActionResult.error(ActionResult.FORBIDDEN, "Tài khoản chưa có hồ sơ hoặc vai trò hợp lệ.");
            }
            sessions.saveSession(session);
            NotificationHelper.refresh(context);
            return ActionResult.success(session.accountId);
        } catch (RuntimeException error) {
            return ActionResult.error(ActionResult.DB_ERROR, "Không thể đăng nhập. Vui lòng thử lại.");
        }
    }

    public ActionResult logout() {
        sessions.clearSession();
        NotificationHelper.refresh(context);
        return ActionResult.success(-1);
    }
}
