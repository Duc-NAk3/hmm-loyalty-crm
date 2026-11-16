# HMM Loyalty CRM – bản demo

Demo cho **19 FR có đánh dấu Demo** trong SRS: FR-01–04, 11, 12, 18–20, 22, 28, 30, 31, 33, 51, 58–60, 65.
Không làm: bán khi mất mạng (FR-09), hóa đơn điện tử, voucher, phân khúc. OTP và SMS là giả lập.

## Bắt đầu / tiếp tục công việc (đọc trước tiên)
1. **`tasks/README.md`:** bảng trạng thái các bước B1–B11 (✅ / ⬜), commit tương ứng, nguồn thông tin. Bước ⬜ đầu tiên là việc tiếp theo; file của bước đó có sẵn API backend, file cần sửa và cách kiểm tra.
2. **Xong một bước:**
   - Đổi trạng thái thành ✅ kèm mã commit trong file bước và trong `tasks/README.md`.
   - Hỏi người dùng, được đồng ý thì commit cùng code và push `Dev`.
   - Dừng lại báo người dùng trước khi sang bước sau (người dùng muốn làm từng bước).
3. **Kế hoạch tổng thể đã duyệt:** `C:\Users\DUC\.claude\plans\scalable-foraging-garden.md`. Bộ nhớ dài hạn: `C:\Users\DUC\.claude\projects\D--Job-SofiTech\memory\`.
4. **Tài liệu nghiệp vụ** (SRS, ERD, Architecture, Đặc tả API) và quy ước viết tài liệu Word: xem `../CLAUDE.md` ở gốc thư mục bài.
5. **Trạng thái hiện tại:** B1–B10 ✅; **B11 Docker là bước tiếp theo** (máy người dùng chưa cài Docker, chỉ viết file, chưa chạy thử được); B12 sau demo (merge `main` khi được duyệt, tài liệu Word còn lại).
6. **Mẫu kiến trúc dùng lại cho project khác:** `D:\Job\SofiTech\CLAUDE_Template\CLAUDE.md` (bản tổng quát hóa từ repo này). Đổi quy ước kiến trúc ở đây thì cập nhật cả file mẫu.

## Tài liệu trong repo
| File | Nội dung |
|---|---|
| `README.md` | Cài đặt, chạy, tài khoản demo, cấu trúc thư mục (đã thử trên CSDL trống) |
| `docs/KICH_BAN_DEMO.md` | Kịch bản demo ~10 phút, 9 bước, có số liệu mong đợi (+12 điểm khách mới; báo giá 545 điểm trên 109.000đ; hóa đơn 181.000đ −200/+16; nhập CSV 40/32/3/5) |
| `docs/HUONG_DAN_RUN_DEBUG.md` | Run & Debug bằng VS Code, mục 5 thử API bằng Swagger, mục 6 lỗi thường gặp |
| `docs/HUONG_DAN_DOCKER.md` | Chạy bằng Docker: khái niệm, lệnh, Docker Desktop, build lại, chia sẻ, lỗi thường gặp |
| `docs/HUONG_DAN_TEST_POS.txt` | 3 thu ngân, 20 mặt hàng, 10 lượt thanh toán có tổng tiền mong đợi; luồng code Controller → Facade/Service → Repository → Entity |
| `sample-data/README.md` | 3 file CSV của 3 POS cũ (định dạng cột khác nhau, có dòng lỗi, SĐT trùng) + `products_import.csv` |
| `tasks/` | Mỗi bước một file `Bxx-*.md` (phạm vi, API, file cần sửa, cách kiểm tra) |

Sửa API (đường dẫn, trường, mã lỗi) thì báo người dùng là tài liệu Word Đặc tả API (`../03_Diagrams/...API_Specification...docx`, API-01–52) cần cập nhật theo.

## Lưu ý môi trường (Windows)
- **Run & Debug cho người dùng:** `docs/HUONG_DAN_RUN_DEBUG.md`; cấu hình VS Code trong `.vscode/` (launch.json, tasks.json, extensions.json – đã đưa lên git). Người dùng debug bằng IDE thì backend của Claude phải tắt để nhả cổng 8080.
- **IDE của người dùng là VS Code** (Extension Pack for Java, biên dịch bằng Eclipse JDT) và **cũng ghi vào `backend/target/`**: trước khi kết luận build/test, chạy `mvnw clean …` để không dùng nhầm file `.class` cũ. `clean` khi backend đang chạy sẽ làm backend chết: dừng preview trước.
- **File do người dùng/công cụ sinh ra thay đổi** (`package-lock.json`, file IDE): xem diff, nếu vô hại thì đề xuất commit riêng trên `Dev` (vẫn hỏi trước khi commit, push), không bỏ qua mãi.
- **Kiểm tra refactor:** chụp JSON của các API chính trước và sau (đăng nhập, tra cứu thành viên, báo giá, dashboard, Customer 360…), so sánh phải giống nhau (chỉ khác `lastLoginAt`), cộng `mvnw clean test`.
- **Ghi file:** dùng công cụ Write/Edit. Heredoc nhiều file trong Bash hay bị lỗi; muốn sửa hàng loạt thì viết script `node -e`.
- **Server để thử:**
  - Dùng preview `backend` (cổng 8080) và `frontend` (cổng 5173), cấu hình trong `D:\Job\SofiTech\.claude\launch.json` (đặt `autoPort: false`).
  - Dừng backend xong thì đợi vài giây cho cổng 8080 được giải phóng rồi mới khởi động lại.
  - Backend nhận request trước khi seeder chạy xong; xem log có dòng "Demo data seeded".
- **Lệnh git:** cảnh báo "LF will be replaced by CRLF" là bình thường. Gõ `git add -A 2>/dev/null` cho gọn.
- **Console frontend:** cảnh báo "Accessing element.ref was removed in React 19" đến từ Ant Design, bỏ qua được.

## Git – bắt buộc
- Remote: GitHub private `Duc-NAk3/hmm-loyalty-crm`. `gh` CLI đã đăng nhập.
- Làm trên nhánh **`Dev`**. Mỗi bước nhỏ là một commit trên `Dev`.
- **Hỏi người dùng trước MỖI lần `git commit` và MỖI lần `git push`, kể cả ở chế độ auto và kể cả commit nhỏ ghi task** (yêu cầu 16/11/2026). Nêu danh sách file và nội dung commit, chờ người dùng đồng ý rồi mới làm; đồng ý commit chưa có nghĩa là đồng ý push.
- **Không merge hoặc push lên `main` khi người dùng chưa duyệt.**
- Kế hoạch các bước B1–B11 nằm trong `C:\Users\DUC\.claude\plans\scalable-foraging-garden.md`.

## Docker (B11, chạy trong thư mục gốc `05_SourceCode`)
- `docker compose up -d --build` → giao diện http://localhost:3000 (nginx chuyển `/api` sang `backend:8080`), Swagger http://localhost:8081/swagger-ui.html.
- 4 dịch vụ: `postgres:17` (volume `pgdata`, healthcheck `pg_isready`), `redis:7` (healthcheck), `backend` (profile `docker`: OTP lưu Redis, CORS cổng 3000; chờ postgres, redis healthy), `frontend` (nginx, `frontend/nginx.conf`). Tất cả `restart: unless-stopped`.
- CSDL trong Docker tách khỏi `hmm_loyalty` local; lần đầu backend tự sinh dữ liệu mẫu. Tạo lại: `docker compose down -v` rồi `up -d`. **`down -v` xóa dữ liệu của người dùng – hỏi trước; muốn thử từ đầu thì dùng project riêng `-p <tên>`.**
- `.dockerignore`: backend bỏ `target`, `local.properties`; frontend bỏ `node_modules`, `dist` (thiếu thì gửi 226 MB và node_modules Windows đè bản Linux).
- Docker 29.9 đã cài trên máy người dùng (16/11/2026).

## Lệnh (Windows; chạy trong `backend/`)
- Chạy: `.\mvnw.cmd spring-boot:run` → http://localhost:8080.
- **Swagger** `/swagger-ui.html` (OpenAPI JSON ở `/v3/api-docs`):
  - `admin/config/OpenApiConfig` khai báo scheme `bearerAuth` (HTTP bearer JWT) áp cho mọi API, nên có nút **Authorize**.
  - API không cần token (`POST /api/auth/login`, `GET /api/pos/terminals`) đánh dấu `@SecurityRequirements` rỗng.
  - `springdoc.swagger-ui.persist-authorization: true` giữ token khi tải lại trang; tag và operation sắp xếp theo alpha.
  - Cách thử: gọi login → copy `token` → Authorize → dán token (không gõ chữ `Bearer`).
- Test: `.\mvnw.cmd clean test` (36 test): `PointCalculatorTest` 6, `TextsTest` 16, `ModularityTest` 1, `LoyaltyIntegrationTest` 7, `RepositoryQueryTest` 3, `SearchFilterTest` 3.
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
  - Gốc package: `<Module>Facade` (interface, chỉ khai báo hàm). Module khác chỉ được gọi qua đây.
  - `api/`: DTO dùng chung giữa các module (dữ liệu đi qua Facade), mỗi class một file, Lombok `@Value` (không setter). `api/package-info.java` có `@NamedInterface("api")` (thư viện `spring-modulith-api`) để Modulith cho phép module khác dùng; thiếu annotation này `ModularityTest` báo "non-exposed type". Ví dụ `customer/api/CustomerInfo`, `loyalty/api/EarnResult`.
  - `controller/`: `@RestController` (tên package Java viết thường). Entity không bao giờ trả ra API.
  - `dto/<chức năng>/`: DTO của API, **mỗi DTO một class, tên có đuôi `Dto`** (`...View` → `...Dto`, còn lại thêm `Dto`: `TierDto`, `TiersResponseDto`, `RegisterMemberRequestDto`) (không dùng record), Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`; field theo đúng tên JSON (camelCase), annotation kiểm tra (`@NotBlank`…) đặt trên field. Ví dụ `loyalty/dto/member/RegisterMemberRequestDto`, `loyalty/dto/tier/TierDto`, `pos/dto/sale/SaleRequestDto`. Đọc giá trị bằng getter (`req.getPhone()`, boolean là `isX()`). Package Java viết thường (`customerimport` vì `import` là từ khóa).
  - `service/`: `@Service` nghiệp vụ và `@Transactional`; cài đặt Facade nằm ở đây (ví dụ `LoyaltyFacadeImpl`, `CustomerService implements CustomerFacade`).
  - `domain/`: `@Entity` (Lombok `@Getter`, rich domain method) và enum trạng thái.
  - `repository/`: interface Spring Data JPA (`extends JpaRepository<Entity, Long>`) – truy vấn theo tên hàm, `@Query` JPQL, `@Lock`.
  - `ModularityTest` (Spring Modulith) báo lỗi khi gọi vào `controller/`, `service/`, `domain/`, `repository/` của module khác hoặc khi có phụ thuộc vòng. Package `demo` được loại khỏi kiểm tra này.
  - Extension Spring Boot Tools của VS Code không biết ngoại lệ đó nên báo `MODULITH_TYPE_REF_VIOLATION` ở `DemoDataSeeder`; `.vscode/settings.json` hạ mức báo này xuống WARNING cho cả workspace (người dùng chọn 16/11/2026). Người dùng thường mở VS Code ở thư mục cha `NguyenAnhDuc_Assignment_Loyalty_CRM`, nên cài đặt được lặp lại ở `../.vscode/settings.json` (ngoài git) – VS Code chỉ đọc `.vscode` của thư mục gốc đang mở. Không mở phần nội bộ module (`OPEN`, named interface) để tắt báo.
