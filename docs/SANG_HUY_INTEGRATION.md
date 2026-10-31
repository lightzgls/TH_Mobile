# Kết nối Huy – Sáng trên nhánh Sáng

Chỉ có trên `feature/sang-subject-month`. Main chỉ nhận bộ sample lớn (30 sinh viên, 30 môn, 3.670 lịch).

- Đã merge nhánh `feature/huy-schedule-agenda`, bao gồm auth, phiên và thông báo của An là phần phụ thuộc mà Huy đã ghép.
- Lịch tháng gọi `ScheduleRepository.listMine()` của Huy với khoảng ngày tháng đang xem, lọc theo Student đăng nhập.
- Demo riêng của Sáng cũng dùng repository Huy với database demo riêng; không còn truy vấn lịch song song.
- Sửa nhận diện quyền cho SubjectPickerActivity: Student được chọn môn dù lớp này kế thừa SubjectListActivity của Admin.
- Các Activity demo chỉ trong src/debug được giữ hoạt động độc lập với phiên thật.
- Huy mới triển khai repository; UI danh sách, chi tiết và ngày/tuần còn là khung. Không triển khai thay phần này.
- Chạy cấu hình app để đăng nhập thật: admin / Admin@123 để quản lý môn, sv01 / Student@123 để xem lịch tháng. Demo độc lập vẫn chạy qua cấu hình Sang UI Demo.
- Không chạy thêm kiểm thử cho phần ghép này theo yêu cầu người dùng.
