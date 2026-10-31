# TH Mobile

Ứng dụng Android quản lý sinh viên, môn học và đặt lịch học. Nhóm **Sáng, An, Thu, Quang, Huy**; Sáng là trưởng nhóm.

## Mở bằng Android Studio

1. Clone repository hoặc tải ZIP và giải nén.
2. Chọn **Open → thư mục TH_Mobile** chứa `settings.gradle`.
3. Đợi **Gradle Sync** hoàn tất; chấp nhận cài SDK nếu được đề nghị.
4. Chọn module **app**, chọn emulator/điện thoại Android 8.0 trở lên, bấm **Run ▶**.

Đã có Gradle Wrapper, không cần cài Gradle riêng. Lần Sync đầu cần Internet. Android Studio tự tạo `local.properties` cho từng máy; không commit file này. Nếu được hỏi Gradle JDK, chọn JDK đi kèm Android Studio hoặc JDK 21. Dùng Android Studio hỗ trợ AGP 9.2; máy dựng khung đang dùng bản 2026.1.4.

| Công cụ | Phiên bản |
| --- | --- |
| Ngôn ngữ và UI | Java source 17 và XML |
| Database | SQLite qua SQLiteOpenHelper, không dùng Room |
| Android Gradle Plugin / Gradle | 9.2.0 / 9.4.1 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 |
| SDK Platform / Build Tools | Android 17 API 37.0 / 36.0.0 |
| AndroidX Activity / WorkManager | 1.10.1 / 2.10.1 |

## Test nhanh với dữ liệu mẫu

1. Lấy `main` mới nhất, mở Android Studio và chọn build variant **debug**.
2. Chọn cấu hình **Sang UI Demo**, chọn emulator rồi bấm **Run ▶**.
3. Thử quản lý môn, chọn môn và lịch tháng ngay với dữ liệu mẫu. Nếu chưa thấy cấu hình Run, chạy **app** rồi mở icon **Sáng UI Demo** trên thiết bị.
4. Bấm **Tạo lại dữ liệu demo** để lấy bộ mẫu mới nếu đã cài demo từ trước.

Bộ mẫu có **1 Admin, 30 Student, 30 môn và 3.670 lịch**, gồm lịch hôm nay, ngày mai, đã qua, sát giờ và ở tháng trước/sau. GIT101 chưa có lịch để thử xóa; MOB101 và DB101 có lịch để thử chặn xóa. Xem [bộ dữ liệu và cách thử cho cả nhóm](docs/SAMPLE_DATA.md).

## Trạng thái hiện tại

Đã có cấu hình project, 4 bảng SQLite, khóa ngoại, seed demo, model, chữ ký hàm, Activity và layout riêng. Trang mở đầu có nút xem màn hình rỗng Admin và Student **chỉ trong bản debug**, không tạo phiên hay cấp quyền.

**Phần Sáng đã có CRUD môn, chọn môn và lịch tháng**, kèm demo visual trong `src/debug` dùng database riêng. Luồng chính còn chờ ghép đăng nhập của An, truy vấn lịch của Huy và form đặt/sửa của Thu; phần chưa triển khai vẫn giữ stub và từ chối quyền. Các nhánh cá nhân khác cần được kiểm tra, review và ghép riêng. Khung chung và việc điều phối không tính đóng góp cá nhân.

Xem [kế hoạch đầy đủ](docs/IMPLEMENTATION_PLAN.md) và [bản Word](docs/Ke_hoach_nhom_5_nguoi.docx).

## Cấu trúc theo mẫu

Java nằm ở `app/src/main/java/com/example/studentmanager/`. Chọn chế độ **Project** trong Android Studio để xem cây thư mục vật lý.

