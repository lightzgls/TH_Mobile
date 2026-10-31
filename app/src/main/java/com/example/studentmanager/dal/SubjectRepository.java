package com.example.studentmanager.dal;

import android.content.Context;
import com.example.studentmanager.models.*;
import com.example.studentmanager.utils.AuthGuard;
import java.util.Collections;
import java.util.List;

/**
 * TODO Sáng: trien khai chuc nang; hien tai chi la khung chu ky ham.
 * Goi tren DbExecutor, kiem tra quyen truoc khi doc/ghi; dung SQL bind parameters.
 */
public class SubjectRepository {
    protected final Context context;
    protected final DBHelper dbHelper;
    protected final AuthGuard authGuard;

    public SubjectRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = DBHelper.getInstance(context);
        this.authGuard = new AuthGuard(context);
    }

    public List<Subject> list(String keyword) { return Collections.emptyList(); }
    public Subject get(long id) { return null; }
    public ActionResult create(SubjectDraft draft) { return ActionResult.notImplemented(); }
    public ActionResult update(long id, SubjectDraft draft) { return ActionResult.notImplemented(); }
    public ActionResult delete(long id) { return ActionResult.notImplemented(); }
}
