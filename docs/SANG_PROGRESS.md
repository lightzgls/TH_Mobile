# Tiến độ phần Sáng

Nhánh: feature/sang-subject-month. Ngày kiểm tra: 31/10/2026.

## Tự bấm thử giao diện ngay

1. Mở project trong Android Studio, đợi Gradle Sync. Dùng build variant **debug**.
2. Trên thanh công cụ, chọn cấu hình chạy **Sang UI Demo**, chọn emulator/điện thoại rồi bấm **Run ▶**. Cấu hình chia sẻ nằm ở `.run/Sang_UI_Demo.run.xml`.
3. Nếu chưa thấy cấu hình đó, chạy cấu hình **app** như bình thường rồi mở icon **Sáng UI Demo** trong danh sách ứng dụng trên thiết bị.
4. Chọn **Quản lý môn** để tìm, xem chi tiết, thêm, sửa, xóa. Để trống form rồi bấm lưu để xem lỗi; thử mã trùng; xóa GIT101 để xem xác nhận, xóa MOB101 để xem lỗi môn đang có lịch.
5. Chọn **Chọn môn** rồi chạm một môn: menu demo hiện lại ID, mã và tên môn đã nhận qua Activity Result.
6. Chọn **Lịch tháng**: hôm nay có hai lịch hiệu lực; bộ mẫu còn có lịch ngày mai, lịch đã qua và lịch ở tháng trước/sau. Chạm ngày khác để xem trạng thái trống; chuyển tháng; chạm lịch để xem thông tin. Nút mở ngày/đặt lịch hiện ngày sẽ truyền sang phần đồng đội.
7. Xoay màn hình, mở bàn phím và thử cỡ chữ lớn để tự xem bố cục. Dùng **Tạo lại dữ liệu demo** khi muốn về dữ liệu ban đầu.

Demo dùng lại layout và xử lý của các Activity thật; chỉ thay nguồn dữ liệu và quyền trong `src/debug`. Database `sang_visual_demo.db` tách khỏi database ứng dụng; không lưu phiên demo vào SessionManager. Mã demo, icon và menu không có trong APK release.

Bộ mẫu dùng chung hiện có 3.670 lịch, tự tạo qua DBHelper cho cả database chính lần đầu và database demo. Xem [bộ dữ liệu cho cả nhóm](SAMPLE_DATA.md); người đã cài demo cũ bấm **Tạo lại dữ liệu demo** để lấy bộ mới.

## Phần đã triển khai

- SubjectRepository: đọc cho Admin/Student; thêm, sửa, xóa chỉ cho Admin.
- Tìm theo mã/tên, trim dữ liệu, mã môn viết hoa, tín chỉ nguyên dương.
- Chặn mã trùng; chặn xóa môn đang được dùng, kể cả lịch CANCELLED.
- SubjectListActivity: tìm kiếm, danh sách, xem chi tiết, mở form sửa và xác nhận xóa.
- SubjectFormActivity: thêm/sửa, báo lỗi từng ô, giữ dữ liệu nhập khi lỗi hoặc tạo lại Activity, khóa nút khi đang lưu.
- SubjectPickerActivity: dùng lại danh sách/tìm kiếm, chỉ cho Student chọn; kiểm tra môn còn tồn tại rồi trả RESULT_OK và EXTRA_SUBJECT_ID. Bấm quay lại trả kết quả hủy mặc định.
- MonthCalendarActivity: lưới thứ Hai–Chủ nhật, tháng trước/sau/hôm nay, giữ ngày chọn khi tạo lại Activity. Đếm PENDING/CONFIRMED, hiển thị lịch ngày đã chọn và chữ trạng thái.
- Từ tháng mở DayWeekCalendarActivity hoặc BookingFormActivity bằng EXTRA_SELECTED_DATE; chạm lịch mở ScheduleDetailActivity bằng EXTRA_SCHEDULE_ID.
- Tải lại trong onResume, database chạy qua DbExecutor; không thêm dependency hay đổi schema.

MonthCalendarHelper là tiện ích riêng của Sáng để tạo ô tháng và lọc lịch hiệu lực; không thay phần truy vấn của Huy.

