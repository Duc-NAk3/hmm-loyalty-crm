# HMM Loyalty CRM – bản demo

Demo cho **19 FR có đánh dấu Demo** trong SRS: FR-01–04, 11, 12, 18–20, 22, 28, 30, 31, 33, 51, 58–60, 65.
Không làm: bán khi mất mạng (FR-09), hóa đơn điện tử, voucher, phân khúc. OTP và SMS là giả lập.

## Bắt đầu / tiếp tục công việc (đọc trước tiên)
1. **`tasks/README.md`:** bảng trạng thái các bước B1–B11 (✅ / ⬜), commit tương ứng, nguồn thông tin. Bước ⬜ đầu tiên là việc tiếp theo; file của bước đó có sẵn API backend, file cần sửa và cách kiểm tra.
2. **Xong một bước:**
   - Đổi trạng thái thành ✅ kèm mã commit trong file bước và trong `tasks/README.md`.
   - Commit cùng code, push `Dev`.
   - Dừng lại báo người dùng trước khi sang bước sau (người dùng muốn làm từng bước).
3. **Kế hoạch tổng thể đã duyệt:** `C:\Users\DUC\.claude\plans\scalable-foraging-garden.md`. Bộ nhớ dài hạn: `C:\Users\DUC\.claude\projects\D--Job-SofiTech\memory\`.
4. **Tài liệu nghiệp vụ** (SRS, ERD, Architecture) và quy ước viết tài liệu Word: xem `../CLAUDE.md` ở gốc thư mục bài.

## Lưu ý môi trường (Windows)
- **Ghi file:** dùng công cụ Write/Edit. Heredoc nhiều file trong Bash hay bị lỗi; muốn sửa hàng loạt thì viết script `node -e`.
- **Server để thử:**
  - Dùng preview `backend` (cổng 8080) và `frontend` (cổng 5173), cấu hình trong `D:\Job\SofiTech\.claude\launch.json` (đặt `autoPort: false`).
  - Dừng backend xong thì đợi vài giây cho cổng 8080 được giải phóng rồi mới khởi động lại.
  - Backend nhận request trước khi seeder chạy xong; xem log có dòng "Demo data seeded".
- **Lệnh git:** cảnh báo "LF will be replaced by CRLF" là bình thường. Gõ `git add -A 2>/dev/null` cho gọn.
- **Console frontend:** cảnh báo "Accessing element.ref was removed in React 19" đến từ Ant Design, bỏ qua được.

## Git – bắt buộc
- Remote: GitHub private `Duc-NAk3/hmm-loyalty-crm`. `gh` CLI đã đăng nhập.
- Làm trên nhánh **`Dev`**. Mỗi bước nhỏ là một commit, push `origin Dev` ngay sau khi kiểm tra xong.
- **Không merge hoặc push lên `main` khi người dùng chưa duyệt.**
- Kế hoạch các bước B1–B11 nằm trong `C:\Users\DUC\.claude\plans\scalable-foraging-garden.md`.

## Lệnh (Windows; chạy trong `backend/`)
- Chạy: `.\mvnw.cmd spring-boot:run` → http://localhost:8080. Swagger ở `/swagger-ui.html`.
- Test: `.\mvnw.cmd test`.
  - Test tích hợp kế thừa `PostgresTestBase` và dùng database `hmm_loyalty_test`, tự tạo lại mỗi lần chạy.
  - Máy không kết nối được PostgreSQL thì nhóm test này tự bỏ qua.
- Mật khẩu PostgreSQL nằm trong `backend/local.properties` (`DB_PASSWORD=...`, đã gitignore). Không in mật khẩu ra màn hình.
- `psql` ở `D:\Setup\PostgreSQL\17\bin\psql.exe`.
- Dữ liệu demo:
  - Database `hmm_loyalty`, tự tạo bởi `DemoDataSeeder` khi CSDL trống.
  - Muốn tạo lại thì `DROP DATABASE hmm_loyalty WITH (FORCE)`, `CREATE DATABASE`, rồi chạy lại backend.

## Backend (`backend/`, Spring Boot 4.0.9, Java 21)
- **Module dưới `vn.sofitech.hmm`:** `shared`, `admin`, `catalog`, `customer`, `loyalty`, `pos`, `report`, `migration`, `demo`.
- **Cấu trúc trong mỗi module (layered + Facade):**
  - Gốc package: `<Module>Facade` (interface) và các record DTO dùng chung giữa module (chưa đổi sang class). Chỉ phần này được module khác gọi.
  - `web/`: `@RestController`. Entity không bao giờ trả ra API.
  - `dto/<chức năng>/`: DTO của API, **mỗi DTO một class** (không dùng record), Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`; field theo đúng tên JSON (camelCase), annotation kiểm tra (`@NotBlank`…) đặt trên field. Ví dụ `loyalty/dto/member/RegisterMemberRequest`, `loyalty/dto/tier/TierView`, `pos/dto/sale/SaleRequest`. Đọc giá trị bằng getter (`req.getPhone()`, boolean là `isX()`). Package Java viết thường (`customerimport` vì `import` là từ khóa).
  - `service/`: `@Service` nghiệp vụ và `@Transactional`; cài đặt Facade nằm ở đây (ví dụ `LoyaltyFacadeImpl`, `CustomerService implements CustomerFacade`).
  - `domain/`: `@Entity` (Lombok `@Getter`, rich domain method) và enum trạng thái.
  - `repository/`: interface Spring Data JPA (`extends JpaRepository<Entity, Long>`) – truy vấn theo tên hàm, `@Query` JPQL, `@Lock`.
  - `ModularityTest` (Spring Modulith) báo lỗi khi gọi vào `web/`, `service/`, `domain/`, `repository/` của module khác hoặc khi có phụ thuộc vòng. Package `demo` được loại khỏi kiểm tra này.
