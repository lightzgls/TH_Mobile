# Tiến độ phần Sáng

Nhánh: feature/sang-subject-month. Ngày kiểm tra: 31/10/2026.

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

AuthGuard của An hiện vẫn từ chối mọi quyền. Các màn hình hiển thị yêu cầu đăng nhập và khóa thao tác. Chưa thể demo CRUD bằng tài khoản thật cho tới khi ghép auth; không thêm phiên giả vào ứng dụng.

ScheduleRepository.listMine của Huy hiện trả danh sách rỗng. Lịch tháng đã gọi đúng API và khoảng ngày của tháng; cần ghép truy vấn thật để hiện dữ liệu. Form đặt/sửa của Thu, chi tiết và lịch ngày/tuần của Huy vẫn thuộc phần của các bạn đó.

Giữ nguyên chữ ký hàm, extras, models, Manifest và schema chung.

## Kiểm thử đã chạy

Lệnh tại thư mục gốc:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest
```

- 6 unit test đạt: 2 test định dạng ngày giờ có sẵn, 4 test mới về lịch tháng.
- 15 test trên emulator Pixel_10 / Android 17 API 37 đạt: 4 schema có sẵn, 6 repository môn, 5 màn hình của Sáng.
- Test repository dùng SQLite in-memory, quyền giả chỉ nằm trong androidTest. Kiểm tra CRUD, chuẩn hóa, tìm chuỗi có %, _, dấu nháy và dấu gạch chéo, dữ liệu sai, mã trùng, chặn xóa môn có lịch đã hủy, quyền Student và chưa đăng nhập.
- Test tháng kiểm tra 28/29/30/31 ngày, lưới 4/5/6 tuần, năm nhuận thế kỷ, loại lịch hủy/khác Student/ngoài tháng, thứ tự giờ và id.
- Test màn hình kiểm tra chặn truy cập chưa đăng nhập, ẩn thao tác Admin trong picker, giữ dữ liệu form khi tạo lại Activity, chuyển năm và giữ ngày chọn, extra ngày sai không gây crash.
- Build và lint đạt; các cảnh báo còn lại thuộc phiên bản dependency và cấu hình backup có sẵn.
- Sau lần kiểm thử trên, chỉnh style thanh nút tháng và định dạng số tín chỉ theo resource; build/lint lại đạt.

Chưa nghiệm thu luồng Admin/Student đăng nhập thật, trả kết quả picker sang form Thu và dữ liệu lịch thật. Các ca đó cần chạy khi ghép phần đồng đội.

## Checklist demo sau khi ghép

1. Admin thêm môn có mã viết thường và khoảng trắng; tìm bằng mã/tên, xem chi tiết, sửa tên/tín chỉ.
2. Thử mã trùng và tín chỉ trống, 0, âm, không nguyên, quá lớn; dữ liệu nhập phải còn.
3. Xóa môn chưa dùng có xác nhận; môn có lịch PENDING/CONFIRMED/CANCELLED phải bị chặn.
4. Student chọn môn trong form Thu, kiểm tra đúng subjectId; hủy chọn không thay môn đang nhập.
5. Tạo/sửa/hủy lịch rồi trở về tháng: số lịch và danh sách ngày cập nhật theo cùng dữ liệu.
6. Chuyển tháng Hai năm nhuận và tháng 30/31 ngày; chọn ngày, mở ngày/tuần và đặt lịch, kiểm tra extra đúng.
7. Chuyển giữa hai Student, kiểm tra không thấy lịch của người trước.
