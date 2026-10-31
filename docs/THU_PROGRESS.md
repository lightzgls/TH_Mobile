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
- `AuthGuard` của An vẫn trả null: form yêu cầu đăng nhập rồi đóng, repository từ chối lưu. Cần ghép auth trước khi demo bằng tài khoản thật.
- `ScheduleRepository.getMine` của Huy vẫn trả null: form sửa đã gọi đúng API; cần ghép truy vấn thật để tải lịch cũ.
- `NotificationHelper` của An vẫn là khung: phần Thu đã gọi sau commit, cần ghép xử lý thông báo thật.
- Không đổi schema, model, chữ ký hàm chung, Manifest hoặc dependency.

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

## Checklist demo sau khi ghép auth và đọc lịch

1. Student đăng nhập, chọn môn/ngày/giờ/địa điểm, lưu; kiểm tra lịch mới là PENDING.
2. Đã có 09:00–10:00: thử 09:30–10:30, 08:00–11:00 và 09:00–10:00 đều bị chặn; 10:00–11:00 được phép.
3. Thử thiếu địa điểm, ngày quá khứ, giờ kết thúc bằng/trước bắt đầu; dữ liệu nhập phải còn. Bấm Lưu liên tục chỉ tạo một lịch.
4. Mở sửa từ chi tiết: giữ nguyên giờ được phép nếu không đụng lịch khác; sửa lịch CONFIRMED vẫn giữ CONFIRMED.
5. Thử id của sinh viên khác, id đã xóa, lịch đã hủy hoặc bắt đầu; không lưu được và không crash.
6. Xoay màn hình khi nhập, quay về danh sách/lịch tháng sau lưu; kiểm tra dữ liệu và ngày đang chọn.
7. Khi ghép thông báo An: sửa giờ phải cập nhật nhắc cũ; từ chối quyền thông báo vẫn lưu được lịch.
