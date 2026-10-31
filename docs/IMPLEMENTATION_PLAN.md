# Kế hoạch phát triển ứng dụng quản lý và đặt lịch học

Nhóm Sáng An Thu Quang Huy • Phiên bản 1 • Ngày 31 tháng 10 năm 2026

## 01 Cách làm và phân công

Nhóm làm một ứng dụng Android chạy trên một thiết bị, lưu dữ liệu bằng SQLite. Admin quản lý sinh viên và môn học; Student quản lý lịch của chính mình. Mục tiêu là hoàn thành đủ yêu cầu, dễ giải thích khi demo và cho phép cả năm người code song song.

Chọn Java + XML, SQLiteOpenHelper và các Activity riêng. Chỉ dùng thêm AndroidX WorkManager cho nhắc lịch. Không làm backend, đồng bộ nhiều thiết bị, đăng ký tự do hay phê duyệt lịch bởi Admin. Các lựa chọn này là phương án triển khai của nhóm; đề bài không bắt buộc ngôn ngữ hay thư viện cụ thể.

### Phân công đã random

| Người | Phần chức năng chính | Khối lượng dự kiến |
| --- | --- | --- |
| An | Đăng nhập, phân quyền, nhớ phiên, thông báo | 20 đơn vị |
| Quang | Quản lý sinh viên và tài khoản đi kèm | 20 đơn vị |
| Sáng | Quản lý môn học, chọn môn, lịch tháng | 20 đơn vị |
| Thu | Đặt lịch, sửa lịch, kiểm tra trùng lịch | 20 đơn vị |
| Huy | Danh sách, tìm kiếm, chi tiết, trạng thái, xóa lịch, lịch ngày và tuần | 20 đơn vị |

100 đơn vị chỉ là thước đo ước lượng để chia đều, không phải điểm chấm của giảng viên. Mỗi người đều chịu trách nhiệm code, kiểm thử và demo phần mình. Nếu việc thực tế lệch nhiều, chuyển một đầu việc chưa làm cùng quyền sở hữu file tương ứng; cập nhật bảng trước khi chuyển.

### Khung chung không tính đóng góp cá nhân

Cả nhóm thống nhất khung trước khi tách nhánh. Sáng điều phối, gom các thay đổi file chung và xếp thứ tự merge vì là trưởng nhóm. Dựng project, tạo bảng, tạo màn hình rỗng, chốt chữ ký hàm, cấu hình build và điều phối Git đều là bước chuẩn bị chung, không cộng vào 20 đơn vị chức năng của Sáng hay người thao tác.

### Đọc nhanh tài liệu

Phần 02 chốt dữ liệu; 03 chốt cây thư mục; 04 chốt cách gọi giữa các phần; 05 đến 07 là việc từng người; 08 chốt quy tắc đặt lịch; 09 là cách làm và merge; 10 là checklist nghiệm thu. Bắt đầu code chức năng sau khi checklist khung ở phần 03 đã đạt.

<!-- PAGE -->

## 02 Dữ liệu và các quy ước cần thống nhất

Giữ đúng bốn bảng của đề. DBHelper bật foreign key cho mọi kết nối, tạo database phiên bản 1 và không cho từng người tự thêm cột. id là INTEGER PRIMARY KEY AUTOINCREMENT; các khóa ngoại là INTEGER; các trường còn lại dùng TEXT, trừ credits là INTEGER.

| Bảng | Các trường giữ nguyên theo đề | Ràng buộc chính |
| --- | --- | --- |
| Account | id, username, password, role | username duy nhất; role là ADMIN hoặc STUDENT |
| Student | id, studentCode, fullname, email, phone, accountId | studentCode và accountId duy nhất; accountId tham chiếu Account.id |
| Subject | id, subjectCode, subjectName, description, credits | subjectCode duy nhất; credits là số nguyên dương |
| StudySchedule | id, studentId, subjectId, studyDate, startTime, endTime, location, status | studentId và subjectId là khóa ngoại; status là PENDING, CONFIRMED hoặc CANCELLED |

### Dữ liệu nhập và cách lưu

