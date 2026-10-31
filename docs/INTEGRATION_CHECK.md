# Kiểm tra bản tích hợp trên main

Ngày 31/10/2026. Dùng Java + XML, SQLiteOpenHelper và bộ sample Sáng đã push lên main.

## Nguồn đã ghép

- Sáng: origin/feature/sang-subject-month tại efd7ac1; có môn, chọn môn, lịch tháng và sample lớn.
- Thu: origin/feature/thu-booking tại 73e9844; có đặt/sửa, validation/chống trùng và CRUD sinh viên của Quang đã được đặt đúng vào app/src/main.
- An: đã có trong lịch sử nhánh Thu/Sáng tại 8193a84; giữ nguyên đăng nhập, phiên và thông báo.
- Huy: origin/feature/huy-schedule-agenda tại 380fccd; repository, danh sách/tìm/lọc và màn hình chi tiết/đổi trạng thái/xóa lịch.
- Không đưa thư mục TH_Mobile-main lồng trong nhánh Quang vào project: chức năng sinh viên đã được Thu tích hợp vào đúng module app, tránh tạo project trùng.

Merge giữ nguyên lịch sử đóng góp, không squash và không force push.

## Phần nối bổ sung

Activity ngày/tuần còn là khung. Đã ghép thêm màn hình chi tiết Huy push trong lúc kiểm thử. Bản tích hợp bổ sung:
- Mở lịch từ danh sách/lịch tháng, kiểm tra chủ sở hữu tại repository.
- Mở form sửa của Thu, tự tải lại sau khi quay về.
- Xác nhận, hủy, xóa có khóa nút trong lúc xử lý; hủy/xóa có hộp thoại xác nhận.
- Lịch đã bắt đầu hoặc đã hủy chỉ xem/xóa; repository kiểm tra lại khi ghi.
- Lịch ngày và tuần (thứ Hai đến Chủ nhật), chọn ngày, trước/sau/hôm nay, chuyển lịch tháng và đặt lịch theo ngày.
- Kiểm tra khoảng ngày tìm kiếm không bị đảo; báo lỗi thay vì để exception thoát khỏi tác vụ nền.
- Không hiển thị SQL exception thô cho người dùng.

Không đổi schema, database version, seed hoặc API repository của các thành viên.

## Dữ liệu dùng chung

Giữ nguyên DBHelper.seedDemo của Sáng: 1 Admin, 30 sinh viên, 30 môn, 3.670 lịch.
Màn hình thật đọc study_schedule.db; demo Sáng vẫn dùng sang_visual_demo.db riêng theo thiết kế sẵn có.

Seed chỉ chạy khi tạo database lần đầu. Cài APK cập nhật không tự reset dữ liệu người dùng.
Sau khi người dùng đồng ý sao lưu và khởi tạo lại, emulator-5554 đã có đúng 31 Account, 30 Student, 30 Subject, 3.670 StudySchedule; kiểm tra SQLite integrity_check và foreign_key_check đều đạt.
Bản sao lưu dữ liệu cũ: local-backups/before-main-20261031-152052.tar, kèm SHA-256. Thư mục local-backups được gitignore để không push dữ liệu cá nhân; bản sao lưu không mất khi chạy Gradle clean.
Không reset dữ liệu mỗi lần mở ứng dụng.

## Kết quả kiểm thử

- assembleDebug, testDebugUnitTest, lintDebug, assembleDebugAndroidTest đạt.
- 21 unit test đạt.
- Lint 0 lỗi; còn cảnh báo resource, autofill, cấu hình và phiên bản dependency.
- Bộ 52 instrumentation test đạt trên emulator riêng API 37.2, database mới dùng sample thật của Sáng.
- IntegratedScheduleFlowTest kiểm tra: tạo tài khoản, đặt lịch, mở danh sách/chi tiết, sửa địa điểm, xác nhận, xem ngày/tuần/tháng, hủy rồi xóa.
- Kiểm tra không đọc được lịch Student khác.
- Kiểm tra WorkManager gửi nhắc thật cho lịch trong 15 phút tới; hủy thì xóa nhắc đã hiển thị và gửi thông báo thay đổi; logout dọn thông báo.
- Các test đặt lịch có các ca giao nhau/bao trùm/sát giờ, dữ liệu sai, quyền sở hữu, rollback và hai yêu cầu đồng thời.
- Test schema xác nhận sample không có lịch hiệu lực chồng nhau và không có khóa ngoại lỗi.

Log cục bộ: app/build/integration-results.txt; kiểm thử lại luồng chịu ảnh hưởng bởi màn hình chi tiết mới của Huy ở app/build/integration-final-results.txt. Test Android chỉ chạy trên emulator riêng; chỉ dọn các bản ghi test tự tạo. Bộ sample của app chính không bị sửa bởi bộ test này.

## Chạy lại

    .\gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
    .\gradlew.bat connectedDebugAndroidTest

Nếu có nhiều thiết bị, chọn serial khi chạy ADB trực tiếp:

    adb -s SERIAL install -r -t app/build/outputs/apk/debug/app-debug.apk
    adb -s SERIAL install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    adb -s SERIAL shell am instrument -w com.example.studentmanager.test/androidx.test.runner.AndroidJUnitRunner

Tài khoản: admin / Admin@123; sv01 đến sv30 / Student@123.
APK: app/build/outputs/apk/debug/app-debug.apk.
WorkManager có thể chạy trễ do hệ điều hành; không dùng để cam kết nhắc chính xác từng phút.