- **ORM:** Spring Data JPA (Hibernate 7), `ddl-auto=validate`.
  - Schema chỉ quản lý bằng Flyway (`db/migration`); V2 đổi bước nhảy sequence thành 50, dùng `@SequenceGenerator(allocationSize = 50)` với optimizer `pooled-lo`.
  - Tên bảng số nhiều, đúng như ERD.
  - **Quan hệ giữa entity:**
    - Trong cùng module dùng `@ManyToOne` / `@OneToMany`.
    - Khác module chỉ giữ ID (`Long customerId`…), nên Hibernate không biết thứ tự khóa ngoại giữa các module. Khi cần, `saveAndFlush` hóa đơn trước khi ghi sổ cái.
  - Cột JSONB map `String` với `@ColumnTransformer(write = "cast(? as jsonb)")`.
  - Module `report` (mô hình đọc) và `TierService.dailyReview` truy vấn bằng JPQL theo tên entity (`EntityManager`); enum trong JPQL viết tên đầy đủ, ví dụ `vn.sofitech.hmm.pos.domain.SalesTransaction.Status.COMPLETED`.
  - Native SQL duy nhất là `nextval('customer.member_code_seq')` để sinh mã thành viên.
- **Observer:** module nghiệp vụ phát `SmsRequestedEvent`; `shared.SmsOutbox` ghi outbox trong cùng giao dịch.
- **Sổ cái điểm chỉ ghi thêm** (có trigger chặn UPDATE/DELETE). `customers.point_balance` luôn bằng tổng sổ cái.
  - EARN tạo `point_lots`.
  - Đổi điểm ghi HOLD (−) khi OTP đúng; lúc hoàn tất hóa đơn ghi RELEASE (+) rồi REDEEM (−) theo từng lô FIFO.
  - Mọi biến động điểm đi qua `LedgerService` (yêu cầu transaction MANDATORY); số dư sửa qua `CustomerFacade.adjustPointBalance`, có khóa `PESSIMISTIC_WRITE`.
  - Chuyển trạng thái đổi điểm do entity `PointRedemption` tự kiểm tra (QUOTED → HELD → CONFIRMED/RELEASED/EXPIRED).
- **Công thức điểm:** chỉ nằm trong `loyalty/PointCalculator` (hàm thuần). `DemoDataSeeder` cũng dùng hàm này.
- **`DemoDataSeeder`:**
  - Ghi bằng JPA theo từng ngày, mỗi ngày 3 lượt flush để đúng thứ tự khóa ngoại.
  - Random seed cố định, nhưng số liệu phụ thuộc ngày chạy (lịch sử từ 01/12/2025 đến hôm qua).
  - Thời gian chạy khoảng 20 giây.
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

## Frontend (`frontend/`)
- **Công nghệ:** Vite 8 + React 19 + TypeScript 7 + Ant Design 6 (locale vi_VN) + Recharts.
- **Lệnh:** `npm run dev` (cổng 5173, proxy `/api` → 8080), `npm run build` (kiểm tra kiểu).
- **Khu vực:** một app gồm `/pos` (thu ngân) và `/portal`.
  - Phiên POS và phiên Portal lưu riêng trong localStorage (`hmm.pos.session`, `hmm.portal.session`).
  - Quầy của máy lưu ở `hmm.pos.terminal`.
- **Gọi API:** qua `src/shared/api.ts`; hàm này tự gắn token theo khu vực và báo lỗi bằng thông báo tiếng Việt của backend.
- **Menu Portal theo vai trò:** khai báo trong `src/portal/menu.tsx`; route khai báo trong `App.tsx`.
- **Biểu đồ (Recharts):** đọc skill `dataviz` trước khi vẽ; màu và kiểu chung ở `src/shared/chart.ts` (Thành viên luôn xanh `#2a78d6`, Vãng lai luôn cam `#eb6834`; một chuỗi một màu; cột ≤ 24px, bo 4px đầu cột; không hai trục Y).
- **Lưu ý TypeScript:** tsconfig bật `verbatimModuleSyntax` và `erasableSyntaxOnly`, nên import kiểu phải dùng `import type`, và không dùng `enum`.
