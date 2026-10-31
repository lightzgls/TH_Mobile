package com.example.studentmanager.dal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.Student;
import com.example.studentmanager.models.StudentDraft;
import com.example.studentmanager.notifications.NotificationHelper;
import com.example.studentmanager.utils.AuthGuard;
import com.example.studentmanager.utils.PasswordHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Các thao tác quản lý sinh viên. Gọi các phương thức từ DbExecutor để không
 * thực hiện truy vấn hoặc ghi SQLite trên main thread.
 */
public class StudentRepository {
    private static final String TAG = "StudentRepository";

    protected final Context context;
    protected final DBHelper dbHelper;
    protected final AuthGuard authGuard;

    public StudentRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = DBHelper.getInstance(context);
        this.authGuard = new AuthGuard(context);
    }

    /** Tìm theo mã, họ tên hoặc email; chuỗi rỗng trả về toàn bộ danh sách. */
    public List<Student> list(String keyword) {
        List<Student> students = new ArrayList<>();
        if (!authGuard.requireAdmin()) return students;

        String value = keyword == null ? "" : keyword.trim();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = null;
        String[] selectionArgs = null;
        if (!value.isEmpty()) {
            selection = DbContract.Students.CODE + " LIKE ? OR "
                    + DbContract.Students.FULLNAME + " LIKE ? OR "
                    + DbContract.Students.EMAIL + " LIKE ?";
            String pattern = "%" + value + "%";
            selectionArgs = new String[]{pattern, pattern, pattern};
        }

        try (Cursor cursor = db.query(
                DbContract.Students.TABLE,
                new String[]{DbContract.Students.ID, DbContract.Students.CODE,
                        DbContract.Students.FULLNAME, DbContract.Students.EMAIL,
                        DbContract.Students.PHONE, DbContract.Students.ACCOUNT_ID},
                selection, selectionArgs, null, null,
                DbContract.Students.FULLNAME + " COLLATE NOCASE ASC")) {
            while (cursor.moveToNext()) students.add(readStudent(cursor));
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to load students", error);
        }
        return students;
    }

    public Student get(long id) {
        if (!authGuard.requireAdmin() || id <= 0) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.Students.TABLE,
                new String[]{DbContract.Students.ID, DbContract.Students.CODE,
                        DbContract.Students.FULLNAME, DbContract.Students.EMAIL,
                        DbContract.Students.PHONE, DbContract.Students.ACCOUNT_ID},
                DbContract.Students.ID + "=?", new String[]{String.valueOf(id)},
                null, null, null)) {
            return cursor.moveToFirst() ? readStudent(cursor) : null;
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to load student " + id, error);
            return null;
        }
    }

    /** Tạo Account role STUDENT và Student trong cùng một transaction. */
    public ActionResult create(StudentDraft draft) {
        if (!authGuard.requireAdmin()) {
            return ActionResult.error(ActionResult.FORBIDDEN, "Chỉ Admin được quản lý sinh viên.");
        }
        String validation = validate(draft, true);
        if (validation != null) return ActionResult.error(ActionResult.VALIDATION, validation);

        String studentCode = normalizeCode(draft.studentCode);
        String username = normalizeUsername(draft.username);
        String fullname = clean(draft.fullname);
        String email = clean(draft.email);
        String phone = clean(draft.phone);

        // Kiểm tra sớm để phản hồi lỗi trùng mà không phải chạy PBKDF2 tốn thời gian.
        // Vẫn kiểm tra lại bên trong transaction để bảo vệ trước các thao tác đồng thời.
        try {
            SQLiteDatabase readable = dbHelper.getReadableDatabase();
            if (exists(readable, DbContract.Students.TABLE, DbContract.Students.CODE,
                    studentCode, -1)) {
                return ActionResult.error(ActionResult.DUPLICATE, "Mã sinh viên đã tồn tại.");
            }
            if (exists(readable, DbContract.Accounts.TABLE, DbContract.Accounts.USERNAME,
                    username, -1)) {
                return ActionResult.error(ActionResult.DUPLICATE, "Username đã tồn tại.");
            }
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to validate unique student fields", error);
            return ActionResult.error(ActionResult.DB_ERROR, "Không thể kiểm tra dữ liệu sinh viên.");
        }

        // Hash trước transaction để không giữ khóa ghi SQLite trong lúc PBKDF2 chạy.
        final String passwordHash;
        try {
            passwordHash = PasswordHelper.hash(draft.password);
            if (passwordHash == null || passwordHash.trim().isEmpty()) {
                return ActionResult.error(ActionResult.DB_ERROR,
                        "Không tạo được mật khẩu bảo mật. Kiểm tra PasswordHelper của phần đăng nhập.");
            }
        } catch (RuntimeException error) {
            Log.e(TAG, "Password hashing is not available", error);
            return ActionResult.error(ActionResult.DB_ERROR,
                    "Chức năng mã hóa mật khẩu chưa sẵn sàng. Cần tích hợp PasswordHelper trước khi tạo tài khoản.");
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long studentId = -1;
        db.beginTransaction();
        try {
            if (exists(db, DbContract.Students.TABLE, DbContract.Students.CODE,
                    studentCode, -1)) {
                return ActionResult.error(ActionResult.DUPLICATE, "Mã sinh viên đã tồn tại.");
            }
            if (exists(db, DbContract.Accounts.TABLE, DbContract.Accounts.USERNAME,
                    username, -1)) {
                return ActionResult.error(ActionResult.DUPLICATE, "Username đã tồn tại.");
            }

            ContentValues account = new ContentValues();
            account.put(DbContract.Accounts.USERNAME, username);
            account.put(DbContract.Accounts.PASSWORD, passwordHash);
            account.put(DbContract.Accounts.ROLE, DbContract.STUDENT);
            long accountId = db.insertOrThrow(DbContract.Accounts.TABLE, null, account);

            ContentValues student = new ContentValues();
            student.put(DbContract.Students.CODE, studentCode);
            student.put(DbContract.Students.FULLNAME, fullname);
            student.put(DbContract.Students.EMAIL, email);
            student.put(DbContract.Students.PHONE, phone);
            student.put(DbContract.Students.ACCOUNT_ID, accountId);
            studentId = db.insertOrThrow(DbContract.Students.TABLE, null, student);

            db.setTransactionSuccessful();
        } catch (SQLiteConstraintException error) {
            Log.w(TAG, "Duplicate or invalid student data", error);
            return ActionResult.error(ActionResult.DUPLICATE,
                    "Mã sinh viên hoặc username đã tồn tại.");
        } catch (SQLException | IllegalStateException error) {
            Log.e(TAG, "Unable to create student", error);
            return ActionResult.error(ActionResult.DB_ERROR, "Không thể tạo sinh viên. Vui lòng thử lại.");
        } finally {
            db.endTransaction();
        }
        return ActionResult.success(studentId);
    }

    /** Cập nhật hồ sơ; accountId và username luôn được giữ nguyên. */
    public ActionResult update(long id, StudentDraft draft) {
        if (!authGuard.requireAdmin()) {
            return ActionResult.error(ActionResult.FORBIDDEN, "Chỉ Admin được quản lý sinh viên.");
        }
        if (id <= 0) return ActionResult.error(ActionResult.VALIDATION, "Sinh viên không hợp lệ.");
        String validation = validate(draft, false);
        if (validation != null) return ActionResult.error(ActionResult.VALIDATION, validation);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            if (!studentExists(db, id)) {
                return ActionResult.error(ActionResult.NOT_FOUND, "Không tìm thấy sinh viên cần sửa.");
            }
            String studentCode = normalizeCode(draft.studentCode);
            if (exists(db, DbContract.Students.TABLE, DbContract.Students.CODE,
                    studentCode, id)) {
                return ActionResult.error(ActionResult.DUPLICATE, "Mã sinh viên đã được sử dụng.");
            }

            ContentValues values = new ContentValues();
            values.put(DbContract.Students.CODE, studentCode);
            values.put(DbContract.Students.FULLNAME, clean(draft.fullname));
            values.put(DbContract.Students.EMAIL, clean(draft.email));
            values.put(DbContract.Students.PHONE, clean(draft.phone));
            int changed = db.update(DbContract.Students.TABLE, values,
                    DbContract.Students.ID + "=?", new String[]{String.valueOf(id)});
            if (changed != 1) {
                return ActionResult.error(ActionResult.NOT_FOUND, "Không tìm thấy sinh viên cần sửa.");
            }
            db.setTransactionSuccessful();
        } catch (SQLiteConstraintException error) {
            Log.w(TAG, "Duplicate student code", error);
            return ActionResult.error(ActionResult.DUPLICATE, "Mã sinh viên đã được sử dụng.");
        } catch (SQLException | IllegalStateException error) {
            Log.e(TAG, "Unable to update student " + id, error);
            return ActionResult.error(ActionResult.DB_ERROR, "Không thể cập nhật sinh viên. Vui lòng thử lại.");
        } finally {
            db.endTransaction();
        }
        return ActionResult.success(id);
    }

    /** Số lịch bị ảnh hưởng, dùng để hiển thị chính xác trong hộp thoại xác nhận. */
    public int countSchedules(long studentId) {
        if (!authGuard.requireAdmin() || studentId <= 0) return -1;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DbContract.Schedules.TABLE + " WHERE "
                        + DbContract.Schedules.STUDENT_ID + "=?",
                new String[]{String.valueOf(studentId)})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to count schedules for student " + studentId, error);
            return -1;
        }
    }

    /** Xóa đúng Account có role STUDENT; khóa ngoại sẽ xóa Student và StudySchedule. */
    public ActionResult delete(long id) {
        if (!authGuard.requireAdmin()) {
            return ActionResult.error(ActionResult.FORBIDDEN, "Chỉ Admin được quản lý sinh viên.");
        }
        if (id <= 0) return ActionResult.error(ActionResult.VALIDATION, "Sinh viên không hợp lệ.");

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            long accountId;
            try (Cursor cursor = db.query(
                    DbContract.Students.TABLE,
                    new String[]{DbContract.Students.ACCOUNT_ID},
                    DbContract.Students.ID + "=?", new String[]{String.valueOf(id)},
                    null, null, null)) {
                if (!cursor.moveToFirst()) {
                    return ActionResult.error(ActionResult.NOT_FOUND, "Sinh viên không còn tồn tại.");
                }
                accountId = cursor.getLong(0);
            }

            // Không bao giờ xóa tài khoản ADMIN, kể cả khi dữ liệu tham chiếu bị sai.
            try (Cursor cursor = db.query(
                    DbContract.Accounts.TABLE,
                    new String[]{DbContract.Accounts.ROLE},
                    DbContract.Accounts.ID + "=?", new String[]{String.valueOf(accountId)},
                    null, null, null)) {
                if (!cursor.moveToFirst() || !DbContract.STUDENT.equals(cursor.getString(0))) {
                    return ActionResult.error(ActionResult.FORBIDDEN,
                            "Không thể xóa tài khoản không thuộc vai trò Student.");
                }
            }

            int deleted = db.delete(DbContract.Accounts.TABLE,
                    DbContract.Accounts.ID + "=? AND " + DbContract.Accounts.ROLE + "=?",
                    new String[]{String.valueOf(accountId), DbContract.STUDENT});
            if (deleted != 1) {
                return ActionResult.error(ActionResult.DB_ERROR, "Không thể xóa tài khoản sinh viên.");
            }
            db.setTransactionSuccessful();
        } catch (SQLException | IllegalStateException error) {
            Log.e(TAG, "Unable to delete student " + id, error);
            return ActionResult.error(ActionResult.DB_ERROR, "Không thể xóa sinh viên. Vui lòng thử lại.");
        } finally {
            db.endTransaction();
        }

        // Cập nhật lại thông báo sau khi transaction xóa và cascade đã commit.
        // Lỗi ở phần thông báo không được làm ứng dụng hiểu nhầm rằng xóa DB thất bại.
        try {
            NotificationHelper.refresh(context);
        } catch (RuntimeException error) {
            Log.w(TAG, "Student was deleted, but schedule notifications could not be refreshed", error);
        }
        return ActionResult.success(id);
    }

    private static Student readStudent(Cursor cursor) {
        Student student = new Student();
        student.id = cursor.getLong(0);
        student.studentCode = cursor.getString(1);
        student.fullname = cursor.getString(2);
        student.email = cursor.getString(3);
        student.phone = cursor.getString(4);
        student.accountId = cursor.getLong(5);
        return student;
    }

    private static boolean studentExists(SQLiteDatabase db, long id) {
        try (Cursor cursor = db.query(DbContract.Students.TABLE,
                new String[]{DbContract.Students.ID}, DbContract.Students.ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return cursor.moveToFirst();
        }
    }

    /** excludeId=-1 có nghĩa là không loại trừ bản ghi nào. */
    private static boolean exists(SQLiteDatabase db, String table, String column,
                                  String value, long excludeId) {
        String selection = column + "=?";
        String[] args;
        if (excludeId > 0) {
            selection += " AND id<>?";
            args = new String[]{value, String.valueOf(excludeId)};
        } else {
            args = new String[]{value};
        }
        try (Cursor cursor = db.query(table, new String[]{"id"}, selection,
                args, null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    private static String validate(StudentDraft draft, boolean creating) {
        if (draft == null) return "Dữ liệu sinh viên không hợp lệ.";
        if (clean(draft.studentCode).isEmpty()) return "Vui lòng nhập mã sinh viên.";
        if (clean(draft.fullname).isEmpty()) return "Vui lòng nhập họ và tên.";
        if (clean(draft.email).isEmpty()) return "Vui lòng nhập email.";
        if (clean(draft.phone).isEmpty()) return "Vui lòng nhập số điện thoại.";
        if (creating && clean(draft.username).isEmpty()) return "Vui lòng nhập username.";
        if (creating && normalizeUsername(draft.username).length() > 100) {
            return "Tài khoản tối đa 100 ký tự.";
        }
        if (creating && (draft.password == null || draft.password.trim().isEmpty())) {
            return "Vui lòng nhập mật khẩu.";
        }
        if (creating && draft.password.length() > 256) return "Mật khẩu tối đa 256 ký tự.";
        return null;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeCode(String value) {
        return clean(value).toUpperCase(Locale.ROOT);
    }

    private static String normalizeUsername(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }
}
