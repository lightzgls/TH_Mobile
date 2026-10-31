package com.example.studentmanager.dal;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.time.LocalDate;

/** Khung chung: schema SQLite va du lieu demo, khong chua CRUD cua thanh vien. */
public final class DBHelper extends SQLiteOpenHelper {
    private static volatile DBHelper instance;

    public static DBHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (DBHelper.class) {
                if (instance == null) {
                    instance = new DBHelper(context.getApplicationContext(), DbContract.DATABASE_NAME);
                }
            }
        }
        return instance;
    }

    // Package access de test dung database in-memory, khong xoa du lieu nguoi dung.
    DBHelper(Context context, String databaseName) {
        super(context, databaseName, null, DbContract.DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Account ( id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL COLLATE NOCASE UNIQUE, password TEXT NOT NULL, role TEXT NOT NULL CHECK(role IN ('ADMIN','STUDENT')) )");
        db.execSQL("CREATE TABLE Student ( id INTEGER PRIMARY KEY AUTOINCREMENT, studentCode TEXT NOT NULL COLLATE NOCASE UNIQUE, fullname TEXT NOT NULL, email TEXT NOT NULL, phone TEXT NOT NULL, accountId INTEGER NOT NULL UNIQUE, FOREIGN KEY(accountId) REFERENCES Account(id) ON DELETE CASCADE )");
        db.execSQL("CREATE TABLE Subject ( id INTEGER PRIMARY KEY AUTOINCREMENT, subjectCode TEXT NOT NULL COLLATE NOCASE UNIQUE, subjectName TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', credits INTEGER NOT NULL CHECK(credits > 0) )");
        db.execSQL("CREATE TABLE StudySchedule ( id INTEGER PRIMARY KEY AUTOINCREMENT, studentId INTEGER NOT NULL, subjectId INTEGER NOT NULL, studyDate TEXT NOT NULL, startTime TEXT NOT NULL, endTime TEXT NOT NULL, location TEXT NOT NULL, status TEXT NOT NULL CHECK(status IN ('PENDING','CONFIRMED','CANCELLED')), CHECK(length(studyDate) = 10), CHECK(length(startTime) = 5 AND length(endTime) = 5 AND startTime < endTime), FOREIGN KEY(studentId) REFERENCES Student(id) ON DELETE CASCADE, FOREIGN KEY(subjectId) REFERENCES Subject(id) ON DELETE RESTRICT )");
        db.execSQL("CREATE INDEX idx_schedule_student_date ON StudySchedule(studentId, studyDate)");
        seedDemo(db);
    }

    private void seedDemo(SQLiteDatabase db) {
        // Chi chay khi tao database lan dau. Tai khoan nay chi dung cho bai demo.
        // Format password: pbkdf2_sha256$iterations$saltHex$hashHex (UTF-8, 256 bit).
        db.execSQL("INSERT INTO Account(id,username,password,role) VALUES(1,?,?,?)",
                new Object[]{"admin", "pbkdf2_sha256$600000$81c63b2b2bbdec35791a438969253ab6$d0b31912d211922fbfc88c7a471b9bd4c88fa08f8a5fc7d1c745ea6626fe8af7", DbContract.ADMIN});
        db.execSQL("INSERT INTO Account(id,username,password,role) VALUES(2,?,?,?)",
                new Object[]{"sv01", "pbkdf2_sha256$600000$3e0811702098364cdf30b83b7da434d3$411e9eda26f39b2fe0eb5d428f95734d8d5f18286800bf4885b89ad7de856fd9", DbContract.STUDENT});
        db.execSQL("INSERT INTO Account(id,username,password,role) VALUES(3,?,?,?)",
                new Object[]{"sv02", "pbkdf2_sha256$600000$7b605951225c43d24cc8f881b4677131$7bc73c45f8fb63730e952d6255139d49542b2f01534ea11b4782ee892662585b", DbContract.STUDENT});
        db.execSQL("INSERT INTO Student(id,studentCode,fullname,email,phone,accountId) VALUES(1,?,?,?,?,2)",
                new Object[]{"SV001", "Sinh viên Một", "sv01@example.com", "0900000001"});
        db.execSQL("INSERT INTO Student(id,studentCode,fullname,email,phone,accountId) VALUES(2,?,?,?,?,3)",
                new Object[]{"SV002", "Sinh viên Hai", "sv02@example.com", "0900000002"});
        db.execSQL("INSERT INTO Subject(id,subjectCode,subjectName,description,credits) VALUES(1,?,?,?,3)",
                new Object[]{"MOB101", "Lập trình di động", "Dữ liệu demo"});
        db.execSQL("INSERT INTO Subject(id,subjectCode,subjectName,description,credits) VALUES(2,?,?,?,3)",
                new Object[]{"DB101", "Cơ sở dữ liệu", "Dữ liệu demo"});
        db.execSQL("INSERT INTO Subject(id,subjectCode,subjectName,description,credits) VALUES(3,?,?,?,2)",
                new Object[]{"GIT101", "Làm việc nhóm với Git", "Dữ liệu demo"});
        String tomorrow = LocalDate.now().plusDays(1).toString();
        db.execSQL("INSERT INTO StudySchedule(studentId,subjectId,studyDate,startTime,endTime,location,status) "
                        + "VALUES(1,1,?,'09:00','10:00',?,?)",
                new Object[]{tomorrow, "Phòng A101", DbContract.PENDING});
        db.execSQL("INSERT INTO StudySchedule(studentId,subjectId,studyDate,startTime,endTime,location,status) "
                        + "VALUES(2,2,?,'10:00','11:00',?,?)",
                new Object[]{tomorrow, "Phòng A102", DbContract.CONFIRMED});
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Can viet migration tu " + oldVersion + " den " + newVersion);
    }
}
