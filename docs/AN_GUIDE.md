# Hướng dẫn phần An: đăng nhập, phiên và thông báo

Nhánh: feature/an-auth-notify. Java + XML, SQLiteOpenHelper, SharedPreferences và WorkManager theo kế hoạch nhóm.

## Đọc code theo thứ tự

1. PasswordHelper: tạo salt ngẫu nhiên, băm PBKDF2, kiểm tra mật khẩu tương thích dữ liệu mẫu. Không trim mật khẩu; không lưu mật khẩu trong phiên.
2. SessionManager: lưu accountId, studentId, username, role và token riêng mỗi lần đăng nhập. Khi đọc luôn kiểm tra Account/Student thật trong SQLite.
3. AuthGuard: kiểm tra quyền tại repository. requireStudent trả Session hợp lệ hoặc null.
4. AuthRepository: validate dữ liệu, bind tham số SQL, kiểm tra mật khẩu, lưu phiên và logout.
5. LoginActivity: nhập tài khoản, báo lỗi, khóa nút khi xử lý, chuyển Home đúng vai trò.
6. MainActivity và BaseActivity: khôi phục phiên, bảo vệ các Activity, xóa back stack khi logout.
7. NotificationHelper, ReminderWorker và TimeChangeReceiver: lên lịch nhắc, kiểm tra dữ liệu trước khi gửi và cập nhật khi đổi giờ.

Các file chung được sửa để tích hợp: MainActivity, BaseActivity, AdminHomeActivity, StudentHomeActivity, các layout Home/main và README. Khi gửi PR cần nêu rõ để Sáng review. Không đổi schema, model hay chữ ký API của các thành viên khác.

## Chạy thử đăng nhập

Mở thư mục gốc bằng Android Studio, Sync, Run module app.

| Username | Password | Vai trò |
| --- | --- | --- |
| admin | Admin@123 | ADMIN |
| sv01 | Student@123 | STUDENT |
| sv02 | Student@123 | STUDENT |

Thử bỏ trống, mật khẩu sai, đăng nhập đúng, đóng/mở app, logout rồi bấm Back.
Tên đăng nhập trim và chuyển chữ thường; mật khẩu giữ nguyên khoảng trắng.
Admin không vào được các trang Student và ngược lại. Repository của các phần khác vẫn phải gọi AuthGuard; bảo vệ Activity không thay cho bảo vệ dữ liệu.

## Quy ước validate

- Username không rỗng, tối đa 100 ký tự sau trim.
- Password không rỗng/không chỉ khoảng trắng, tối đa 256 ký tự. Không áp thêm điều kiện mật khẩu mạnh ngoài phạm vi đề.
- Mật khẩu băm sai định dạng, salt/hash sai hex hoặc số vòng lặp không hợp lệ được từ chối.
- Phiên sai kiểu dữ liệu, role bị sửa, tài khoản/hồ sơ mất hoặc accountId/studentId không khớp sẽ bị xóa.
- Dữ liệu lịch ngày giờ không hợp lệ không làm gián đoạn các lịch còn lại khi lên nhắc.

Quang cần dùng cùng giới hạn khi tạo tài khoản. PasswordHelper.hash ném IllegalArgumentException khi mật khẩu mới không hợp lệ.

## Ghép thông báo với Thu, Huy và Quang

Gọi sau khi đã commit transaction, không gọi khi dữ liệu còn chưa được lưu:

    NotificationHelper.refresh(context);

Khi sửa/hủy/xóa lịch thành công, gọi thêm:

    NotificationHelper.showChange(context, studentId, scheduleId);

Tạo lịch mới chỉ cần refresh và thông báo thành công trên giao diện.
Khi xóa sinh viên, Quang gọi refresh để xóa nhắc không còn hợp lệ.
Hai hàm tự chuyển việc sang DbExecutor, không cần chờ kết quả. Lỗi thông báo không rollback lịch.

refresh hủy work cũ theo tag, chờ hủy xong, rồi tạo work riêng cho mỗi lịch PENDING/CONFIRMED của Student hiện tại. Nhắc trước 15 phút; nếu còn dưới 15 phút thì lên lịch sớm nhất có thể; không nhắc lịch đã bắt đầu.
Worker kiểm tra lại token phiên, chủ sở hữu, trạng thái, thời điểm bắt đầu và múi giờ. Lịch đã xóa/hủy/đổi giờ hoặc thuộc phiên cũ không được gửi.
Một buổi học cùng giờ bắt đầu chỉ nhắc một lần trong cùng phiên; mở lại app không nhắc lặp.
Thông báo đã hiển thị nhưng không còn hợp lệ được dọn khi refresh. Chạm thông báo mở ứng dụng qua bước kiểm tra phiên.

Android 13 trở lên hỏi quyền khi Student vào Home lần đầu. Từ chối quyền vẫn sử dụng ứng dụng bình thường; có nút bật thông báo để xin lại hoặc mở Settings.
WorkManager có thể chạy trễ theo điều kiện hệ thống, không cam kết đúng từng phút.

## Kiểm tra tự động

    .\gradlew.bat assembleDebug testDebugUnitTest lintDebug
    .\gradlew.bat connectedDebugAndroidTest

PasswordHelperTest: mật khẩu seed, Unicode, khoảng trắng, salt ngẫu nhiên, hash sai và đầu vào sai.
AnSessionTest: database in-memory, phiên bị sửa, tài khoản/hồ sơ mất, token thay đổi và logout.
AnLoginTest: tài khoản demo trên emulator test, sai mật khẩu, đầu vào SQL, hai vai trò và logout. Không sửa/xóa bảng; có ghi/xóa phiên của app test. Chạy trên emulator dành cho kiểm thử.
DBHelperSchemaTest có sẵn kiểm tra seed và khóa ngoại.

Cần demo thêm khi ghép chức năng của Thu/Huy: đặt buổi học trong 15 phút tới, nhận nhắc, sửa giờ, hủy/xóa, đổi Student và từ chối quyền. Các chức năng booking/CRUD/calendar hiện vẫn là khung của người khác nên chưa có luồng giao diện đầy đủ để demo các thao tác này.

APK debug: app/build/outputs/apk/debug/app-debug.apk.

## Tài liệu đối chiếu

- [WorkManager API](https://developer.android.com/reference/androidx/work/WorkManager)
- [Quyền thông báo](https://developer.android.com/develop/ui/views/notifications/notification-permission)

## Kết quả kiểm tra lần triển khai này

- assembleDebug: thành công.
- testDebugUnitTest: 7 test qua (5 mật khẩu, 2 định dạng ngày giờ).
- connectedDebugAndroidTest: 10 test qua trên Pixel_8_Pro, Android 17 (API 37).
- lintDebug: 0 lỗi, 11 cảnh báo về dependency/cấu hình/resource; không nâng dependency ngoài phạm vi.
- git diff --check: không có lỗi whitespace.
- Chưa kiểm thử xuyên suốt giao diện đặt/sửa/hủy lịch vì các phần đó còn là khung.
