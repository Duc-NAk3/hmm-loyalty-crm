# Hoa Mai Mart – Loyalty CRM (bản demo)

Bản demo chứng minh khái niệm (proof of concept) của SofiTech cho hệ thống **Loyalty CRM có phân hệ bán hàng tại quầy (POS)** của chuỗi siêu thị mini Hoa Mai Mart (3 cửa hàng).
Phạm vi: **19 yêu cầu chức năng có đánh dấu Demo** trong tài liệu SRS (xem [mục 7](#7-19-chức-năng-demo-và-màn-hình)).

| Thành phần | Công nghệ |
|---|---|
| `backend/` | Spring Boot 4 (Java 21), Spring Data JPA (Hibernate 7), PostgreSQL 17, Flyway, Spring Security (JWT), Spring Modulith (kiểm tra ranh giới module) |
| `frontend/` | React 19 + Vite + TypeScript + Ant Design 6 + Recharts – một ứng dụng gồm **POS tại quầy** và **Web Portal** |

**Tài liệu kèm theo**
| Tài liệu | Nội dung |
|---|---|
| [docs/KICH_BAN_DEMO.md](docs/KICH_BAN_DEMO.md) | Kịch bản trình bày khoảng 10 phút |
| [docs/HUONG_DAN_RUN_DEBUG.md](docs/HUONG_DAN_RUN_DEBUG.md) | Chạy và debug bằng VS Code / IntelliJ, lỗi thường gặp |
| [docs/HUONG_DAN_TEST_POS.txt](docs/HUONG_DAN_TEST_POS.txt) | 3 thu ngân, 20 mặt hàng, 10 lượt thanh toán tính sẵn, các lượt có thành viên, luồng dữ liệu trong code |
| [sample-data/README.md](sample-data/README.md) | File CSV "xuất từ POS cũ" để demo chuyển đổi dữ liệu, kết quả mong đợi |

---

## 1. Yêu cầu cài đặt

| Phần mềm | Phiên bản | Ghi chú |
|---|---|---|
| JDK | 21 trở lên | Đã chạy thử trên JDK 25 |
| Node.js | 22 | kèm npm |
| PostgreSQL | 17 | cổng 5432, user `postgres` |
| Maven | không cần cài | dùng Maven Wrapper `mvnw` có sẵn, lần đầu tự tải |

## 2. Cài đặt và chạy (lần đầu)

**Bước 1 – Mật khẩu PostgreSQL.** Tạo file `backend/local.properties` (đã có trong `.gitignore`, không đưa lên git):
```
DB_PASSWORD=<mật khẩu user postgres>
```
Các giá trị khác có mặc định: `DB_HOST=localhost`, `DB_PORT=5432`, `DB_NAME=hmm_loyalty`, `DB_USER=postgres` (đặt trong file này hoặc biến môi trường nếu khác).

**Bước 2 – Tạo database rỗng:**
```bash
psql -U postgres -c "CREATE DATABASE hmm_loyalty"
```
Nếu Windows báo không tìm thấy `psql`, dùng đường dẫn đầy đủ trong thư mục cài PostgreSQL, ví dụ `"C:/Program Files/PostgreSQL/17/bin/psql.exe"` (hoặc tạo database bằng pgAdmin).

**Bước 3 – Chạy backend** (cửa sổ terminal thứ nhất):
```bash
cd backend
.\mvnw.cmd spring-boot:run
```
- Lần đầu: Flyway tạo 26 bảng, rồi `DemoDataSeeder` nạp dữ liệu mẫu (khoảng 20 giây). Chờ log **`Demo data seeded`** rồi mới mở giao diện.
- Backend chạy ở http://localhost:8080. Tài liệu API (Swagger): http://localhost:8080/swagger-ui.html.

**Bước 4 – Chạy frontend** (cửa sổ terminal thứ hai):
```bash
cd frontend
npm install
npm run dev
```
`npm install` chỉ cần lần đầu. Frontend chạy ở http://localhost:5173 và chuyển mọi lời gọi `/api` sang cổng 8080.

**Bước 5 – Mở http://localhost:5173** và chọn khu vực:
- **POS tại quầy:** lần đầu chọn quầy cho máy (ví dụ `HM001-Q01`), đăng nhập thu ngân `tn.hm001.1`, mở ca.
- **Web Portal:** đăng nhập `admin` (hoặc tài khoản khác ở mục 3).

**Tạo lại dữ liệu sạch** (nên làm trước buổi demo): tắt backend, rồi
```bash
psql -U postgres -c "DROP DATABASE hmm_loyalty WITH (FORCE)"
psql -U postgres -c "CREATE DATABASE hmm_loyalty"
```
và chạy lại backend. Lịch sử bán hàng mẫu luôn tính từ 01/12/2025 đến hôm qua, nên số liệu thay đổi theo ngày chạy.

## 3. Tài khoản demo

Mật khẩu chung: **`Demo@123`**. Trang đăng nhập Portal có sẵn danh sách tài khoản (mục "Tài khoản demo").

| Tên đăng nhập | Vai trò | Cửa hàng | Dùng ở |
|---|---|---|---|
| `admin` | Quản trị hệ thống | toàn chuỗi | Portal – mọi chức năng, duyệt giá, chuyển đổi dữ liệu, người dùng |
| `ql.hm001`, `ql.hm002`, `ql.hm003` | Quản lý cửa hàng | HM001 / HM002 / HM003 | Portal (chỉ thấy số liệu cửa hàng mình) và POS |
| `tn.hm001.1`, `tn.hm001.2` | Thu ngân | HM001 | POS quầy `HM001-Q01`, `HM001-Q02` |
| `tn.hm002.1`, `tn.hm002.2` | Thu ngân | HM002 | POS quầy `HM002-Q01`, `HM002-Q02` |
| `tn.hm003.1`, `tn.hm003.2` | Thu ngân | HM003 | POS quầy `HM003-Q01`, `HM003-Q02` |
| `cskh` | Chăm sóc khách hàng | toàn chuỗi | Portal – khách hàng, Customer 360, xem SĐT đầy đủ |
| `marketing` | Marketing | toàn chuỗi | Portal – dashboard, khách hàng |
| `danhmuc` | Danh mục và giá | toàn chuỗi | Portal – mặt hàng, tạo giá (không duyệt được) |

Thu ngân chỉ đăng nhập được ở quầy thuộc cửa hàng của mình, và không đăng nhập được Portal.

## 4. Khách hàng và dữ liệu mẫu

| Khách | SĐT | Dùng để demo |
|---|---|---|
| Nguyễn Thị Lan | `0901234567` | Hạng Kim cương, nhiều điểm, **chưa xác thực** → đổi điểm lần đầu bằng OTP thì chuyển Đã xác thực |
| Trần Văn Hùng | `0912345678` | **Đang bị khóa đổi điểm** → POS báo không đổi được |
| Phạm Minh Tuấn | `0935123456` | Khách thường có điểm → đổi điểm, hủy đổi điểm |
| _(chưa có)_ | `0987654321` | **Đăng ký thành viên mới** tại quầy |
| _(chưa có)_ | `0869xxxxxx` | Dành cho file CSV chuyển đổi dữ liệu |

**Dữ liệu sinh sẵn:** 3 cửa hàng ở 3 miền, 6 quầy POS, 13 tài khoản, 4 hạng (Thành viên ×1, Bạc ×1.2, Vàng ×1.5, Kim cương ×2), 13 ngành hàng với 115 mặt hàng (có giá riêng theo cửa hàng, hàng hạn chế khuyến mại, thẻ nạp không tích điểm), 800 khách, khoảng 24.000 hóa đơn từ 01/12/2025. Điểm, lô điểm, sổ cái và hạng được tính bằng đúng logic nghiệp vụ, nên số dư điểm luôn bằng tổng sổ cái.

**OTP và SMS là giả lập:** không gửi tin thật. Xem mã OTP ở nút **"Điện thoại khách (giả lập)"** trên POS hoặc trang **Hộp thư SMS giả lập** trên Portal.

## 5. Chạy test

```bash
cd backend
.\mvnw.cmd test
```
36 test: công thức điểm, chuẩn hóa dữ liệu, kiểm tra ranh giới module (Spring Modulith), và test tích hợp với PostgreSQL thật (đổi điểm, sổ cái FIFO, giá hiệu lực, bộ lọc tìm kiếm). Test tích hợp dùng database riêng `hmm_loyalty_test` (tự tạo lại mỗi lần), không đụng dữ liệu demo; máy không có PostgreSQL thì nhóm này tự bỏ qua.

Kiểm tra kiểu TypeScript của frontend: `cd frontend` rồi `npm run build`.

## 6. Kiến trúc tóm tắt

**Modular monolith:** một ứng dụng Spring Boot chia thành các module độc lập, mỗi module sở hữu bảng của mình và chỉ gọi module khác qua **Facade** (interface công khai). `ModularityTest` (Spring Modulith) báo lỗi nếu một module gọi thẳng vào phần nội bộ của module khác.

```
 Trình duyệt ── React (5173) ── /api ──► Spring Boot (8080) ──JPA/Hibernate──► PostgreSQL (hmm_loyalty)
                 /pos   /portal          admin · catalog · customer · loyalty · pos · report · migration · shared
```

| Module | Nội dung |
|---|---|
| `admin` | Đăng nhập JWT, vai trò, cửa hàng, quầy, người dùng, tham số hệ thống |
| `catalog` | Mặt hàng, mã vạch, ngành hàng, bản giá có duyệt (giá cửa hàng ưu tiên giá chuỗi) |
| `customer` | Hồ sơ thành viên, chuẩn hóa SĐT, consent |
| `loyalty` | Tích điểm, lô điểm, sổ cái chỉ ghi thêm, đổi điểm có OTP, hạng thành viên |
| `pos` | Ca bán hàng, hóa đơn, thanh toán tiền mặt và điểm |
| `report` | Dashboard, tìm khách, Customer 360 (mô hình chỉ đọc) |
| `migration` | Nhập khách hàng từ 3 POS cũ |
| `shared` | Hộp thư SMS (outbox), nhật ký thao tác (audit), xử lý lỗi chung |

**Bên trong mỗi module:**
```
<module>/
  <Module>Facade.java   cửa giao tiếp cho module khác (interface)
  api/                  dữ liệu đi qua Facade (class @Value, @NamedInterface)
  controller/           @RestController – API HTTP
  dto/<chức năng>/      dữ liệu nhận / trả của API (class ...Dto)
  service/              nghiệp vụ, @Transactional, cài đặt Facade
  domain/               @Entity (bảng)
  repository/           Spring Data JPA (truy vấn ORM, Specification)
```

**Một số điểm thiết kế:** sổ cái điểm chỉ ghi thêm (trigger chặn sửa/xóa); đổi điểm theo trạng thái QUOTED → HELD → CONFIRMED / RELEASED / EXPIRED; trừ điểm theo lô cũ trước (FIFO); khóa dòng khi cộng/trừ điểm; SMS ghi vào outbox trong cùng giao dịch; mã hóa đơn do quầy sinh để gửi lại không bị ghi trùng.

## 7. 19 chức năng Demo và màn hình

| FR | Chức năng | Màn hình |
|---|---|---|
| FR-01 | Đăng nhập theo quầy, mở / đóng ca, đối chiếu tiền mặt | POS |
| FR-02 | Bán hàng: quét mã vạch, tìm theo tên, giá hiệu lực theo cửa hàng | POS – màn bán hàng |
| FR-03 | Gắn / bỏ thành viên vào hóa đơn | POS – khung Thành viên (F4) |
| FR-04 | Thanh toán tiền mặt và điểm, tiền thừa, hóa đơn | POS – Thanh toán (F9) |
| FR-11 | Mặt hàng: tạo, sửa, nhiều mã vạch, nhập CSV | Portal – Mặt hàng và giá |
| FR-12 | Bản giá mới ở trạng thái nháp, ADMIN duyệt mới có hiệu lực | Portal – Mặt hàng và giá, tab Giá chờ duyệt |
| FR-18 | Tra cứu thành viên theo SĐT (+84/84/0) hoặc mã | POS; Portal – Khách hàng |
| FR-19 | Đăng ký thành viên nhanh tại quầy, SMS chào mừng | POS |
| FR-20 | Xác thực SĐT bằng OTP lần đổi điểm đầu tiên | POS – Đổi điểm |
| FR-22 | Customer 360: hồ sơ, hóa đơn mọi cửa hàng đến từng dòng, sổ cái, consent, lịch sử hạng, nguồn dữ liệu | Portal – Khách hàng |
| FR-28 | Tích điểm theo hệ số hạng, bỏ hàng hạn chế khuyến mại và phần trả bằng điểm | POS – hóa đơn |
| FR-30 | Đổi điểm: tối đa 50%, tối thiểu 50 điểm, OTP, giữ điểm 15 phút, hủy trả điểm | POS – Đổi điểm |
| FR-31 | Hạng thành viên: lên hạng ngay khi đạt ngưỡng, xét hạng hằng ngày | POS – hóa đơn; Portal – Hạng thành viên |
| FR-33 | Sổ cái điểm chỉ ghi thêm, số dư bằng tổng sổ cái | Portal – Customer 360, tab Sổ cái |
| FR-51 | Dashboard KPI: tỷ lệ định danh, doanh thu thành viên, quay lại, phân bố hạng | Portal – Dashboard |
| FR-58 | Nhập khách hàng từ file của 3 POS cũ (mỗi bên một định dạng) | Portal – Chuyển đổi dữ liệu |
| FR-59 | Chuẩn hóa, ghi dòng lỗi kèm dữ liệu gốc | Portal – Chuyển đổi dữ liệu |
| FR-60 | Gộp trùng SĐT vào hồ sơ có sẵn, lưu bản ghi nguồn | Portal – Chuyển đổi dữ liệu; Customer 360 |
| FR-65 | Người dùng, vai trò, khóa tài khoản; menu và API theo vai trò | Portal – Người dùng |

## 8. Giới hạn của bản demo

- **Không bán khi mất mạng** (FR-09 không thuộc phạm vi demo).
- **OTP và SMS giả lập**, không gửi tin thật; **không có hóa đơn điện tử**, voucher, phân khúc khách hàng.
- **Không xác thực hai lớp** cho tài khoản quản trị.
- **Không quản lý tồn kho** (ngoài phạm vi dự án).
- POS và Portal là **một ứng dụng web** chia hai khu vực (kiến trúc thật đề xuất POS PWA và Portal riêng).
- Mã OTP lưu trong bộ nhớ backend khi chạy local; bản Docker dùng Redis.
- Thanh toán chỉ có tiền mặt và điểm (chưa có thẻ, chuyển khoản / QR).

## 9. Cấu trúc thư mục

```
05_SourceCode/
  backend/            Spring Boot (mvnw, pom.xml, src/main/java/vn/sofitech/hmm/..., db/migration)
  frontend/           React (src/pos – POS, src/portal – Web Portal, src/shared – dùng chung)
  sample-data/        CSV khách hàng của 3 POS cũ + CSV mặt hàng
  docs/               Kịch bản demo, hướng dẫn Run & Debug, hướng dẫn test POS
  tasks/              Tiến độ các bước làm demo
  .vscode/            Cấu hình Run and Debug cho VS Code
```
