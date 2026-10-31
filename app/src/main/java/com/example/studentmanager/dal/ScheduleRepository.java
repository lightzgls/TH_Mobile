package com.example.studentmanager.dal;

import android.content.Context;
import com.example.studentmanager.models.*;
import com.example.studentmanager.utils.AuthGuard;
import java.util.Collections;
import java.util.List;

/**
 * TODO Huy: trien khai chuc nang; hien tai chi la khung chu ky ham.
 * Goi tren DbExecutor, kiem tra quyen truoc khi doc/ghi; dung SQL bind parameters.
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

    public List<ScheduleItem> listMine(ScheduleFilter filter) { return Collections.emptyList(); }
    public ScheduleItem getMine(long id) { return null; }
    public ActionResult setStatus(long id, String status) { return ActionResult.notImplemented(); }
    public ActionResult deleteMine(long id) { return ActionResult.notImplemented(); }
}
