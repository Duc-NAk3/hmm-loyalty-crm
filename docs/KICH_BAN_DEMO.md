# Kịch bản demo (khoảng 10 phút) – Hoa Mai Mart Loyalty CRM

Mục tiêu: cho thấy một vòng khép kín **bán hàng tại quầy → định danh khách → tích / đổi điểm an toàn → khách hàng 360 → quản trị giá và dữ liệu → số liệu cho quản lý**, trên đúng 19 chức năng Demo của SRS.

---

## Chuẩn bị (trước buổi demo khoảng 10 phút)

1. **Tạo lại dữ liệu sạch** (README mục 2): `DROP DATABASE hmm_loyalty WITH (FORCE)`, `CREATE DATABASE hmm_loyalty`, chạy backend, chờ log `Demo data seeded`; chạy frontend.
2. **Mở 3 cửa sổ trình duyệt** cạnh nhau (dùng cửa sổ ẩn danh để các phiên không đè nhau):

   | Cửa sổ | Mở | Đăng nhập |
   |---|---|---|
   | **A – POS** | http://localhost:5173 → POS tại quầy → chọn quầy `HM001-Q01` | `tn.hm001.1` (chưa mở ca) |
   | **B – Portal quản trị** | http://localhost:5173 → Web Portal | `admin` |
   | **C – Portal danh mục** (ẩn danh) | Web Portal | `danhmuc` |

3. Để sẵn thư mục `sample-data/` (file `customers_POS_ALPHA_HM001.csv`).
4. Mật khẩu mọi tài khoản: `Demo@123`.

**Mã vạch dùng trong buổi demo** (gõ hoặc dán vào ô quét rồi Enter):

| Mã vạch | Mặt hàng | Giá | Ghi chú |
|---|---|---|---|
| `8931002000144` | Thịt ba chỉ heo 500g | 75.000 | |
| `8931003000266` | Sữa tươi tiệt trùng có đường 1L | 34.000 | |
| `8931001000015` | Cải ngọt Đà Lạt 500g (RAU001) | 18.000 | dùng ở bước đổi giá |
| `8931004000401` | Sữa công thức pha sẵn 180ml lốc 4 | 72.000 | **hạn chế khuyến mại**: không tích, không đổi điểm |

Phím tắt POS: **F2** ô quét mã · **F4** tra cứu thành viên · **F9** thanh toán.

---

## Diễn biến

| # | Thời gian | Nội dung | FR |
|---|---|---|---|
| 0 | 0:30 | Giới thiệu bối cảnh | |
| 1 | 0:45 | Mở ca tại quầy | FR-01 |
| 2 | 1:30 | Khách mới: bán hàng, đăng ký nhanh, tích điểm | FR-02, 03, 19, 28 |
| 3 | 2:00 | Khách Kim cương đổi điểm có OTP lần đầu | FR-18, 20, 30, 04, 28 |
| 4 | 0:30 | Khách bị khóa đổi điểm; hủy đổi điểm trả lại điểm | FR-30 |
| 5 | 1:15 | Customer 360 và sổ cái điểm | FR-22, 33 |
| 6 | 1:15 | Đổi giá có duyệt | FR-11, 12 |
| 7 | 1:00 | Chuyển đổi dữ liệu từ POS cũ | FR-58, 59, 60 |
| 8 | 0:45 | Dashboard cho quản lý | FR-51, 31 |
| 9 | 0:30 | Phân quyền | FR-65 |

### 0. Giới thiệu (30 giây)
"HMM có 3 cửa hàng dùng 3 phần mềm POS khác nhau, không biết ai là khách quen. Hệ thống mới thay cả 3 POS, mỗi hóa đơn đều có thể gắn thành viên; điểm và hạng dùng chung toàn chuỗi."

### 1. Mở ca (cửa sổ A, 45 giây)
1. Đăng nhập `tn.hm001.1` → màn **Mở ca bán hàng** → giữ tiền đầu ca 2.000.000 → **Mở ca**.
2. Nói: mỗi quầy chỉ một ca mở; thu ngân chỉ đăng nhập được ở quầy thuộc cửa hàng mình.

