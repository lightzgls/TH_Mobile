# Dữ liệu mẫu dùng chung

Bộ mẫu có **1 Admin, 30 sinh viên, 30 môn và 3.670 lịch**, nằm trong `DBHelper.seedDemo()`, tự tạo khi database được tạo lần đầu. Không cần nhập SQL hay copy file database. Ngày mẫu tính theo ngày trên thiết bị lúc tạo database, không gắn cứng vào ngày viết code.

## Mở để test giao diện ngay

1. Lấy main mới nhất, mở project trong Android Studio, chọn variant **debug**.
2. Chọn cấu hình **Sang UI Demo**, chọn emulator/điện thoại và bấm **Run ▶**.
3. Nếu cấu hình chưa xuất hiện, chạy **app** rồi mở icon **Sáng UI Demo** trong danh sách ứng dụng.
4. Dùng các nút quản lý môn, chọn môn và lịch tháng. Bấm **Tạo lại dữ liệu demo** nếu đã cài bộ demo cũ hoặc muốn bắt đầu lại.

Demo dùng các Activity của Sáng và cùng bộ seed như database chung. Dữ liệu được lưu riêng trong `sang_visual_demo.db`; nút tạo lại chỉ tác động đến database demo này. Các luồng chính vẫn dùng `study_schedule.db` và kiểm tra phiên qua AuthGuard.

## Tài khoản, sinh viên và môn học

| Username | Mật khẩu demo | Vai trò | Student ID |
| --- | --- | --- | --- |
| admin | Admin@123 | ADMIN | Không có |
| sv01 | Student@123 | STUDENT | 1 |
| sv02 | Student@123 | STUDENT | 2 |
| sv03 đến sv30 | Student@123 | STUDENT | 3 đến 30 |

Đây là tài khoản giả cho bài tập. Password trong SQLite là hash PBKDF2, không phải mật khẩu rõ. Đăng nhập thật cần phần auth của An được ghép vào main; menu demo hoạt động độc lập với tiến độ auth.

28 sinh viên bổ sung có họ tên tiếng Việt ngắn/dài, mã SV003–SV030, email sv03@example.com đến sv30@example.com và số điện thoại giả để thử tìm kiếm/cuộn danh sách. Tất cả dữ liệu chỉ phục vụ bài tập.

| Mã môn | Tên | Tín chỉ | Cách thử |
| --- | --- | --- | --- |
| MOB101 | Lập trình di động | 3 | Có lịch, không xóa được |
| DB101 | Cơ sở dữ liệu | 3 | Có lịch, không xóa được |
| GIT101 | Làm việc nhóm với Git | 2 | Chưa có lịch, xóa được sau xác nhận |

27 môn bổ sung: JAVA101, WEB101, PY101, DSA101, NET101, OS101, SE101, TEST101, UI101, UX101, AI101, ML101, SEC101, CLOUD101, DEVOPS101, SQL201, API101, LINUX101, MATH101, STAT101, ENG101, PM101, IOT101, GAME101, DES101, PROJ101, RES101. Tên môn có độ dài khác nhau; mô tả có bản trống và bản dài để xem bố cục xuống dòng; tín chỉ 2–4.

GIT101, DES101, PROJ101 và RES101 chưa có lịch, dùng để thử xóa. Các môn còn lại đã có lịch để kiểm tra chặn xóa.

## 14 lịch tình huống cố định

Gọi D là ngày tạo database. Sinh viên 1 là sv01, sinh viên 2 là sv02.

| Sinh viên | Ngày | Giờ | Môn | Trạng thái |
| --- | --- | --- | --- | --- |
| sv01 | D + 1 | 09:00–10:00 | MOB101 | PENDING |
| sv02 | D + 1 | 10:00–11:00 | DB101 | CONFIRMED |
| sv01 | D | 09:00–10:00 | MOB101 | PENDING |
| sv01 | D | 14:00–15:30 | DB101 | CONFIRMED |
| sv01 | D | 16:00–17:00 | MOB101 | CANCELLED |
| sv02 | D | 09:00–10:00 | DB101 | PENDING |
| sv01 | D − 1 | 09:00–10:00 | MOB101 | CONFIRMED |
| sv01 | D + 3 | 10:00–11:00 | DB101 | PENDING |
| sv01 | D + 7 | 14:00–15:30 | MOB101 | CONFIRMED |
| sv01 | D + 7 | 15:30–16:30 | DB101 | PENDING |
| sv01 | D + 7 | 16:00–17:00 | DB101 | CANCELLED |
| sv02 | D + 7 | 14:00–15:30 | MOB101 | CONFIRMED |
| sv01 | Ngày cuối tháng trước D | 08:00–09:00 | DB101 | CONFIRMED |
| sv01 | Ngày đầu tháng sau D | 11:00–12:00 | MOB101 | PENDING |

