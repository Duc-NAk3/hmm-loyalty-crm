# HMM Loyalty CRM – bản demo

Demo cho **19 FR có đánh dấu Demo** trong SRS: FR-01–04, 11, 12, 18–20, 22, 28, 30, 31, 33, 51, 58–60, 65.
Không làm: bán khi mất mạng (FR-09), hóa đơn điện tử, voucher, phân khúc. OTP và SMS là giả lập.

## Git – bắt buộc
- Remote: GitHub private `Duc-NAk3/hmm-loyalty-crm`. `gh` CLI đã đăng nhập.
- Làm trên nhánh **`Dev`**. Mỗi bước nhỏ là một commit, push `origin Dev` ngay sau khi kiểm tra xong.
- **Không merge hoặc push lên `main` khi người dùng chưa duyệt.**
- Kế hoạch các bước B1–B11 nằm trong `C:\Users\DUC\.claude\plans\scalable-foraging-garden.md`.

## Lệnh (Windows; chạy trong `backend/`)
- Chạy: `.\mvnw.cmd spring-boot:run` → http://localhost:8080. Swagger ở `/swagger-ui.html`.
- Test: `.\mvnw.cmd test`.
  - Test tích hợp dùng database `hmm_loyalty_test`, tự tạo lại mỗi lần chạy.
  - Máy không kết nối được PostgreSQL thì nhóm test này tự bỏ qua.
- Mật khẩu PostgreSQL nằm trong `backend/local.properties` (`DB_PASSWORD=...`, đã gitignore). Không in mật khẩu ra màn hình.
- `psql` ở `D:\Setup\PostgreSQL\17\bin\psql.exe`.
- Dữ liệu demo:
  - Database `hmm_loyalty`, tự tạo bởi `DemoDataSeeder` khi CSDL trống.
  - Muốn tạo lại thì `DROP DATABASE hmm_loyalty WITH (FORCE)`, `CREATE DATABASE`, rồi chạy lại backend.

## Backend (`backend/`, Spring Boot 4.0.9, Java 21)
- **Module:** mỗi module là một package phẳng dưới `vn.sofitech.hmm`: `shared`, `admin`, `catalog`, `customer`, `loyalty`, `pos`, `report`, `migration`, `demo`.
  - `ModularityTest` (Spring Modulith) chặn phụ thuộc vòng.
  - Chiều phụ thuộc: `pos → loyalty → customer → shared`; `admin` chỉ phụ thuộc `shared`.
- **Truy cập dữ liệu:** dùng `JdbcClient` với SQL tường minh, **không dùng JPA**.
  - Schema chỉ quản lý bằng Flyway (`db/migration`).
  - Tên bảng số nhiều, đúng như ERD; ví dụ `loyalty.point_ledgers`, `pos.sales_transactions`.
- **Sổ cái điểm chỉ ghi thêm** (có trigger chặn UPDATE/DELETE). `customers.point_balance` luôn bằng tổng sổ cái.
  - EARN tạo `point_lots`.
  - Đổi điểm ghi HOLD (−) khi OTP đúng; lúc hoàn tất hóa đơn ghi RELEASE (+) rồi REDEEM (−) theo từng lô FIFO.
  - Mọi biến động điểm đi qua `LedgerService` (yêu cầu transaction MANDATORY).
- **Công thức điểm:** chỉ nằm trong `loyalty/PointCalculator` (hàm thuần). `DemoDataSeeder` cũng dùng hàm này.
- **Bảo mật:** JWT HS256 (oauth2 resource server); claim `uid`, `role`, `storeId`.
  - 6 vai trò: ADMIN, STORE_MANAGER, CASHIER, CSKH, MARKETING, CATALOG.
  - `/api/pos/**` cho thu ngân; `/api/portal/**` cho các vai trò trung tâm.
  - Thu ngân chỉ đăng nhập kèm `terminalCode`.
- **OTP:** qua `OtpStore` – chạy local thì lưu trong bộ nhớ; profile `docker` thì lưu Redis.
- **SMS:** ghi vào `shared.outbox_messages`; tác vụ nền đánh dấu SENT.
- **Lỗi nghiệp vụ:** `ApiException` với thông báo tiếng Việt.

## Dữ liệu demo
- Mật khẩu mọi tài khoản: `Demo@123`.
- Tài khoản: `admin`, `ql.hm001..003`, `tn.hm001.1/2`, `tn.hm002.1/2`, `tn.hm003.1/2`, `cskh`, `marketing`, `danhmuc`.
- Quầy: `HM001-Q01` … `HM003-Q02`.
- Khách:
  - 0901234567 Nguyễn Thị Lan: DIAMOND, chưa xác thực – dùng demo OTP lần đầu.
  - 0912345678: đang bị khóa đổi điểm.
  - 0935123456: khách thường, có điểm để demo đổi điểm.
  - 0987654321: chưa có trong dữ liệu, dùng demo đăng ký mới.
  - SĐT `0869xxxxxx`: dành cho file CSV chuyển đổi mẫu.

## Frontend (`frontend/`, đang làm)
Vite + React + TypeScript + Ant Design + Recharts; một app gồm `/pos` (thu ngân) và `/portal`; proxy `/api` → 8080.