- Bắt buộc: username, password khi tạo tài khoản; studentCode, fullname, email, phone khi tạo sinh viên; subjectCode, subjectName, credits khi tạo môn; đủ ngày, giờ, môn và địa điểm khi đặt lịch. description được để trống.
- Trim khoảng trắng đầu cuối. Chuẩn hóa username thành chữ thường, mã sinh viên và mã môn thành chữ hoa trước khi kiểm tra duy nhất. Không tự viết tìm kiếm bỏ dấu; tìm kiếm chuỗi con theo dữ liệu nhập là đủ.
- Ngày lưu dạng yyyy-MM-dd; giờ lưu dạng HH:mm với đủ hai chữ số. Giao diện hiển thị dd/MM/yyyy và HH:mm. Không lưu chuỗi hiển thị vào database.
- Mỗi lịch chỉ nằm trong một ngày; startTime phải nhỏ hơn endTime. Dùng múi giờ hiện tại của thiết bị nhất quán khi tính thời điểm nhắc. Bộ chọn ngày giờ cần trả về cùng định dạng đã chốt.
- password chứa giá trị băm có salt do PasswordHelper của An tạo, không chứa mật khẩu rõ. SharedPreferences lưu thông tin phiên, không lưu mật khẩu.

### Tạo và xóa dữ liệu liên quan

Quang tạo Account có role STUDENT và Student trong cùng một transaction. Nếu lỗi ở một bước thì rollback cả hai. Sửa sinh viên không đổi id, accountId hay username; cho phép sửa mã sinh viên nếu không trùng. Không thêm chức năng đổi mật khẩu vào phạm vi ban đầu.

Student.accountId dùng ON DELETE CASCADE. StudySchedule.studentId cũng dùng ON DELETE CASCADE. Xóa sinh viên bằng cách xóa tài khoản STUDENT tương ứng, sau hộp thoại xác nhận có nêu số lịch bị xóa. Không cho thao tác này xóa tài khoản ADMIN.

StudySchedule.subjectId dùng ON DELETE RESTRICT. Môn đã được dùng trong bất kỳ lịch nào, kể cả lịch đã hủy, chưa được xóa; trả về thông báo dễ hiểu. Sau khi xóa sinh viên hoặc thay đổi lịch, yêu cầu cập nhật nhắc lịch để không còn thông báo cũ.

<!-- PAGE -->

## 03 Khung dự án và quyền sửa file

Một module app; package gốc com.example.studentmanager, theo cấu trúc mẫu. Cây rút gọn dưới đây nằm trong app/src/main/java/. README có danh sách file đầy đủ. Khung và điểm nối không tính là chức năng hoàn thành.

```text
com.example.studentmanager
├── activities
│   ├── LoginActivity.java                        [An]
│   ├── StudentListActivity.java                  [Quang]
│   ├── SubjectListActivity.java                  [Sáng]
│   ├── BookingFormActivity.java                  [Thu]
│   ├── ScheduleListActivity.java                 [Huy]
│   ├── MonthCalendarActivity.java                [Sáng]
│   ├── DayWeekCalendarActivity.java              [Huy]
│   └── ... các form và chi tiết tương ứng
├── dal
│   ├── DBHelper.java và DbContract.java          [Khung]
│   ├── AuthRepository.java                       [An]
│   ├── StudentRepository.java                    [Quang]
│   ├── SubjectRepository.java                    [Sáng]
│   ├── BookingRepository.java                    [Thu]
│   └── ScheduleRepository.java                   [Huy]
├── models
│   ├── Account.java
│   ├── Student.java
│   ├── Subject.java
│   ├── StudySchedule.java
│   └── ... Session, các Draft, Filter và Result
├── utils
│   ├── SessionManager, PasswordHelper, AuthGuard  [An]
│   ├── BookingValidator                         [Thu]
│   └── AppConstants, DbExecutor, DateTimeHelper  [Khung]
├── notifications
│   └── NotificationHelper, ReminderWorker,
│       TimeChangeReceiver                       [An]
└── MainActivity.java                            [Khung]
```

### Những file phải có trước khi tách nhánh

- Project mở trực tiếp bằng Android Studio, Gradle Wrapper 9.4.1, Android Gradle Plugin 9.2.0, minSdk 26, compileSdk và targetSdk 37, Java source 17. README hướng dẫn Open, Sync và Run.
- DBHelper singleton, DbContract, bốn model, database version 1, seed đúng một lần và bật khóa ngoại. Seed có 1 Admin, 2 Student, 3 môn và lịch ngày hôm sau.
- DbExecutor chạy tuần tự các việc database trên một luồng nền. Repository không cập nhật View; Activity gọi repository qua executor rồi dùng runOnUiThread để cập nhật màn hình.
- Tạo sẵn các Activity trong cây trên, layout rỗng và đăng ký trong AndroidManifest.xml; tạo nút điều hướng và khóa extras ở phần 04. Màn hình chưa có chức năng hiển thị thông báo đang phát triển, không giả báo thành công.
- Tạo sẵn chữ ký public ở phần 04. Hàm chưa triển khai trả NOT_IMPLEMENTED hoặc danh sách rỗng; không bỏ kiểm tra quyền để các màn hình tạm chạy.