Ngày đầu/tháng cuối có thể trùng với D + 1, D − 1 hoặc ngày tương đối khác. Giờ được chọn để các lịch hiệu lực của cùng sinh viên không giao nhau.

## 3.656 lịch bổ sung để thử dữ liệu lớn

- Mỗi sinh viên có hai khung giờ 07:00–08:00 và 18:30–20:00, cách ba ngày một lần từ D − 90 đến D + 90.
- Riêng sv01 và sv02 bỏ hai lịch sinh thêm ở D để giữ các tình huống hôm nay trong bảng trên. Do đó có 3.656 lịch sinh thêm, tổng cả bộ là **3.670 lịch**.
- Cả 30 sinh viên đều có lịch; mã môn, ba trạng thái và bảy địa điểm được luân phiên cố định, không dùng random nên mỗi lần reset dễ đối chiếu.
- Địa điểm gồm Phòng A101, A102, B201, B202, Phòng máy C301, Thư viện và Trực tuyến.
- Có nhiều ngày trống giữa các ngày học, nhiều ngày có hai lịch và lịch trải qua nhiều tháng để thử cuộn/tìm/lọc, chuyển tháng và lọc đúng sinh viên.
- Các lịch hiệu lực của cùng sinh viên không giao giờ; có lịch cùng giờ của các sinh viên khác nhau.

## Gợi ý từng người kiểm thử sau khi ghép phần mình

- **An:** đăng nhập ba tài khoản, kiểm tra vai trò/phiên và nhắc các lịch tương lai. Thời gian hôm nay có thể đã qua; dùng lịch tương lai hoặc tạo lịch gần hiện tại để thử nhắc.
- **Quang:** cuộn/tìm/sửa 30 sinh viên, xác nhận số lịch trước khi xóa và kiểm tra cascade; không xóa tài khoản Admin.
- **Sáng:** tìm/xem/sửa môn, mã trùng, xóa môn đang dùng và chưa dùng; chọn môn trả đúng ID; xem tháng trước/sau, ngày có lịch và ngày trống.
- **Thu:** thử lịch sát giờ, chặn giao giờ, bỏ qua lịch đã hủy và lịch của sinh viên khác. Ví dụ D + 7 của sv01 đã có hai lịch sát nhau lúc 14:00–16:30; 16:30–17:30 được phép nếu vẫn ở tương lai, dù có lịch CANCELLED 16:00–17:00.
- **Huy:** tìm/lọc theo ngày, trạng thái, mã/tên môn và địa điểm; chuyển giữa sv01/sv02; lịch tháng chỉ đếm PENDING/CONFIRMED. Kiểm tra trạng thái của lịch tương lai và quyền sở hữu.

Menu demo hiện chỉ bao phủ màn hình của Sáng. Các màn hình còn là stub sẽ cần ghép chức năng trước khi test toàn bộ luồng nhóm.

## Khi đã cài app từ trước

Seed không tự ghi đè dữ liệu đang có và không chạy lại mỗi lần mở app. Với demo visual, bấm **Tạo lại dữ liệu demo** để lấy đủ 30 sinh viên, 30 môn, 3.670 lịch và ngày mới.

Với database của luồng chính, dùng thiết bị test mới hoặc chủ động Clear storage/gỡ cài đặt rồi cài lại. Thao tác này xóa dữ liệu cục bộ của app trên thiết bị đó. Không đổi database version hoặc tự xóa dữ liệu người dùng chỉ để cập nhật sample.

## Kiểm tra tự động

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest
```

Test seed kiểm tra không nhân đôi dữ liệu, đủ ba trạng thái và dữ liệu biên tháng, không có lịch hiệu lực giao giờ của cùng sinh viên, không có khóa ngoại hỏng và giữ GIT101 chưa được sử dụng.
