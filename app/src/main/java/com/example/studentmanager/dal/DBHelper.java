package com.example.studentmanager.dal;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.time.LocalDate;
import java.util.Locale;

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
        String studentPassword = "pbkdf2_sha256$600000$3e0811702098364cdf30b83b7da434d3$411e9eda26f39b2fe0eb5d428f95734d8d5f18286800bf4885b89ad7de856fd9";
        db.execSQL("INSERT INTO Account(id,username,password,role) VALUES(1,?,?,?)",
                new Object[]{"admin", "pbkdf2_sha256$600000$81c63b2b2bbdec35791a438969253ab6$d0b31912d211922fbfc88c7a471b9bd4c88fa08f8a5fc7d1c745ea6626fe8af7", DbContract.ADMIN});
        db.execSQL("INSERT INTO Account(id,username,password,role) VALUES(2,?,?,?)",
                new Object[]{"sv01", studentPassword, DbContract.STUDENT});
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
        seedMoreStudents(db, studentPassword);
        seedMoreSubjects(db);
        LocalDate today = LocalDate.now();
        insertDemoSchedule(db, 1, 1, today.plusDays(1), "09:00", "10:00", "Phòng A101", DbContract.PENDING);
        insertDemoSchedule(db, 2, 2, today.plusDays(1), "10:00", "11:00", "Phòng A102", DbContract.CONFIRMED);
        insertDemoSchedule(db, 1, 1, today, "09:00", "10:00", "Phòng A101", DbContract.PENDING);
        insertDemoSchedule(db, 1, 2, today, "14:00", "15:30", "Thư viện", DbContract.CONFIRMED);
        insertDemoSchedule(db, 1, 1, today, "16:00", "17:00", "Lịch đã hủy", DbContract.CANCELLED);
        // Hai sinh viên được có lịch cùng giờ để kiểm tra lọc theo chủ sở hữu.
        insertDemoSchedule(db, 2, 2, today, "09:00", "10:00", "Phòng B201", DbContract.PENDING);
        insertDemoSchedule(db, 1, 1, today.minusDays(1), "09:00", "10:00", "Lịch đã qua", DbContract.CONFIRMED);
        insertDemoSchedule(db, 1, 2, today.plusDays(3), "10:00", "11:00", "Phòng A102", DbContract.PENDING);
        // Hai lịch hiệu lực sát giờ; lịch hủy giao giờ vẫn được giữ để thử bộ lọc.
        insertDemoSchedule(db, 1, 1, today.plusDays(7), "14:00", "15:30", "Phòng A101", DbContract.CONFIRMED);
        insertDemoSchedule(db, 1, 2, today.plusDays(7), "15:30", "16:30", "Thư viện", DbContract.PENDING);
        insertDemoSchedule(db, 1, 2, today.plusDays(7), "16:00", "17:00", "Lịch đã hủy", DbContract.CANCELLED);
        insertDemoSchedule(db, 2, 1, today.plusDays(7), "14:00", "15:30", "Phòng B201", DbContract.CONFIRMED);
        insertDemoSchedule(db, 1, 2, today.withDayOfMonth(1).minusDays(1), "08:00", "09:00", "Tháng trước", DbContract.CONFIRMED);
        insertDemoSchedule(db, 1, 1, today.withDayOfMonth(1).plusMonths(1), "11:00", "12:00", "Tháng sau", DbContract.PENDING);
        // GIT101 không có lịch: dùng để kiểm tra xóa môn chưa được sử dụng.
        seedMoreSchedules(db, today);
    }

    private void seedMoreStudents(SQLiteDatabase db, String passwordHash) {
        String[] names = {
                "Nguyễn Minh Anh", "Trần Hoàng Bảo", "Lê Ngọc Chi", "Phạm Quốc Dũng",
                "Hoàng Thùy Dương", "Vũ Minh Đức", "Đặng Gia Hân", "Bùi Thanh Hải",
                "Đỗ Khánh Hòa", "Hồ Quang Huy", "Ngô Bảo Khánh", "Dương Mỹ Linh",
                "Lý Nhật Minh", "Mai Phương Nam", "Phan Thảo Nguyên", "Võ Kim Ngọc",
                "Nguyễn Hoàng Phúc", "Trần Minh Quân", "Lê Hồng Sơn", "Phạm Thanh Tâm",
                "Hoàng Bảo Trân", "Vũ Anh Tuấn", "Đặng Tường Vy", "Bùi Hải Yến",
                "Đỗ Thị Nguyễn Minh Châu", "Hồ Lê Quang Trường", "Ngô Trúc Giang", "Dương Quốc Việt"
        };
        for (int i = 0; i < names.length; i++) {
            int studentId = i + 3;
            int accountId = studentId + 1;
            String username = String.format(Locale.ROOT, "sv%02d", studentId);
            db.execSQL("INSERT INTO Account(id,username,password,role) VALUES(?,?,?,?)",
                    new Object[]{accountId, username, passwordHash, DbContract.STUDENT});
            db.execSQL("INSERT INTO Student(id,studentCode,fullname,email,phone,accountId) VALUES(?,?,?,?,?,?)",
                    new Object[]{studentId, String.format(Locale.ROOT, "SV%03d", studentId), names[i],
                            username + "@example.com", String.format(Locale.ROOT, "090000%04d", studentId), accountId});
        }
    }

    private void seedMoreSubjects(SQLiteDatabase db) {
        String[][] subjects = {
                {"JAVA101", "Lập trình Java"}, {"WEB101", "Thiết kế web"},
                {"PY101", "Lập trình Python"}, {"DSA101", "Cấu trúc dữ liệu và giải thuật"},
                {"NET101", "Mạng máy tính"}, {"OS101", "Hệ điều hành"},
                {"SE101", "Công nghệ phần mềm"}, {"TEST101", "Kiểm thử phần mềm"},
                {"UI101", "Thiết kế giao diện người dùng"}, {"UX101", "Trải nghiệm người dùng"},
                {"AI101", "Trí tuệ nhân tạo"}, {"ML101", "Học máy"},
                {"SEC101", "An toàn thông tin"}, {"CLOUD101", "Điện toán đám mây"},
                {"DEVOPS101", "Triển khai và vận hành phần mềm"}, {"SQL201", "Cơ sở dữ liệu nâng cao"},
                {"API101", "Xây dựng dịch vụ API"}, {"LINUX101", "Quản trị Linux"},
                {"MATH101", "Toán rời rạc"}, {"STAT101", "Xác suất thống kê"},
                {"ENG101", "Tiếng Anh chuyên ngành"}, {"PM101", "Quản lý dự án phần mềm"},
                {"IOT101", "Internet vạn vật"}, {"GAME101", "Lập trình trò chơi"},
                {"DES101", "Phân tích và thiết kế hệ thống"}, {"PROJ101", "Đồ án phát triển ứng dụng"},
                {"RES101", "Phương pháp nghiên cứu"}
        };
        for (int i = 0; i < subjects.length; i++) {
            int id = i + 4;
            String description = i % 4 == 0 ? "" : "Môn học mẫu: " + subjects[i][1]
                    + ". Dùng để thử tìm kiếm, danh sách dài và nội dung xuống dòng.";
            db.execSQL("INSERT INTO Subject(id,subjectCode,subjectName,description,credits) VALUES(?,?,?,?,?)",
                    new Object[]{id, subjects[i][0], subjects[i][1], description, 2 + i % 3});
        }
    }

    private void seedMoreSchedules(SQLiteDatabase db, LocalDate today) {
        String[] locations = {"Phòng A101", "Phòng A102", "Phòng B201", "Phòng B202",
                "Phòng máy C301", "Thư viện", "Trực tuyến"};
        String[] statuses = {DbContract.CANCELLED, DbContract.PENDING, DbContract.PENDING,
                DbContract.CONFIRMED, DbContract.CONFIRMED};
        for (int studentId = 1; studentId <= 30; studentId++) {
            // Mỗi ba ngày có hai lịch, từ 90 ngày trước đến 90 ngày sau.
            for (int offset = -90; offset <= 90; offset += 3) {
                // Giữ nguyên ca hôm nay của sv01/sv02 để dễ đối chiếu demo.
                if (studentId <= 2 && offset == 0) continue;
                for (int slot = 0; slot < 2; slot++) {
                    int key = studentId + offset / 3 + slot;
                    int subjectId = 4 + Math.floorMod(key, 24);
                    insertDemoSchedule(db, studentId, subjectId, today.plusDays(offset),
                            slot == 0 ? "07:00" : "18:30", slot == 0 ? "08:00" : "20:00",
                            locations[Math.floorMod(key, locations.length)],
                            statuses[Math.floorMod(key, statuses.length)]);
                }
            }
        }
        // 3.656 lịch sinh thêm + 14 ca cố định = 3.670 lịch.
        // GIT101, DES101, PROJ101, RES101 được để trống để thử xóa môn.
    }

    private void insertDemoSchedule(SQLiteDatabase db, long studentId, long subjectId,
                                    LocalDate date, String start, String end, String location, String status) {
        db.execSQL("INSERT INTO StudySchedule(studentId,subjectId,studyDate,startTime,endTime,location,status)"
                        + " VALUES(?,?,?,?,?,?,?)",
                new Object[]{studentId, subjectId, date.toString(), start, end, location, status});
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Can viet migration tu " + oldVersion + " den " + newVersion);
    }
}