### 2. Khách mới – đăng ký nhanh và tích điểm (cửa sổ A, 1 phút 30)
1. Quét `8931002000144`, `8931003000266`, `8931001000015` → tổng **127.000đ**.
2. **F4**, gõ `0987654321`, Enter → chưa là thành viên → form **Đăng ký thành viên nhanh** mở, SĐT điền sẵn.
3. Nhập họ tên (ví dụ `Lê Thu Hà`), tick đồng ý nhận SMS → **Đăng ký và gắn vào hóa đơn**.
   Thẻ thành viên: mã `HM1…`, hạng Thành viên ×1, 0 điểm, nhãn **Chưa xác thực**.
4. Bấm **Điện thoại khách (giả lập)** → tin **chào mừng** kèm mã thành viên. Đóng lại.
5. **F9** → Enter (khách đưa đúng 127.000) → hóa đơn: **Điểm cộng +12**, số dư 12.
   Nói: đăng ký không cần OTP để không làm chậm quầy; SĐT sẽ được xác thực ở lần đổi điểm đầu tiên.
6. **Hóa đơn mới (Enter).**

### 3. Khách Kim cương đổi điểm bằng OTP (cửa sổ A, 2 phút)
1. Quét `8931002000144`, `8931003000266`, `8931004000401` → tổng **181.000đ**; dòng sữa công thức có nhãn đỏ **Hạn chế KM**.
2. **F4**, gõ `+84 901 234 567` (cố ý dạng +84) → thẻ **Nguyễn Thị Lan**: Kim cương ×2, khoảng 5.000 điểm, nhãn **Chưa xác thực**, điểm sắp hết hạn.
3. **Đổi điểm** → báo giá: giá trị hàng được đổi điểm **109.000đ** (đã bỏ sữa công thức), tối đa **545 điểm = 54.500đ** (50%).
   Sửa thành **200** → **Gửi OTP cho khách**.
4. **Xem OTP trên điện thoại khách (giả lập)** → đọc mã 6 số.
   - Gõ sai một lần để thấy "Mã OTP không đúng".
   - Gõ đúng → tự xác nhận → "Đã giữ 200 điểm (−20.000đ). SĐT của khách đã được xác thực". Thẻ có khung **Đang giữ** với đồng hồ 15 phút; nhãn Chưa xác thực biến mất.
5. **F9** → "Trừ bằng điểm −20.000", khách trả tiền mặt **161.000** → Enter.
6. Hóa đơn: Điểm 20.000 · Tiền mặt 161.000 · **Điểm đã dùng −200** · **Điểm cộng +16**.
   Nói cách tính: tích trên 109.000 − 20.000 (phần trả bằng điểm) = 89.000 → 8 điểm × hệ số Kim cương 2 = **16**.
7. **Hóa đơn mới.**

### 4. Khóa đổi điểm và hủy đổi điểm (cửa sổ A, 30 giây)
1. Quét `8931002000144`, **F4** `0912345678` (Trần Văn Hùng) → nhãn đỏ **Tạm khóa đổi điểm**; bấm **Đổi điểm** → "Tài khoản đang tạm khóa đổi điểm, mời khách liên hệ CSKH". Đóng.
2. (Nếu còn thời gian) Bỏ gắn Hùng, gắn `0935123456` (Phạm Minh Tuấn), đổi điểm có OTP như bước 3, rồi bấm **Hủy đổi điểm (trả lại điểm)** → số dư trở lại như cũ. Hoặc quét thêm một mặt hàng → hệ thống tự trả điểm và yêu cầu đổi lại.
3. **Hủy hóa đơn đang bán.**

### 5. Customer 360 (cửa sổ B, 1 phút 15)
1. Portal `admin` → **Khách hàng** → tìm `lan` → bấm **Nguyễn Thị Lan**.
2. Chỉ ra: trạng thái **Đã xác thực** (từ bước 3), số dư điểm có dấu **"khớp tổng sổ cái"**, tiến độ hạng.
3. Tab **Hóa đơn**: hóa đơn vừa bán ở HM001 trên cùng (−200 / +16), bấm **+** xem từng dòng hàng (sữa công thức có nhãn Không tích điểm). Các hóa đơn khác ở cả 3 cửa hàng.
4. Tab **Sổ cái điểm**: HOLD −200 (khi OTP đúng) → RELEASE +200 → nhiều dòng REDEEM trừ từ lô cũ nhất (FIFO, tổng −200) → EARN +16. Nói: sổ cái chỉ ghi thêm, không sửa / xóa được.
5. Bấm **Xem SĐT đầy đủ** → thông báo đã ghi nhật ký.