## Điểm nối cần ghép với đồng đội

AuthGuard của An hiện vẫn từ chối mọi quyền. Các màn hình của luồng chính hiển thị yêu cầu đăng nhập và khóa thao tác. Chưa thể demo CRUD bằng tài khoản thật cho tới khi ghép auth. Sáng UI Demo ở trên dùng quyền và database riêng chỉ trong bản debug.

ScheduleRepository.listMine của Huy hiện trả danh sách rỗng. Lịch tháng đã gọi đúng API và khoảng ngày của tháng; cần ghép truy vấn thật để hiện dữ liệu. Form đặt/sửa của Thu, chi tiết và lịch ngày/tuần của Huy vẫn thuộc phần của các bạn đó.

Giữ nguyên chữ ký hàm, extras, models, Manifest và schema chung.

## Kiểm thử đã chạy

Lệnh tại thư mục gốc:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest
```

- 6 unit test đạt: 2 test định dạng ngày giờ có sẵn, 4 test mới về lịch tháng.
- 15 test trên emulator Pixel_10 / Android 17 API 37 đạt: 4 schema có sẵn, 6 repository môn, 5 màn hình của Sáng.
- Bổ sung 4 test visual demo đạt: danh sách có dữ liệu, form báo lỗi rồi lưu, picker trả ID/tên về menu, tháng hiện hai lịch hiệu lực. Tổng hiện tại là 6 unit test và 19 test emulator.
- Khi bổ sung bộ mẫu dùng chung, thêm 1 test kiểm tra dữ liệu biên tháng, trạng thái, khóa ngoại và không trùng giờ hiệu lực. Bộ kiểm thử hiện có 6 unit test và 20 test emulator.
- Test repository dùng SQLite in-memory, quyền giả chỉ nằm trong androidTest. Kiểm tra CRUD, chuẩn hóa, tìm chuỗi có %, _, dấu nháy và dấu gạch chéo, dữ liệu sai, mã trùng, chặn xóa môn có lịch đã hủy, quyền Student và chưa đăng nhập.
- Test tháng kiểm tra 28/29/30/31 ngày, lưới 4/5/6 tuần, năm nhuận thế kỷ, loại lịch hủy/khác Student/ngoài tháng, thứ tự giờ và id.
- Test màn hình kiểm tra chặn truy cập chưa đăng nhập, ẩn thao tác Admin trong picker, giữ dữ liệu form khi tạo lại Activity, chuyển năm và giữ ngày chọn, extra ngày sai không gây crash.
- Build và lint đạt; các cảnh báo còn lại thuộc phiên bản dependency và cấu hình backup có sẵn.
- Sau lần kiểm thử trên, chỉnh style thanh nút tháng và định dạng số tín chỉ theo resource; build/lint lại đạt.
- Đã build debug/release, kiểm tra APK release chỉ có launcher chính, và xem ảnh chụp màn hình demo trên emulator. Từ ảnh QA đã chỉnh căn ô lịch có nhãn hai dòng.

Chưa nghiệm thu luồng Admin/Student đăng nhập thật, trả kết quả picker sang form Thu và dữ liệu lịch thật. Các ca đó cần chạy khi ghép phần đồng đội.

## Checklist demo sau khi ghép

1. Admin thêm môn có mã viết thường và khoảng trắng; tìm bằng mã/tên, xem chi tiết, sửa tên/tín chỉ.
2. Thử mã trùng và tín chỉ trống, 0, âm, không nguyên, quá lớn; dữ liệu nhập phải còn.
3. Xóa môn chưa dùng có xác nhận; môn có lịch PENDING/CONFIRMED/CANCELLED phải bị chặn.
4. Student chọn môn trong form Thu, kiểm tra đúng subjectId; hủy chọn không thay môn đang nhập.
5. Tạo/sửa/hủy lịch rồi trở về tháng: số lịch và danh sách ngày cập nhật theo cùng dữ liệu.
6. Chuyển tháng Hai năm nhuận và tháng 30/31 ngày; chọn ngày, mở ngày/tuần và đặt lịch, kiểm tra extra đúng.
7. Chuyển giữa hai Student, kiểm tra không thấy lịch của người trước.