### Quyền sở hữu giúp giảm conflict

Mỗi người chỉ sửa file được giao trong README, test cùng chức năng và resource có tiền tố riêng: auth_/notify_ cho An, student_ cho Quang, subject_/month_ cho Sáng, booking_ cho Thu, schedule_/agenda_ cho Huy. Dùng strings_auth.xml, strings_student.xml… thay vì cùng sửa strings.xml. ID layout cũng có tiền tố tương ứng.

File chung gồm Gradle, Manifest, DBHelper, DbContract, models, MainActivity, BaseActivity, hai HomeActivity, AppConstants, DbExecutor, DateTimeHelper, theme và README. Sáng gom thay đổi thành PR chung; không tính đóng góp chức năng. Không tạo lại project hoặc format toàn repository trên nhánh cá nhân.

### Có thể code song song ngay sau khung

Cả năm người bắt đầu cùng lúc từ commit M0. Khi hàm của người khác chưa xong, dùng dữ liệu giả hoặc phiên giả chỉ trong test và bản debug để làm giao diện, kiểm tra logic phần mình. Giữ nguyên đầu vào và đầu ra ở phần 04; không tự viết lại chức năng của người khác. Ghép dữ liệu thật theo M1 và bỏ mọi đường chạy giả khỏi bản bàn giao. Code song song, nhưng merge lần lượt để dễ kiểm tra.

<!-- PAGE -->

## 04 Cách các phần gọi nhau

Repository nhận Context trong constructor và chỉ làm việc với dữ liệu. Hàm đọc trả model hoặc List; hàm ghi và đăng nhập trả ActionResult gồm ok, code, message, id. id là bản ghi vừa tạo hoặc vừa thao tác; khi lỗi là -1. Không đưa password ra model dùng để hiển thị.

| File public | Hàm cần chốt trong khung | Người triển khai |
| --- | --- | --- |
| AuthRepository | login(username, password); logout() | An |
| SessionManager | getSession(); clearSession() | An |
| AuthGuard | requireAdmin(); requireStudent() | An |
| PasswordHelper | hash(rawPassword); verify(rawPassword, savedHash) | An |
| StudentRepository | list(keyword); get(id); create(draft); update(id, draft); delete(id) | Quang |
| SubjectRepository | list(keyword); get(id); create(draft); update(id, draft); delete(id) | Sáng |
| BookingRepository | save(scheduleId, draft) | Thu |
| ScheduleRepository | listMine(filter); getMine(id); setStatus(id, status); deleteMine(id) | Huy |
| NotificationHelper | refresh(context); showChange(context, studentId, scheduleId) | An |

### Dữ liệu truyền qua các hàm

- Session: accountId, studentId, username, role. Với Admin, studentId = -1. getSession có thể trả null; phiên khôi phục phải được đối chiếu lại với Account và Student trước khi dùng.
- StudentDraft: studentCode, fullname, email, phone, username, password. Hai trường cuối chỉ dùng khi tạo mới. SubjectDraft: subjectCode, subjectName, description, credits.
- BookingDraft: subjectId, studyDate, startTime, endTime, location. save nhận scheduleId = -1 khi tạo mới; khi sửa nhận id đã có. studentId luôn lấy từ phiên đã kiểm tra, status tạo mới luôn là PENDING.
- ScheduleFilter: keyword, fromDate, toDate, status. Khoảng ngày lấy cả hai đầu; trường trống nghĩa là không lọc. keyword tìm theo mã hoặc tên môn và địa điểm. Kết quả kèm tên môn, sắp theo ngày, giờ bắt đầu rồi id.
- Hàm get trả null khi không tìm thấy hoặc không được truy cập. Danh sách không trả null. Hàm ghi dùng mã OK, VALIDATION, DUPLICATE, CONFLICT, FORBIDDEN, NOT_FOUND, IN_USE, DB_ERROR; không hiển thị lỗi SQL thô.

### Điều hướng không truyền cả đối tượng

Khung định nghĩa EXTRA_STUDENT_ID, EXTRA_SUBJECT_ID, EXTRA_SCHEDULE_ID và EXTRA_SELECTED_DATE. Sửa lịch mở BookingFormActivity với scheduleId; đặt mới từ lịch biểu truyền selectedDate. Chi tiết lịch mở bằng scheduleId và vẫn phải kiểm tra chủ sở hữu.

SubjectPickerActivity trả subjectId bằng Activity Result API. BookingFormActivity tự tải lại tên môn từ id. Khi quay về danh sách hoặc lịch biểu, onResume tải lại dữ liệu; cách này đơn giản và đủ cho quy mô bài. Không ghi database trong onResume.

