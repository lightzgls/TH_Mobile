package com.example.studentmanager.dal;

import android.content.Context;
import com.example.studentmanager.models.*;
import com.example.studentmanager.utils.AuthGuard;
import java.util.Collections;
import java.util.List;

/**
 * TODO An: trien khai chuc nang; hien tai chi la khung chu ky ham.
 * Goi tren DbExecutor, kiem tra quyen truoc khi doc/ghi; dung SQL bind parameters.
 */
public class AuthRepository {
    protected final Context context;
    protected final DBHelper dbHelper;
    protected final AuthGuard authGuard;

    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = DBHelper.getInstance(context);
        this.authGuard = new AuthGuard(context);
    }

    public ActionResult login(String username, String password) { return ActionResult.notImplemented(); }
    public ActionResult logout() { return ActionResult.notImplemented(); }
}
