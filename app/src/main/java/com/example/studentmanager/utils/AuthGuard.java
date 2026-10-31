package com.example.studentmanager.utils;

import android.content.Context;
import com.example.studentmanager.models.Session;

/** TODO An: guard dung tai repository. Stub luon tu choi, khong gia quyen truy cap. */
public class AuthGuard {
    public AuthGuard(Context context) {}
    public boolean requireAdmin() { return false; }
    public Session requireStudent() { return null; }
}