Admin vào trang quản lý sinh viên và môn học. Student vào danh sách lịch, đặt lịch và lịch biểu; từ lịch biểu có nút chuyển ngày tuần hoặc tháng. Các nút này được đặt sẵn trong khung. Mỗi repository vẫn kiểm tra quyền, không chỉ ẩn nút trên giao diện.

<!-- PAGE -->

## 05 Việc của An và Quang

### An phụ trách đăng nhập phiên và thông báo

| Đầu việc | Đơn vị |
| --- | --- |
| Màn hình login, kiểm tra mật khẩu, điều hướng theo vai trò | 5 |
| SharedPreferences, khôi phục phiên, logout và kiểm tra quyền | 4 |
| PasswordHelper dùng chung cho tạo tài khoản và đăng nhập | 2 |
| Nhắc lịch sắp tới, báo thay đổi, hủy và cập nhật nhắc cũ | 6 |
| Kiểm thử phần trên và chuẩn bị demo | 3 |
| Tổng | 20 |

Login kiểm tra tài khoản trong SQLite. Phiên lưu ở SharedPreferences chế độ riêng tư, gồm accountId, studentId, username, role; mỗi lần mở app kiểm tra lại dữ liệu gốc. Sai mật khẩu, tài khoản bị xóa hoặc Student không có hồ sơ thì không đăng nhập. Logout xóa phiên, xóa back stack và hủy nhắc của người vừa đăng xuất.

Thông báo dùng WorkManager, một công việc riêng cho mỗi lịch PENDING hoặc CONFIRMED của Student đang đăng nhập, nhắc trước 15 phút. refresh hủy các công việc nhắc cũ theo tag rồi tạo lại từ database; chạy tuần tự và chờ thao tác hủy hoàn tất trước khi tạo mới. Nếu còn dưới 15 phút nhưng chưa tới giờ học thì lên lịch chạy sớm nhất có thể; lịch đã bắt đầu không tạo nhắc mới.

Worker đọc lại phiên và lịch trước khi gửi, kiểm tra đúng chủ sở hữu, trạng thái còn hiệu lực và ngày giờ còn khớp với dữ liệu lúc lên lịch. Tạo notification channel; xin POST_NOTIFICATIONS trên Android 13 trở lên. Từ chối quyền không làm lỗi thao tác đặt lịch. WorkManager có thể chạy trễ do hệ thống; bài này không cam kết báo chính xác từng phút.

showChange chỉ gửi thông báo thay đổi cho Student đang đăng nhập và có studentId khớp. Thu và Huy gọi sau khi ghi lịch thành công; lỗi thông báo không rollback dữ liệu. refresh được gọi sau login, logout, thay đổi lịch, xóa sinh viên, mở lại app và khi nhận thay đổi giờ hoặc múi giờ qua receiver đã đăng ký trong khung.

Nghiệm thu: đúng hai vai trò; tắt mở app còn phiên hợp lệ; logout không quay lại trang riêng; nhắc cũ biến mất sau sửa, hủy, xóa; chuyển tài khoản không thấy thông báo của người trước. Bàn giao PasswordHelper và AuthGuard sớm cho Quang, Thu, Huy.

### Quang phụ trách sinh viên và tài khoản đi kèm

| Đầu việc | Đơn vị |
| --- | --- |
| Danh sách, tìm kiếm, chi tiết sinh viên | 5 |
| Form thêm và sửa với kiểm tra đầu vào | 5 |
| Tạo Account và Student nguyên tử, dùng PasswordHelper | 4 |
| Xóa tài khoản sinh viên và lịch liên quan có xác nhận | 3 |
| Kiểm thử CRUD, rollback, quyền Admin và demo | 3 |
| Tổng | 20 |

Tìm theo mã, họ tên hoặc email. Khi tạo mới, nhập cả username và password; dùng transaction và báo rõ lỗi trùng mã hay username. Khi xóa, xác nhận số lịch mất, xóa đúng tài khoản STUDENT rồi gọi refresh. Nghiệm thu bằng cách tạo sinh viên, đăng nhập tài khoản vừa tạo, sửa hồ sơ, tìm lại và xóa; thử lỗi tạo hồ sơ để chứng minh không còn Account mồ côi.

<!-- PAGE -->

## 06 Việc của Sáng và Thu

### Sáng phụ trách môn học chọn môn và lịch tháng

| Đầu việc | Đơn vị |
| --- | --- |
| Danh sách, tìm kiếm, thêm, sửa và xem môn học | 7 |
| Xóa môn có kiểm tra đang được dùng, kiểm tra dữ liệu | 3 |
| Màn hình chọn môn trả subjectId cho Thu | 3 |
| Lịch tháng, chọn ngày và mở lịch hoặc form đặt lịch | 5 |
| Kiểm thử chức năng và demo | 2 |
| Tổng | 20 |

