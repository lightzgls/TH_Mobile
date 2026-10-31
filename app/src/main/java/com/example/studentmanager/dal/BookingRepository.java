package com.example.studentmanager.dal;

import android.content.Context;
import com.example.studentmanager.models.*;
import com.example.studentmanager.utils.AuthGuard;
import java.util.Collections;
import java.util.List;

/**
 * TODO Thu: trien khai chuc nang; hien tai chi la khung chu ky ham.
 * Goi tren DbExecutor, kiem tra quyen truoc khi doc/ghi; dung SQL bind parameters.
 */
public class BookingRepository {
    protected final Context context;
    protected final DBHelper dbHelper;
    protected final AuthGuard authGuard;

    public BookingRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = DBHelper.getInstance(context);
        this.authGuard = new AuthGuard(context);
    }

    // scheduleId = -1 khi tao; studentId lay tu AuthGuard, khong lay tu draft hay Intent.
    public ActionResult save(long scheduleId, BookingDraft draft) { return ActionResult.notImplemented(); }
}