- **ORM:** Spring Data JPA (Hibernate 7), `ddl-auto=validate`.
  - Schema chỉ quản lý bằng Flyway (`db/migration`); V2 đổi bước nhảy sequence thành 50, dùng `@SequenceGenerator(allocationSize = 50)` với optimizer `pooled-lo`.
  - Tên bảng số nhiều, đúng như ERD.
  - **Quan hệ giữa entity:**
    - Trong cùng module dùng `@ManyToOne` / `@OneToMany`.
    - Khác module chỉ giữ ID (`Long customerId`…), nên Hibernate không biết thứ tự khóa ngoại giữa các module. Khi cần, `saveAndFlush` hóa đơn trước khi ghi sổ cái.
  - Cột JSONB map `String` với `@ColumnTransformer(write = "cast(? as jsonb)")`.
  - Module `report` (mô hình đọc) và `TierService.dailyReview` truy vấn bằng JPQL theo tên entity (`EntityManager`); enum trong JPQL viết tên đầy đủ, ví dụ `vn.sofitech.hmm.pos.domain.SalesTransaction.Status.COMPLETED`.
  - Native SQL duy nhất là `nextval('customer.member_code_seq')` để sinh mã thành viên.
  - **Bộ lọc tùy chọn (trang tìm kiếm): không dùng mẹo `(:x is null or …)` trong JPQL.** Tham số null không rõ kiểu, Hibernate gửi xuống PostgreSQL dạng `bytea` (lỗi `upper(bytea) does not exist`), và lỗi chỉ hiện tùy lần gọi đầu sau khi khởi động vì query plan được cache. Thay vào đó chỉ thêm điều kiện khi có lọc:
    - Trong module sở hữu entity: **Specification** (`JpaSpecificationExecutor`), ví dụ `catalog/repository/ProductSpecifications` + `Specification.allOf(...)`.
    - Trong module `report`: **Criteria API** qua `EntityManager`, lấy entity theo tên trong metamodel (không import entity module khác), ví dụ `CustomerQueryService.search`.
    - Test: `report/SearchFilterTest` (gọi tìm kiếm không có từ khóa đầu tiên).
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
  - Phân quyền mức URL ở `admin/config/SecurityConfig`: `/api/pos/**` cho CASHIER, STORE_MANAGER; `/api/portal/users/**`, `/api/portal/migration/**` chỉ ADMIN; `/api/portal/**` cho các vai trò trung tâm. Mức hàm dùng `@PreAuthorize` (ví dụ sửa mặt hàng/giá chỉ CATALOG, ADMIN).
  - Thu ngân chỉ đăng nhập kèm `terminalCode`; thu ngân đăng nhập Portal bị từ chối.
  - Controller đọc người dùng bằng `CurrentUser.of(jwt)`; `scopeStore(...)` ép quản lý cửa hàng chỉ xem cửa hàng mình.
  - 401 và 403 mức URL không có body; 403 mức hàm trả JSON `FORBIDDEN`.