Môn học được tìm theo mã hoặc tên. Admin được CRUD; Student chỉ đọc và chọn. Mã môn không trùng, tên bắt buộc, credits là số nguyên dương. Môn đang có lịch thì từ chối xóa, không tự xóa lịch của sinh viên.

Lịch tháng dùng giao diện lưới đơn giản, không cần thư viện lịch ngoài. Ô ngày có số lịch PENDING và CONFIRMED; chọn ngày sẽ hiện các lịch của ngày đó hoặc mở màn hình ngày của Huy. Có nút tháng trước, tháng sau, hôm nay và đặt lịch ở ngày đã chọn. Chỉ lấy lịch của Student hiện tại qua ScheduleRepository; không tự viết một bảng lịch khác.

Nghiệm thu: CRUD môn đủ; chọn môn trả đúng id; không xóa môn đang dùng; tháng có 28, 29, 30, 31 ngày hiển thị đúng; chọn ngày mở đúng dữ liệu. Khi Huy chưa hoàn thiện repository, dùng dữ liệu giả trong test hoặc debug của riêng chức năng, không merge dữ liệu giả vào luồng release.

Vai trò trưởng nhóm của Sáng là chốt khung chung với nhóm, gom yêu cầu sửa file chung và điều phối PR. Việc đó không được tính vào 20 đơn vị trên và không thay cho chức năng phải demo.

### Thu phụ trách đặt sửa và chống trùng lịch

| Đầu việc | Đơn vị |
| --- | --- |
| Form đặt lịch, chọn môn, ngày, giờ và địa điểm | 5 |
| Form sửa lịch và giữ dữ liệu khi nhập sai | 3 |
| Kiểm tra ngày giờ, chủ sở hữu và lịch trùng trong transaction | 7 |
| Nối thông báo sau khi lưu thành công | 1 |
| Kiểm thử biên, chống lưu hai lần và demo | 4 |
| Tổng | 20 |

Tạo và sửa dùng chung BookingFormActivity, gọi SubjectPickerActivity của Sáng. Không cho chọn sinh viên hay truyền status tùy ý. Chặn lịch mới trong quá khứ; chỉ sửa lịch chưa bắt đầu và chưa CANCELLED. Giữ nguyên trạng thái PENDING hoặc CONFIRMED khi sửa, không tự đổi trạng thái.

BookingRepository.save kiểm tra đầu vào, lấy studentId từ phiên và xác minh bản ghi khi sửa. Kiểm tra trùng rồi insert hoặc update trong cùng một transaction; không chỉ kiểm tra tại form. Sửa thì loại trừ chính id đang sửa. Khóa nút lưu trong lúc xử lý để tránh nhấn hai lần.

Sau commit gọi NotificationHelper.refresh và showChange. Tạo mới hiển thị xác nhận trong app; thông báo thay đổi dành cho sửa. Nghiệm thu bằng các ca ở phần 08, đặc biệt ca lịch bao trùm, lịch sát nhau và sửa lịch của người khác. Bàn giao BookingValidator cùng ca kiểm thử trước khi ghép UI đầy đủ.

<!-- PAGE -->

## 07 Việc của Huy và cách nối lịch biểu

### Huy phụ trách xem và quản lý lịch cá nhân

| Đầu việc | Đơn vị |
| --- | --- |
| Danh sách, tìm kiếm, lọc, chi tiết lịch của mình | 6 |
| Đổi trạng thái và xóa lịch có xác nhận | 4 |
| Lịch ngày và tuần, chuyển ngày tuần, mở chi tiết | 6 |
| Truy vấn chung cho lịch biểu, nối form Thu và thông báo An | 1 |
| Kiểm thử quyền sở hữu, bộ lọc, trạng thái và demo | 3 |
| Tổng | 20 |

ScheduleRepository chỉ trả lịch của Student đang đăng nhập. Danh sách có tìm theo tên hoặc mã môn, địa điểm; lọc khoảng ngày và trạng thái. Chi tiết thể hiện môn, ngày, giờ, địa điểm và trạng thái. Có nút đặt mới, sửa, xác nhận, hủy và xóa tùy tình trạng lịch.

PENDING được chuyển sang CONFIRMED hoặc CANCELLED; CONFIRMED được chuyển sang CANCELLED; CANCELLED chỉ xem hoặc xóa. Xóa là xóa hẳn bản ghi sau xác nhận. Hủy vẫn giữ bản ghi để xem lại. Chỉ đổi trạng thái với lịch chưa bắt đầu; lịch đã bắt đầu được xem hoặc xóa. Kiểm tra này ở repository, không chỉ ở nút bấm.

