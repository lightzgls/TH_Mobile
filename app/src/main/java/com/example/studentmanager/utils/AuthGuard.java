package com.example.studentmanager.utils;

import android.content.Context;
import com.example.studentmanager.dal.DbContract;
import com.example.studentmanager.models.Session;

/** Repository van phai kiem tra quyen, ke ca khi UI da an nut. */
public class AuthGuard {
    private final SessionManager sessions;
    public AuthGuard(Context context) { sessions = new SessionManager(context); }
    public boolean requireAdmin() {
        Session session = sessions.getSession();
        return session != null && DbContract.ADMIN.equals(session.role);
    }
    public Session requireStudent() {
        Session session = sessions.getSession();
        return session != null && DbContract.STUDENT.equals(session.role) ? session : null;
    }
}
