package com.example.studentmanager.dal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.Subject;
import com.example.studentmanager.models.SubjectDraft;
import com.example.studentmanager.utils.AuthGuard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Sáng: gọi trên DbExecutor; Student chỉ đọc, Admin được quản lý môn. */
public class SubjectRepository {
    protected final Context context;
    protected final DBHelper dbHelper;
    protected final AuthGuard authGuard;

    public SubjectRepository(Context context) {
        this(context, DBHelper.getInstance(context), new AuthGuard(context));
    }

    // Chỉ dùng trong test với database in-memory và quyền giả của test.
    SubjectRepository(Context context, DBHelper dbHelper, AuthGuard authGuard) {
        this.context = context.getApplicationContext();
        this.dbHelper = dbHelper;
        this.authGuard = authGuard;
    }

    private boolean canRead() {
        return authGuard.requireAdmin() || authGuard.requireStudent() != null;
    }

    public List<Subject> list(String keyword) {
        if (!canRead()) return Collections.emptyList();
        String term = clean(keyword).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        List<Subject> subjects = new ArrayList<>();
        try (Cursor cursor = dbHelper.getReadableDatabase().query(DbContract.Subjects.TABLE, null,
                "subjectCode LIKE ? ESCAPE '\\' OR subjectName LIKE ? ESCAPE '\\'",
                new String[]{"%" + term + "%", "%" + term + "%"},
                null, null, "subjectCode COLLATE NOCASE, id")) {
            while (cursor.moveToNext()) subjects.add(read(cursor));
        }
        return subjects;
    }

    public Subject get(long id) {
        if (!canRead() || id <= 0) return null;
        try (Cursor cursor = dbHelper.getReadableDatabase().query(DbContract.Subjects.TABLE,
                null, "id=?", new String[]{Long.toString(id)}, null, null, null)) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }

    public ActionResult create(SubjectDraft draft) {
        if (!authGuard.requireAdmin()) return forbidden();
        return save(-1, draft);
    }

    public ActionResult update(long id, SubjectDraft draft) {
        if (!authGuard.requireAdmin()) return forbidden();
        if (id <= 0) return notFound();
        return save(id, draft);
    }

    private ActionResult save(long id, SubjectDraft draft) {
        if (draft == null || clean(draft.subjectCode).isEmpty()
                || clean(draft.subjectName).isEmpty() || draft.credits <= 0) {
            return ActionResult.error(ActionResult.VALIDATION,
                    "Nhập mã, tên môn và số tín chỉ nguyên dương.");
        }
        ContentValues values = new ContentValues();
        values.put(DbContract.Subjects.CODE, clean(draft.subjectCode).toUpperCase(Locale.ROOT));
        values.put(DbContract.Subjects.NAME, clean(draft.subjectName));
        values.put(DbContract.Subjects.DESCRIPTION, clean(draft.description));
        values.put(DbContract.Subjects.CREDITS, draft.credits);
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            if (id == -1) return ActionResult.success(db.insertOrThrow(DbContract.Subjects.TABLE, null, values));
            int changed = db.update(DbContract.Subjects.TABLE, values, "id=?", new String[]{Long.toString(id)});
            return changed == 1 ? ActionResult.success(id) : notFound();
        } catch (SQLiteConstraintException exception) {
            return ActionResult.error(ActionResult.DUPLICATE, "Mã môn học đã tồn tại.");
        } catch (SQLiteException exception) {
            return ActionResult.error(ActionResult.DB_ERROR, "Không thể lưu môn học. Vui lòng thử lại.");
        }
    }

    public ActionResult delete(long id) {
        if (!authGuard.requireAdmin()) return forbidden();
        if (id <= 0) return notFound();
        try {
            // RESTRICT bảo vệ cả lịch đã hủy và thao tác ghi đồng thời.
            int changed = dbHelper.getWritableDatabase().delete(DbContract.Subjects.TABLE,
                    "id=?", new String[]{Long.toString(id)});
            return changed == 1 ? ActionResult.success(id) : notFound();
        } catch (SQLiteConstraintException exception) {
            return ActionResult.error(ActionResult.IN_USE, "Môn đang có lịch học, không thể xóa.");
        } catch (SQLiteException exception) {
            return ActionResult.error(ActionResult.DB_ERROR, "Không thể xóa môn học. Vui lòng thử lại.");
        }
    }

    private static Subject read(Cursor cursor) {
        Subject subject = new Subject();
        subject.id = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.Subjects.ID));
        subject.subjectCode = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Subjects.CODE));
        subject.subjectName = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Subjects.NAME));
        subject.description = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Subjects.DESCRIPTION));
        subject.credits = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.Subjects.CREDITS));
        return subject;
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static ActionResult forbidden() {
        return ActionResult.error(ActionResult.FORBIDDEN, "Chỉ Admin được quản lý môn học.");
    }
    private static ActionResult notFound() {
        return ActionResult.error(ActionResult.NOT_FOUND, "Môn học không còn tồn tại.");
    }
}
