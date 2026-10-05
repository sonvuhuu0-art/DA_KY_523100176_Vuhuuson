# Ứng Dụng Quản Lý & Đặt Lịch Sân Bóng (Desktop)

Ứng dụng desktop viết bằng **Java 17 + JavaFX**, dữ liệu lưu trong **SQLite**
(file `data.db` nằm cùng thư mục với file chạy), không cần cài server database
riêng. Đóng gói thành 1 file `.jar` chạy độc lập bằng `maven-shade-plugin`.

## Yêu cầu môi trường (để build)

- JDK 17 trở lên (khuyên dùng JDK 21 LTS)
- Maven 3.9+ (nếu máy chưa có Maven, xem mục "Không có Maven?" bên dưới)

## Cấu trúc dự án

```
src/main/java/com/sanbong/
  ├── model/        POJO ứng với từng bảng trong database
  ├── dao/           lớp truy cập dữ liệu (JDBC thuần, PreparedStatement)
  ├── service/        nghiệp vụ (đặt sân, xác thực, thanh toán)
  ├── controller/     controller JavaFX cho màn hình khách hàng
  ├── controller/admin controller JavaFX cho màn hình quản trị
  ├── db/             khởi tạo & seed dữ liệu mẫu cho SQLite
  ├── util/            kết nối DB, quản lý phiên đăng nhập, chuyển màn hình
  └── exception/       exception nghiệp vụ (VD: SlotAlreadyBookedException)
src/main/resources/
  ├── fxml/            giao diện khách hàng (login, danh sách sân, đặt sân...)
  ├── fxml/admin/       giao diện quản trị (dashboard, quản lý sân/lịch/khách)
  └── css/styles.css    giao diện dùng chung
```

## Chạy ứng dụng khi đang phát triển

```bash
mvn javafx:run
```

Lần chạy đầu tiên sẽ tự động tạo file `data.db` và seed sẵn dữ liệu mẫu:

- **Admin**: `admin@sanbong.local` / `admin123`
- **Chủ sân** (đã duyệt): `chusan1@sanbong.local`, `chusan2@sanbong.local` / `owner123`
- **Chủ sân** (đang chờ duyệt, để demo luồng phê duyệt): `chusan3@sanbong.local` / `owner123`
- **Khách hàng**: `khach1@sanbong.local` ... `khach5@sanbong.local` / `123456`

## Đóng gói thành 1 file `.jar` chạy độc lập

```bash
mvn clean package
```

File kết quả: `target/san-bong-app.jar` (đã gộp toàn bộ thư viện JavaFX,
sqlite-jdbc, jBCrypt nhờ `maven-shade-plugin`). Chạy bằng:

```bash
java -jar target/san-bong-app.jar
```

File `data.db` sẽ tự tạo ngay cạnh file `.jar` khi chạy lần đầu — copy cả 2
file (`.jar` + `data.db` sau khi đã chạy 1 lần, nếu muốn giữ dữ liệu) sang máy
khác là chạy được ngay, không cần cài Java thêm gì ngoài JRE 17+.

> Lưu ý: fat-jar này gọi vào một lớp `Launcher` (không kế thừa
> `javafx.application.Application`) làm Main-Class, để tránh lỗi kinh điển
> "JavaFX runtime components are missing" khi chạy jar đóng gói.

## Shortcut chạy nhanh trên Desktop

Đã tạo sẵn shortcut **"Quan Ly Dat San Bong"** trên Desktop, trỏ vào
`dist/san-bong-app.jar` (không phải `target/`, vì `mvn clean` sẽ xóa sạch
thư mục `target/` — bao gồm cả `data.db` và ảnh đã upload — mỗi lần build
lại). Sau khi sửa code và build lại, cần copy file jar mới vào `dist/` để
shortcut chạy đúng bản mới nhất:

```bash
mvn clean package
cp target/san-bong-app.jar dist/san-bong-app.jar
```

`dist/data.db` và `dist/images/` sẽ giữ nguyên qua các lần build vì chỉ
file `.jar` bị ghi đè, không đụng tới `target/`.

