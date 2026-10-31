# Tiến độ phần Thu

Nhánh: `feature/thu-booking`. Ngày kiểm tra: 31/10/2026.

## Phần đã triển khai

- `BookingValidator`: bắt buộc môn và địa điểm; ngày, giờ đúng định dạng và có thật; giờ bắt đầu ở tương lai và nhỏ hơn giờ kết thúc. Không hỗ trợ qua nửa đêm.
- `BookingRepository.save(-1, draft)` tạo lịch `PENDING`; truyền id thật để sửa. Sinh viên lấy từ `AuthGuard`, không lấy từ Intent.
- Kiểm tra quyền sở hữu, lịch chưa bắt đầu và chưa hủy khi sửa. Giữ nguyên trạng thái khi sửa.
- Kiểm tra trùng và ghi dữ liệu trong một transaction. Chỉ xét cùng sinh viên, cùng ngày, lịch `PENDING` hoặc `CONFIRMED`. Loại chính id đang sửa; cho phép hai lịch sát giờ nhau.
- Trim dữ liệu, dùng tham số SQL, đóng Cursor và transaction khi có lỗi. Trả lỗi dễ hiểu, không đưa lỗi SQL lên giao diện.
- Sau commit gọi `NotificationHelper.refresh`; khi sửa gọi thêm `showChange`. Lỗi thông báo không làm đổi kết quả lưu.
- Form dùng chung cho tạo/sửa, nhận `EXTRA_SELECTED_DATE` hoặc `EXTRA_SCHEDULE_ID`. Chọn môn qua Activity Result API và tải lại môn theo id.
- Có bộ chọn ngày/giờ, giữ dữ liệu nhập khi lỗi hoặc tạo lại Activity, khóa thao tác trong lúc tải/lưu. Database chạy qua `DbExecutor`.

## Cách đọc code

1. Đọc `BookingValidator.validate`: các quy tắc dữ liệu nhập.
2. Đọc `BookingRepository.save`, rồi `saveInTransaction` và `checkEditable`: luồng lưu, quyền và truy vấn chống trùng.
3. Đọc `BookingFormActivity.onCreate`, `loadForm` và `save`: nối giao diện với các repository.

Constructor riêng và `notifySaved` của repository hỗ trợ test SQLite in-memory; quyền giả chỉ nằm trong `androidTest`, không có phiên giả trong ứng dụng.

## Điểm nối đã ghép và còn chờ

- Đã fetch remote và merge code Sáng tại commit `e352dac` bằng merge commit `1c50317`: có repository môn, CRUD môn, bộ chọn môn trả `EXTRA_SUBJECT_ID` và lịch tháng.
- Đã merge nhánh An tại `8193a84` bằng commit `1f2ef66`: đăng nhập, phiên, AuthGuard và thông báo đã nối với booking.
- Đã đưa 8 file chức năng của Quang tại `f6fd687` vào đúng `app/src/main`. Nhánh Quang chứa các file trong thư mục lồng `TH_Mobile-main`; chỉ lấy phần CRUD sinh viên và resource tương ứng, giữ cấu hình build/CI đang dùng.
- Sửa kiểm tra màn hình dùng chung: SubjectPickerActivity kế thừa SubjectListActivity nhưng thuộc Student. Kiểm tra picker trước nhóm màn hình Admin để Student chọn môn được.
- `ScheduleRepository.getMine` của Huy vẫn trả null: form sửa đã gọi đúng API; cần ghép truy vấn thật để tải lịch cũ.
- Đã kiểm tra lưu thành công tạo work nhắc lịch thật qua NotificationHelper và WorkManager. Chưa nghiệm thu thời điểm hiển thị nhắc hoặc toàn bộ thao tác sửa/hủy/xóa khi chưa có phần Huy.
- Không đổi schema, model, chữ ký repository, Manifest hoặc dependency. BaseActivity có sửa nhỏ ở phân quyền picker khi tích hợp.

## Kiểm thử