Mọi câu UPDATE hoặc DELETE dùng cả id và studentId trong điều kiện. Không tìm thấy bản ghi của mình thì trả NOT_FOUND. Dùng transaction khi đọc trạng thái để quyết định chuyển rồi cập nhật. Sau commit gọi refresh và showChange với studentId đã kiểm tra; khi xóa vẫn giữ studentId trong biến để gửi tín hiệu đúng người.

### Lịch ngày và tuần làm theo cách đơn giản

Chế độ ngày là danh sách lịch của một ngày, sắp theo giờ. Chế độ tuần là bảy nhóm ngày từ thứ Hai đến Chủ nhật, không cần vẽ thời khóa biểu nhiều cột. Có ngày hoặc tuần trước, sau, hôm nay; chạm vào một lịch mở chi tiết. Nút thêm mở form Thu với ngày đang chọn.

Lịch biểu mặc định hiển thị PENDING và CONFIRMED. Lịch CANCELLED vẫn tìm được trong danh sách qua bộ lọc. Mỗi trạng thái có chữ rõ ràng, không chỉ phân biệt bằng màu. Khi không có dữ liệu, hiển thị trạng thái trống và nút đặt lịch.

### Hợp đồng giữa Huy Sáng và Thu

- Huy hoàn thiện listMine(filter) sớm để Sáng dùng cho lịch tháng. Cả lịch tháng lẫn ngày tuần đều dùng cùng dữ liệu, cùng ngày yyyy-MM-dd và cùng cách tính số lịch đang hiệu lực.
- Sáng sở hữu MonthCalendarActivity; Huy sở hữu DayWeekCalendarActivity. Không cùng sửa một CalendarActivity. Các nút chuyển màn hình và extras đã có trong khung.
- Thu sở hữu toàn bộ form thêm và sửa. Huy chỉ mở form bằng id hoặc ngày đã chọn; không sao chép form hay logic chống trùng.
- Sau khi quay về màn hình, tải lại bằng onResume để thấy thay đổi ngay. Không cần event bus, service tự viết hay kiến trúc nhiều module.

Nghiệm thu: Student A không đọc hoặc sửa lịch Student B dù cố truyền id; tìm kiếm kết hợp lọc đúng; đổi trạng thái đúng quy tắc; xóa có xác nhận; ngày tuần và tháng cùng hiển thị một bộ lịch sau khi Thu lưu.

<!-- PAGE -->

## 08 Quy tắc đặt lịch thống nhất

Những quy tắc dưới đây là cách nhóm xử lý các điểm đề chưa nêu chi tiết. Chốt trước khi code để năm phần không tự hiểu khác nhau. Admin không duyệt lịch; Student tự xác nhận hoặc hủy lịch của mình.

### Khi nào hai lịch bị trùng

Chỉ xét cùng studentId, cùng studyDate và lịch hiện có đang PENDING hoặc CONFIRMED. Hai khoảng giờ trùng nếu giờ bắt đầu mới nhỏ hơn giờ kết thúc cũ và giờ kết thúc mới lớn hơn giờ bắt đầu cũ. Địa điểm hoặc môn khác nhau vẫn bị trùng. Hai sinh viên khác nhau được học cùng giờ; đề không yêu cầu quản lý sức chứa phòng.

```sql
SELECT COUNT(*) FROM StudySchedule
WHERE studentId = ? AND studyDate = ?
  AND status IN ('PENDING', 'CONFIRMED')
  AND startTime < ? AND endTime > ?
  AND id <> ?;
```

Thứ tự tham số: studentId hiện tại, ngày mới, endTime mới, startTime mới, id đang sửa. Tạo mới dùng id = -1. Dùng tham số bind, không nối trực tiếp chuỗi nhập vào SQL. So sánh chuỗi giờ chỉ đúng khi toàn bộ dữ liệu có định dạng HH:mm cố định.

### Thứ tự xử lý khi lưu

1. Kiểm tra phiên Student hợp lệ; lấy studentId từ phiên.
2. Kiểm tra môn còn tồn tại, ngày giờ hợp lệ, chưa ở quá khứ và địa điểm có nội dung.
3. Bắt đầu transaction; nếu sửa thì tải lại lịch theo cả id và studentId, kiểm tra chưa bắt đầu và chưa hủy.
4. Chạy truy vấn trùng lịch. Có trùng thì trả CONFLICT và rollback; không trùng thì insert hoặc update.
5. Commit rồi kết thúc transaction. Mọi lỗi phải đi qua finally để đóng transaction; dùng try-with-resources cho Cursor.
6. Chỉ sau commit mới cập nhật nhắc lịch và giao diện. Lỗi thông báo không làm mất lịch đã lưu.