## Đóng gói thành file cài đặt `.exe` (không cần máy đích cài Java)

Dùng `jpackage` (có sẵn từ JDK 14+) sau khi đã build fat-jar ở trên:

```bash
jpackage ^
  --name "SanBongApp" ^
  --input target ^
  --main-jar san-bong-app.jar ^
  --main-class com.sanbong.Launcher ^
  --type exe ^
  --win-shortcut ^
  --win-menu ^
  --icon src\main\resources\images\app-icon.ico
```

Kết quả là 1 file `.exe` cài đặt sẵn JVM, double-click chạy ngay trên máy
không có Java — phù hợp cho buổi bảo vệ đồ án. Nên build và thử chạy trên
một máy khác (không phải máy đang code) ít nhất vài ngày trước khi nộp.

## Không có Maven?

Nếu máy chưa cài Maven, tải bản zip từ
`https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip`,
giải nén, rồi chạy `mvn.cmd` bằng đường dẫn đầy đủ tới thư mục `bin` vừa giải
nén thay vì gõ `mvn` trực tiếp.

## Tài khoản & vai trò

Ứng dụng theo mô hình **nền tảng đa chủ sân** (nhiều chủ sân cùng đăng ký sử
dụng chung 1 hệ thống, giống 1 sàn trung gian) chứ không phải "1 chủ sân duy
nhất". Ba vai trò:

- **customer** (khách hàng): xem danh sách sân từ mọi chủ sân (lọc theo loại
  sân/cụm sân, xem điểm đánh giá trung bình), xem khung giờ trống theo ngày,
  đặt sân, xem/hủy lịch đã đặt (chỉ hủy được nếu còn cách giờ đặt tối thiểu 2
  tiếng), đánh giá sao + nhận xét sau khi lịch đặt đã hoàn thành, quản lý hồ
  sơ cá nhân (sửa họ tên/SĐT, đổi mật khẩu). Đăng ký xong dùng được ngay.

- **venue_owner** (chủ sân): tự quản lý cụm sân/sân/khung giờ giá **của
  riêng mình**, xử lý lịch đặt cho sân mình (xác nhận/hủy/đánh dấu hoàn
  thành/ghi nhận thanh toán/đặt hộ khách), xem dashboard doanh thu riêng —
  hoàn toàn không thấy được dữ liệu của chủ sân khác. Đăng ký ở màn hình
  đăng ký (tick "Đăng ký làm chủ sân") sẽ ở trạng thái **chờ duyệt**, phải
  được admin phê duyệt (màn "Quản lý chủ sân") mới đăng nhập được.

- **admin** (quản trị hệ thống): duyệt/khóa tài khoản chủ sân, khóa/mở tài
  khoản khách hàng, đồng thời có toàn quyền xem/quản lý **mọi** cụm sân, sân,
  lịch đặt và dashboard trên toàn hệ thống (không chỉ 1 chủ) để giám sát và
  xử lý tranh chấp khi cần. Khi tạo/sửa cụm sân, admin chọn cụm đó thuộc về
  chủ sân nào qua ô "Chủ sân".

Về mặt kỹ thuật, 4 màn hình Tổng quan / Quản lý cụm sân / Quản lý sân / Quản
lý lịch đặt dùng chung 1 bộ controller cho cả admin lẫn chủ sân — chỉ khác
nhau ở phạm vi dữ liệu (`SessionManager.ownerScope()`: `null` = toàn hệ
thống cho admin, id của chủ sân đang đăng nhập cho venue_owner).

## Chống trùng lịch

Bảng `bookings` có ràng buộc `UNIQUE(field_id, time_slot_id, booking_date)`
ở tầng SQLite, và `BookingService.createBooking` kiểm tra trùng lịch + insert
trong cùng 1 transaction JDBC (`setAutoCommit(false)` ... `commit`/`rollback`)
để tránh race condition khi có 2 thao tác đặt sân xảy ra gần như đồng thời.