- **OTP:** qua `OtpStore` – chạy local thì lưu trong bộ nhớ; profile `docker` thì lưu Redis.
- **SMS:** ghi vào `shared.outbox_messages`; tác vụ nền đánh dấu SENT.
- **Tác vụ định kỳ** (`@EnableScheduling`): hết hạn giữ điểm mỗi 30 giây (`RedemptionService`), xét hạng 2 giờ sáng (`TierService`), gửi outbox SMS mỗi 3 giây (`SmsOutbox`).
- **Lỗi:** body luôn `{code, message}`, thông báo tiếng Việt (chuỗi chung ở `shared.Texts`).
  - Nghiệp vụ: ném `ApiException.badRequest/conflict/forbidden(code, msg)` hoặc `ApiException.notFound(msg)` (`NotFoundException`).
  - `shared.GlobalExceptionHandler` map: `@Valid` sai → 400 `VALIDATION`; JSON sai kiểu → 400 `BAD_REQUEST`; `DataIntegrityViolationException` → 409 `DUPLICATE`; lỗi khóa lạc quan/bi quan → 409 `CONCURRENT_UPDATE`; `AccessDeniedException` → 403 `FORBIDDEN`; còn lại → 500 `ERROR`.
- **Phân trang:** trả `shared.PageResult<T>` dạng `{total, items}`. `PageResult` và `CurrentUser` còn là record (kiểu hạ tầng, không phải DTO nghiệp vụ).

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