```text
com.example.studentmanager
│
├── activities
│   ├── BaseActivity.java                  [Khung chung]
│   ├── AdminHomeActivity.java             [Khung chung]
│   ├── StudentHomeActivity.java           [Khung chung]
│   ├── LoginActivity.java                 [An]
│   ├── StudentListActivity.java           [Quang]
│   ├── StudentFormActivity.java           [Quang]
│   ├── StudentDetailActivity.java         [Quang]
│   ├── SubjectListActivity.java           [Sáng]
│   ├── SubjectFormActivity.java           [Sáng]
│   ├── SubjectPickerActivity.java         [Sáng]
│   ├── MonthCalendarActivity.java         [Sáng]
│   ├── BookingFormActivity.java           [Thu]
│   ├── ScheduleListActivity.java          [Huy]
│   ├── ScheduleDetailActivity.java        [Huy]
│   └── DayWeekCalendarActivity.java       [Huy]
│
├── dal
│   ├── DBHelper.java                      [Khung chung]
│   ├── DbContract.java                    [Khung chung]
│   ├── AuthRepository.java                [An]
│   ├── StudentRepository.java             [Quang]
│   ├── SubjectRepository.java             [Sáng]
│   ├── BookingRepository.java             [Thu]
│   └── ScheduleRepository.java            [Huy]
│
├── models
│   ├── Account.java
│   ├── Student.java
│   ├── Subject.java
│   ├── StudySchedule.java
│   ├── Session.java
│   ├── StudentDraft.java
│   ├── SubjectDraft.java
│   ├── BookingDraft.java
│   ├── ScheduleFilter.java
│   ├── ScheduleItem.java
│   └── ActionResult.java
│
├── utils
│   ├── AppConstants.java                  [Khung chung]
│   ├── DateTimeHelper.java                [Khung chung]
│   ├── DbExecutor.java                    [Khung chung]
│   ├── SessionManager.java                [An]
│   ├── PasswordHelper.java                [An]
│   ├── AuthGuard.java                     [An]
│   └── BookingValidator.java              [Thu]
│
├── notifications
│   ├── NotificationHelper.java            [An]
│   ├── ReminderWorker.java                [An]
│   └── TimeChangeReceiver.java            [An]
│
└── MainActivity.java                      [Khung chung]
```

Chỉ 4 model đầu tương ứng 4 bảng; Session, Draft, Filter, Result và ScheduleItem là dữ liệu truyền qua hàm.

## Phân công đã random

Mỗi người **20 đơn vị chức năng dự kiến**, gồm code, kiểm thử và demo. Mỗi người chỉ sửa Java ghi tên mình trong cây trên, test riêng và các resource tương ứng:

| Người | Chức năng | Resource riêng |
| --- | --- | --- |
| An | Login, vai trò, nhớ phiên, thông báo | `auth_*`, `notify_*`, `strings_auth.xml`, `strings_notify.xml` |
| Quang | CRUD sinh viên và tài khoản đi kèm | `activity_student_list/form/detail.xml`, `student_list/form/detail_*`, `strings_student.xml` |
| Sáng | CRUD môn, chọn môn, lịch tháng | `subject_*`, `month_*`, `strings_subject.xml`, `strings_month.xml` |
| Thu | Đặt, sửa, chống trùng lịch | `booking_*`, `strings_booking.xml` |
| Huy | Danh sách, tìm kiếm, chi tiết, trạng thái, xóa lịch, ngày tuần | `schedule_*`, `agenda_*`, `strings_schedule.xml`, `strings_agenda.xml` |

Tên layout có dạng `activity_auth_login.xml`, `activity_student_list.xml`… File chung gồm Manifest, Gradle, DBHelper, DbContract, models, MainActivity, BaseActivity, hai HomeActivity và tiện ích ghi Khung chung. Khi cần đổi, thống nhất để Sáng gom PR riêng. Không cùng sửa `strings.xml` hay tự tạo lại project.

## Điểm nối dùng chung