### 6. Đổi giá có duyệt (cửa sổ C rồi B, 1 phút 15)
1. Cửa sổ C (`danhmuc`) → **Mặt hàng và giá** → tìm `RAU001` → bấm dòng **Cải ngọt Đà Lạt 500g**.
2. Phần **Giá bán**: Toàn chuỗi, giá **20000**, để trống "Hiệu lực từ" → **Tạo bản giá** → "Đã tạo bản giá nháp, chờ ADMIN duyệt". Tab **Giá chờ duyệt**: `danhmuc` thấy dòng chờ nhưng **không có nút Duyệt**.
   (Có sẵn một bản nháp 19.100đ hiệu lực từ ngày mai – minh họa giá tương lai.)
3. Cửa sổ B (`admin`) → **Mặt hàng và giá** → tab **Giá chờ duyệt** → **Duyệt** dòng 20.000 → xác nhận.
4. Cửa sổ A (POS) → quét `8931001000015` → giá **20.000đ**. **Hủy hóa đơn đang bán.**

### 7. Chuyển đổi dữ liệu (cửa sổ B, 1 phút)
1. **Chuyển đổi dữ liệu** → cửa hàng **HM001**, POS cũ **POS Alpha** (hiện định dạng cột của nhà cung cấp) → chọn `sample-data/customers_POS_ALPHA_HM001.csv` → **Nhập dữ liệu**.
2. Kết quả: **40 dòng · 32 tạo mới · 3 gộp · 5 lỗi**; bảng lỗi có lý do và dữ liệu gốc (SĐT bàn, thiếu tên, ngày 31/02, email sai, SĐT thiếu số).
3. Nói: SĐT dạng +84 / có dấu chấm được chuẩn hóa; khách trùng SĐT (Lan, Hùng) được **gộp vào hồ sơ có sẵn**, không tạo hồ sơ mới; khách mới ở trạng thái Chưa xác thực, chưa có consent marketing.
4. (Nếu còn thời gian) Customer 360 của Lan → tab **Nguồn dữ liệu**: có bản ghi từ POS_ALPHA.

### 8. Dashboard (cửa sổ B, 45 giây)
1. **Dashboard**: tỷ lệ hóa đơn có định danh, tỷ trọng doanh thu thành viên, tỷ lệ quay lại, phân bố hạng, doanh thu theo ngày (thành viên / vãng lai), theo cửa hàng, ngành hàng.
2. Đổi bộ lọc: **7 ngày qua**, cửa hàng **HM001** → số liệu đổi theo. Nút **Bảng** để xem số chính xác.
3. (Tùy chọn) **Hạng thành viên** → bảng hạng, chính sách điểm, nút **Chạy xét hạng**.

### 9. Phân quyền (30 giây)
1. Cửa sổ C: đăng xuất, đăng nhập `ql.hm001` → Dashboard, ô cửa hàng **khóa ở HM001**; menu không có Người dùng / Chuyển đổi dữ liệu.
2. Đăng xuất, thử đăng nhập Portal bằng `tn.hm001.1` → "Thu ngân chỉ đăng nhập trên POS".
3. (Tùy chọn) `cskh` → không có Dashboard; có Khách hàng và xem được SĐT đầy đủ.

### Kết (30 giây)
"Một hệ thống thay 3 POS; mọi hóa đơn có thể định danh; điểm an toàn nhờ OTP, giữ điểm có thời hạn và sổ cái chỉ ghi thêm; dữ liệu cũ đưa vào được mà không tạo hồ sơ trùng; quản lý có số liệu theo cửa hàng."

---

## Sự cố thường gặp khi demo

| Hiện tượng | Xử lý nhanh |
|---|---|
| POS báo "Không kết nối được máy chủ" | Backend chưa chạy xong; xem terminal backend, chờ `Started` |
| `0987654321` báo đã là thành viên | Dữ liệu đã dùng cho lần demo trước → tạo lại database, hoặc dùng SĐT khác dạng `0869…` chưa có trong file CSV |
| Hết giờ giữ điểm (15 phút) | Hệ thống tự trả điểm và báo; bấm Đổi điểm lại |
| OTP "hết hạn" | Mã OTP sống 5 phút; bấm "Đổi số điểm" rồi gửi OTP mới |
| Bị đẩy về trang đăng nhập | Phiên hết hạn hoặc vừa tạo lại database → đăng nhập lại |
| Cửa sổ POS và Portal đè phiên nhau | Dùng cửa sổ ẩn danh cho tài khoản thứ hai |