| Lịch đã có | Lịch muốn lưu | Kết quả |
| --- | --- | --- |
| 09:00 đến 10:00 | 09:30 đến 10:30 | Chặn vì giao nhau |
| 09:00 đến 10:00 | 08:00 đến 11:00 | Chặn vì bao trùm |
| 09:00 đến 10:00 | 09:00 đến 10:00 | Chặn vì giống hệt |
| 09:00 đến 10:00 | 10:00 đến 11:00 | Cho phép sát giờ |
| 09:00 đến 10:00 CANCELLED | 09:15 đến 09:45 | Cho phép |
| 09:00 đến 10:00 của A | Cùng giờ của B | Cho phép |
| Chính lịch đang sửa | Giữ nguyên giờ của nó | Cho phép nếu không đụng lịch khác |

Ngày mới phải có thời điểm bắt đầu sau thời điểm hiện tại. Giờ bắt đầu bằng hoặc sau giờ kết thúc bị từ chối. Không hỗ trợ lịch qua nửa đêm hoặc lặp định kỳ trong phạm vi ban đầu.

<!-- PAGE -->

## 09 Trình tự làm việc và merge

### Các mốc ngắn và rõ

| Mốc | Công việc | Điều kiện qua mốc |
| --- | --- | --- |
| M0 Khung chung | Cả nhóm chốt schema, chữ ký hàm, màn hình, dependency và quyền sửa file | Cả 5 máy build và chạy được cùng một commit |
| M1 Làm song song | An giao AuthGuard và PasswordHelper; Sáng giao chọn môn; Huy giao listMine; Thu giao validator; Quang giao tạo tài khoản | Các điểm nối chạy với dữ liệu thật, không cần đợi toàn bộ UI |
| M2 Hoàn thiện | Mỗi người hoàn thành checklist và test chức năng của mình | PR nhỏ, build được, có bằng chứng chạy |
| M3 Ghép luồng | Sáng điều phối merge từng PR, người viết sửa lỗi của mình | Luồng Admin tạo dữ liệu rồi Student đặt lịch chạy liên tục |
| M4 Chốt bài | Cả nhóm chạy bảng nghiệm thu, sửa lỗi, tập demo | Không còn lỗi chặn, tạo APK và chốt commit |

M0 làm xong trước khi chia nhánh chức năng. Có thể dự kiến 1 đến 2 buổi cho code song song và 1 buổi ghép, kiểm thử; đây là ước lượng ban đầu, không phải thời hạn đã cam kết. Đề chấm theo thời gian nên ưu tiên hoàn thành luồng thật sớm, tránh dành nhiều thời gian trang trí.

### Nhánh và quy tắc gửi PR

Nhánh chính dùng main. Nhánh cá nhân: feature/an-auth-notify, feature/quang-student, feature/sang-subject-month, feature/thu-booking, feature/huy-schedule-agenda. Tất cả tạo từ commit M0; không tự tạo project Android riêng rồi chép đè vào repository.

```text
git switch main
git pull --ff-only
git switch -c feature/ten-phan-viec

# Trước khi gửi hoặc cập nhật PR
git fetch origin
git merge origin/main
```

Tự giải quyết conflict trong file mình sở hữu. Nếu conflict ở file chung, dừng phần đó và trao đổi với Sáng để gom thay đổi; không chọn nhận toàn bộ một phía khi chưa đọc. Không force push lên main. Không commit local.properties, thư mục build, .gradle, mật khẩu máy hay file cấu hình IDE cá nhân.

### Thứ tự merge giúp giảm phụ thuộc

1. PR chức năng nền của An gồm auth, session và password; Quang và Huy lấy main mới. An tiếp tục thông báo ở PR sau.
2. PR môn học và chọn môn của Sáng; PR sinh viên của Quang có thể ghép ngay sau đó.
3. PR truy vấn và màn hình danh sách của Huy, sau đó PR đặt sửa lịch của Thu.
4. PR còn lại của Huy về trạng thái và ngày tuần; PR lịch tháng của Sáng; PR thông báo của An.
5. PR sửa lỗi tích hợp theo người sở hữu chức năng. Đổi schema hoặc chữ ký hàm phải đi bằng PR chung trước, rồi mọi người cập nhật nhánh.