- Repository nhận `Context`. Gọi database qua `DbExecutor.execute(...)`; dùng `runOnUiThread` để cập nhật UI.
- Hàm ghi/login trả `ActionResult(ok, code, message, id)` qua factory có sẵn; lỗi có `id=-1`.
- `AuthGuard.requireAdmin()` trả boolean; `requireStudent()` trả Session hợp lệ hoặc null. An triển khai; repository kiểm tra quyền trước khi đọc/ghi.
- `BookingRepository.save(-1, draft)` tạo mới; id thật để sửa. Lấy studentId từ phiên, không từ Intent.
- `ScheduleRepository.listMine(filter)` trả `List<ScheduleItem>` có mã và tên môn. Huy làm sớm để Sáng dùng lịch tháng.
- SubjectPickerActivity trả `AppConstants.EXTRA_SUBJECT_ID` bằng Activity Result API; Thu nhận để đặt lịch.
- Ngày lưu `yyyy-MM-dd`, giờ `HH:mm`; trạng thái `PENDING`, `CONFIRMED`, `CANCELLED`.
- Sau commit lịch gọi `NotificationHelper.refresh(context)`. Khi sửa/hủy/xóa gọi thêm `showChange(context, studentId, scheduleId)`; lỗi thông báo không rollback dữ liệu.
- Trở về danh sách hoặc lịch biểu thì tải lại ở `onResume`. Có thể dùng dữ liệu giả trong test/debug khi đồng đội chưa xong; giữ chữ ký hàm và bỏ đường chạy giả khỏi bản bàn giao.

## Dữ liệu demo

DBHelper chỉ seed lần đầu: 1 Admin, 30 Student, 30 môn và 3.670 lịch; có lịch từ 90 ngày trước đến 90 ngày sau. Ngày mẫu tính theo ngày tạo database trên thiết bị. Database của demo visual dùng cùng bộ seed, lưu riêng trong `sang_visual_demo.db`.

| Username | Mật khẩu demo | Vai trò |
| --- | --- | --- |
| admin | Admin@123 | ADMIN |
| sv01 | Student@123 | STUDENT |
| sv02 | Student@123 | STUDENT |

Đây là tài khoản giả để học và demo. **Chưa login được cho tới khi An triển khai xong.** Seed chứa password băm theo format `pbkdf2_sha256$600000$saltHex$hashHex`, PBKDF2WithHmacSHA256, UTF-8, salt 16 byte và key 256 bit. An triển khai hash/verify tương thích; tài khoản mới dùng salt ngẫu nhiên. Không lưu password trong SharedPreferences.

- Xóa Account của Student → Student và lịch liên quan bị xóa theo CASCADE.
- Môn đã có lịch dùng → chặn xóa bằng RESTRICT.
- Chống trùng lịch do Thu triển khai trong transaction, chưa có trong khung.
- Muốn reset demo: gỡ app hoặc Clear storage; sẽ xóa dữ liệu trên thiết bị. Không reset tự động mỗi lần mở app.

## Code song song và merge

Sau khi cả nhóm mở/build được khung, tạo nhánh riêng từ main:

```text
feature/an-auth-notify
feature/quang-student
feature/sang-subject-month
feature/thu-booking
feature/huy-schedule-agenda
```

Trước khi gửi PR: `git fetch origin` rồi `git merge origin/main`. Thứ tự ghép: auth/password của An → môn của Sáng và sinh viên của Quang → đọc lịch của Huy → đặt/sửa của Thu → trạng thái/ngày tuần của Huy, lịch tháng của Sáng và thông báo của An. Chia PR nhỏ, ít nhất một người khác review, kèm cách thử và ảnh/video. Không force push main.

## Kiểm tra build

Android Studio: **Build → Assemble Project**, hoặc terminal ở thư mục gốc:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

macOS/Linux dùng `chmod +x gradlew` rồi `./gradlew assembleDebug testDebugUnitTest lintDebug`. APK debug ở `app/build/outputs/apk/debug/app-debug.apk`.

Test SQLite trên emulator/thiết bị bằng `connectedDebugAndroidTest`; test dùng database in-memory. GitHub Actions đã cấu hình build, unit test và lint khi push/PR.

## Tài liệu chính thức

- [AGP 9.2 và công cụ tương thích](https://developer.android.com/build/releases/agp-9-2-0-release-notes)
- [SQLiteOpenHelper](https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper)
- [WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