Lệnh chuẩn tại thư mục gốc:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
.\gradlew.bat connectedDebugAndroidTest
```

- Sau khi ghép Sáng: build app và APK test đạt; 16 unit test đạt (10 validator, 2 ngày giờ, 4 lịch tháng); lint có 0 lỗi, 7 cảnh báo thuộc cấu hình/dependency sẵn có.
- Máy kiểm tra dùng JDK 24 và Gradle 9.5.0 đã có, vì tải Wrapper 9.4.1 chậm. Giữ nguyên Java source 17 và cấu hình project của nhóm.
- Đã viết và biên dịch 12 test repository SQLite: tạo PENDING, trim, các dạng giao nhau/bao trùm, sát giờ, lịch hủy/khác sinh viên/khác ngày, sửa giữ trạng thái, rollback, quyền sở hữu, lịch đã bắt đầu, dữ liệu sai, lỗi thông báo và hai yêu cầu đồng thời.
- Đã chạy đạt cả 27 test instrumentation trên emulator Android 17 API 37.1: 12 test booking, 4 schema, 6 repository môn và 5 màn hình của Sáng.
- Test SQLite dùng database in-memory. Emulator dùng bản sao dữ liệu riêng trong `app/build`, không sửa dữ liệu AVD gốc.
- Task Gradle `connectedDebugAndroidTest` chờ mà chưa cài APK trong môi trường này; đã cài hai APK vừa build và chạy cùng AndroidJUnitRunner trực tiếp qua ADB, kết quả `OK (27 tests)`. Log: `app/build/thu-instrumentation-results.txt`.

### Sau khi ghép An

- Build app và APK test đạt; 21 unit test đạt; lint có 0 lỗi, 6 cảnh báo ở lần kiểm tra.
- Cả 36 test instrumentation đạt trên emulator riêng, gồm 3 test luồng mới An–Sáng–Thu. Log: `app/build/thu-auth-integration-results.txt`.
- BookingFlowTest dùng màn hình và tài khoản thật: đăng nhập từ LoginActivity, mở form từ Student Home, nhận kết quả chọn môn, chọn giờ, chặn lịch trùng, giữ dữ liệu lỗi, lưu sát giờ và kiểm tra WorkManager đã lên nhắc. Có thêm test khôi phục form khi tạo lại Activity và chặn Admin mở form Student.
- Cập nhật SubjectScreenTest theo phân quyền đã ghép: Student vào picker, Admin vào form môn; chưa đăng nhập được chuyển về login.
- File cục bộ `gradle/gradle-daemon-jvm.properties` xuất hiện trong lúc làm việc và yêu cầu JDK 25 chưa có sẵn. Không sửa file này. Đã build bản sao mã nguồn/cấu hình trong `app/build/thu-integration-check` bằng JDK 24 + Gradle 9.5.0; Java source vẫn là 17. APK kiểm tra nằm trong `app/build/thu-integration-check/app/build/outputs/apk/debug/`.

### Sau khi nối thêm Quang

- Admin tạo sinh viên bằng form thật; tài khoản mới đăng nhập qua AuthRepository của An, đặt lịch qua BookingRepository của Thu và xóa sinh viên sẽ cascade lịch liên quan.
- Đồng bộ giới hạn username 100 ký tự và password 256 ký tự với login, tránh tạo tài khoản không đăng nhập được. Kiểm tra quyền ở các màn hình sinh viên đi qua BaseActivity và luồng nền, không truy vấn phiên trên UI thread.
- 21 unit test và cả 38 test instrumentation đạt. 5 test BookingFlowTest bao gồm tài khoản mới, giới hạn đầu vào, luồng chọn môn/lưu/nhắc, khôi phục form và chặn sai vai trò.
- Build đạt, lint 0 lỗi và 18 cảnh báo (gồm resource, autofill và cấu hình/dependency). Log bộ test cuối: `app/build/thu-all-integration-results.txt`.

Lệnh chạy trực tiếp sau khi build hai APK (chọn đúng serial emulator trên máy):

```powershell
adb -s emulator-5580 install -r -t app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5580 install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5580 shell am instrument -w com.example.studentmanager.test/androidx.test.runner.AndroidJUnitRunner
```

## Các commit chính

- `57385ed`: validator và unit test.
- `bcffb7b`: repository, transaction, chống trùng và test SQLite.
- `b04f818`: form đặt/sửa và resource.
- `1c50317`: ghép chức năng môn học/chọn môn/lịch tháng của Sáng.
- `1f2ef66`: ghép đăng nhập và thông báo của An.
- `47e4c64`: sửa phân quyền picker và kiểm thử luồng thật An–Sáng–Thu.

## Checklist thử luồng thật

Đăng nhập `sv01` / `Student@123`, chọn Đặt lịch mới để thử luồng tạo. Các ca mở lịch cũ để sửa cần ghép ScheduleRepository và màn hình của Huy; remote hiện chưa có nhánh Huy ở lần fetch kiểm tra.

1. Student đăng nhập, chọn môn/ngày/giờ/địa điểm, lưu; kiểm tra lịch mới là PENDING.
2. Đã có 09:00–10:00: thử 09:30–10:30, 08:00–11:00 và 09:00–10:00 đều bị chặn; 10:00–11:00 được phép.
3. Thử thiếu địa điểm, ngày quá khứ, giờ kết thúc bằng/trước bắt đầu; dữ liệu nhập phải còn. Bấm Lưu liên tục chỉ tạo một lịch.
4. Mở sửa từ chi tiết: giữ nguyên giờ được phép nếu không đụng lịch khác; sửa lịch CONFIRMED vẫn giữ CONFIRMED.
5. Thử id của sinh viên khác, id đã xóa, lịch đã hủy hoặc bắt đầu; không lưu được và không crash.
6. Xoay màn hình khi nhập, quay về danh sách/lịch tháng sau lưu; kiểm tra dữ liệu và ngày đang chọn.
7. Khi ghép thông báo An: sửa giờ phải cập nhật nhắc cũ; từ chối quyền thông báo vẫn lưu được lịch.