Mỗi PR ghi rõ chức năng, file chung muốn đổi, cách thử, ảnh hoặc video ngắn và phần còn thiếu. Tối thiểu một người khác xem; PR của Sáng do một thành viên khác review. Build bằng gradlew.bat assembleDebug và chạy test thích hợp trước khi merge. Tránh auto-format hoặc đổi tên hàng loạt.

<!-- PAGE -->

## 10 Nghiệm thu và bàn giao

### Bảng kiểm chung trước khi chốt

| Ca kiểm thử | Kết quả cần có | Người chứng minh |
| --- | --- | --- |
| Login đúng sai, hai vai trò, mở lại app, logout | Đúng quyền, nhớ phiên hợp lệ, không vào lại trang cũ | An |
| Nhắc sắp tới, đổi giờ, hủy, xóa, đổi người dùng, từ chối quyền | Không nhắc lịch cũ hoặc lịch người khác; app vẫn lưu được | An |
| Thêm sửa tìm xem xóa sinh viên | Dữ liệu đúng; tạo được tài khoản đăng nhập; không có dữ liệu mồ côi | Quang |
| Xóa sinh viên đã có lịch, Student thử mở trang Admin | Cascade đúng sau xác nhận; chặn sai quyền | Quang |
| Thêm sửa tìm xem xóa môn, chọn môn | Đủ CRUD; chặn mã trùng và xóa môn đang dùng | Sáng |
| Lịch tháng và chọn ngày | Đúng số ngày, đúng lịch của mình, mở đúng ngày | Sáng |
| Đặt sửa lịch, ngày giờ sai, lịch trùng và sát giờ | Đúng toàn bộ ca ở phần 08; không tạo hai lịch do bấm lặp | Thu |
| Đọc sửa lịch người khác, mở bản ghi đã bị xóa | Không lộ dữ liệu, không crash, báo lỗi rõ | Thu và Huy |
| Tìm lọc chi tiết, trạng thái, hủy và xóa lịch | Đúng chủ sở hữu, đúng chuyển trạng thái, có xác nhận xóa | Huy |
| Lịch ngày tuần tháng sau tạo sửa và hủy | Cùng dữ liệu, tự tải lại khi quay về | Huy và Sáng |
| Đóng mở app và dùng hai tài khoản Student | Dữ liệu SQLite còn; phiên và danh sách không lẫn nhau | Cả nhóm |

Chạy trên ít nhất một máy hoặc emulator thuộc Android 13 trở lên để thử quyền thông báo. Với logic chống trùng và trạng thái, có test tự động các trường hợp biên; với CRUD và điều hướng, dùng checklist cùng ảnh hoặc video. Thời điểm WorkManager chạy có thể trễ; khi demo thông báo dùng lịch gần hiện tại, thiết bị đang hoạt động và ghi nhận thời gian thực tế.

### Kịch bản demo gọn

Admin đăng nhập → tạo một sinh viên và môn → logout → Student vừa tạo đăng nhập → đặt lịch hợp lệ → thử đặt lịch trùng và bị chặn → xem chi tiết rồi sửa giờ → xác nhận và xem ngày tuần tháng → nhận nhắc hoặc thông báo thay đổi → hủy lịch và kiểm tra nhắc cũ đã hủy → mở lại app kiểm tra dữ liệu và phiên. Sau đó Admin minh họa tìm sửa xóa sinh viên và môn chưa được dùng.

### Điều kiện một người hoàn thành phần việc

Code đã merge; không để stub trên luồng của mình; tự chạy các ca được giao; người khác có thể checkout và chạy theo README; có commit hoặc PR thể hiện đóng góp; demo được phần chức năng mà không cần người khác thao tác hộ. Đóng góp đánh giá bằng phần chạy được và kiểm thử, không theo số dòng code, số commit hay việc dựng khung.

Sáng chốt README hướng dẫn mở project, bộ SDK và dependency đã dùng, tài khoản demo, cách build, cách tạo lại dữ liệu demo và phân công. Cả nhóm bàn giao repository GitHub hoặc GitLab, APK debug, kế hoạch này và bằng chứng kiểm thử. Chỉ reset database cho demo bằng thao tác debug có chủ đích; không xóa dữ liệu mỗi lần mở app.

### Tài liệu đối chiếu

Yêu cầu gốc: Test3.docx.md do nhóm cung cấp. Các mục đăng nhập, SharedPreferences, SQLite, CRUD, booking, chống trùng, trạng thái, lịch biểu, thông báo và cộng tác Git đều được phân công ở trên.

- SharedPreferences: https://developer.android.com/training/data-storage/shared-preferences
- WorkManager và thời điểm thực thi: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work
- Quyền thông báo: https://developer.android.com/develop/ui/views/notifications/notification-permission
