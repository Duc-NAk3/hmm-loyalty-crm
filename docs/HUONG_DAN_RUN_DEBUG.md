# Hướng dẫn chạy (Run) và gỡ lỗi (Debug) – HMM Loyalty CRM

Hệ thống gồm 3 phần chạy cùng lúc trên máy:

| Phần | Địa chỉ | Chạy bằng |
|---|---|---|
| PostgreSQL 17 | cổng 5432, database `hmm_loyalty` | dịch vụ Windows (đã cài sẵn) |
| Backend Spring Boot | http://localhost:8080 (Swagger: `/swagger-ui.html`) | Maven Wrapper hoặc IDE |
| Frontend React (Vite) | http://localhost:5173 | `npm run dev` |

Frontend gửi mọi lời gọi `/api/...` sang cổng 8080 (cấu hình proxy trong `frontend/vite.config.ts`).
**Request đi vào backend nào đang giữ cổng 8080**, nên muốn breakpoint dừng thì backend đó phải được chạy bằng Debug.

---

## 1. Chuẩn bị (làm một lần)

1. **Cài:** JDK 21 trở lên, Node 22, PostgreSQL 17. Maven không cần cài (đã có `mvnw`).
2. **Mật khẩu PostgreSQL:** tạo file `backend/local.properties` (không đưa lên git):
   ```
   DB_PASSWORD=<mật khẩu user postgres>
   ```
   File này được đọc theo **thư mục đang chạy** – vì vậy backend phải chạy với thư mục làm việc là `backend/`.
3. **Tạo database rỗng:**
   ```bash
   "D:/Setup/PostgreSQL/17/bin/psql.exe" -U postgres -c "CREATE DATABASE hmm_loyalty"
   ```
   Lần đầu backend chạy sẽ tự tạo bảng (Flyway) và nạp dữ liệu mẫu (~20 giây, log có dòng `Demo data seeded`).
4. **Thư viện frontend:** trong `frontend/` chạy `npm install`.

**Tạo lại dữ liệu sạch** (trước buổi demo): tắt backend, chạy `DROP DATABASE hmm_loyalty WITH (FORCE)` rồi `CREATE DATABASE hmm_loyalty`, chạy lại backend.

---

## 2. Chạy nhanh bằng terminal (không debug)

Hai cửa sổ terminal:
```bash
cd backend
.\mvnw.cmd spring-boot:run
```
```bash
cd frontend
npm run dev
```
Mở http://localhost:5173 → **POS tại quầy** (quầy `HM001-Q01`, `tn.hm001.1`) hoặc **Web Portal** (`admin`, `ql.hm001`, `cskh`, `marketing`, `danhmuc`). Mật khẩu chung `Demo@123`.

Dừng: `Ctrl+C` trong từng terminal.

---

## 3. Debug bằng VS Code (khuyên dùng)

### 3.1 Mở đúng thư mục
- **File → Open Folder → chọn `05_SourceCode`** (không mở riêng `backend`), để VS Code thấy cấu hình trong `.vscode/`.
- VS Code gợi ý cài extension (`.vscode/extensions.json`): **Extension Pack for Java** và **Spring Boot Extension Pack** → bấm Install.
- Đợi góc dưới hiện "Java: Ready" (lần đầu VS Code đọc `pom.xml`, mất 1–2 phút).

### 3.2 Các cấu hình có sẵn (`.vscode/launch.json`)
Mở khung **Run and Debug** (`Ctrl+Shift+D`), chọn cấu hình ở ô trên cùng rồi bấm ▶ (hoặc `F5`):

| Cấu hình | Dùng khi |
|---|---|
| **Backend: Debug (Spring Boot)** | Chạy backend ở chế độ Debug – dùng thường xuyên nhất |
| **Backend: Debug + log SQL** | Như trên, kèm in ra câu SQL Hibernate sinh và giá trị từng tham số (điều tra lỗi truy vấn) |
| **Backend: Gắn debug vào cổng 5005** | Backend đang chạy bằng task "Backend: chạy chờ gắn debug (cổng 5005)" – gắn debugger vào sau |
| **Frontend: Debug trên Chrome / Edge** | Mở trình duyệt có debugger; đặt breakpoint ngay trong file `.tsx` (cần chạy frontend trước) |
| **Cả hệ thống: Backend + Frontend** | Một lần bấm: chạy frontend, backend Debug, mở Chrome |

Các lệnh khác ở **Ctrl+Shift+P → Tasks: Run Task** (`.vscode/tasks.json`): chạy frontend, chạy test, build sạch…

### 3.3 Đặt breakpoint – ví dụ đăng ký thành viên
1. **Tắt mọi backend khác đang giữ cổng 8080** (terminal đang chạy `mvnw`, hoặc backend do Claude chạy). Nếu không, Debug sẽ báo `Port 8080 was already in use`.
2. Chọn **Backend: Debug (Spring Boot)** → `F5`. Chờ log `Started HmmLoyaltyApplication`.
3. Mở `backend/src/main/java/vn/sofitech/hmm/loyalty/controller/MemberController.java`, bấm vào **lề trái** dòng `CurrentUser u = CurrentUser.of(jwt);` trong hàm `register` → hiện chấm đỏ.
   - Đặt ở **dòng lệnh bên trong hàm**, không đặt ở dòng khai báo hàm.
4. Trên POS: tra cứu một SĐT **chưa là thành viên** (ví dụ `0869999010`) → form đăng ký mở → bấm **Đăng ký**. VS Code dừng ở dòng đó.
5. Điều khiển:

| Phím | Ý nghĩa |
|---|---|
| `F10` (Step Over) | Chạy dòng hiện tại, sang dòng kế |
| `F11` (Step Into) | Đi vào bên trong hàm đang gọi – ví dụ vào `CustomerService.registerAtPos` |
| `Shift+F11` (Step Out) | Chạy hết hàm hiện tại, quay ra nơi gọi |
| `F5` (Continue) | Chạy tiếp tới breakpoint sau |
| `Shift+F5` | Dừng hẳn backend |

6. Xem dữ liệu: khung **Variables** (rê chuột vào biến `req` để thấy `phone`, `fullName`…), **Watch** (thêm biểu thức như `req.getPhone()`), **Debug Console** (gõ biểu thức Java để tính thử).

**Breakpoint có điều kiện:** chuột phải chấm đỏ → *Edit Breakpoint* → nhập `req.getPhone().startsWith("0869")` – chỉ dừng khi đúng điều kiện.

**Sửa code khi đang debug:** sửa thân hàm rồi lưu, VS Code tự nạp lại (Hot Code Replace). Đổi chữ ký hàm, thêm field, sửa annotation thì phải dừng và chạy lại.

### 3.4 Debug frontend
- Cách nhanh: trong trình duyệt bấm `F12` → tab **Network** xem từng lời gọi `/api` (request gửi gì, response trả gì, mã lỗi 400/401/403/500); tab **Console** xem lỗi JavaScript.
- Breakpoint trong code React: chạy task **Frontend: npm run dev**, rồi cấu hình **Frontend: Debug trên Chrome** → đặt chấm đỏ trong file `.tsx` (ví dụ `frontend/src/pos/RegisterModal.tsx`, hàm `submit`).

### 3.5 Debug test
- Mở file test (ví dụ `backend/src/test/java/vn/sofitech/hmm/report/SearchFilterTest.java`), phía trên mỗi hàm có chữ **Run Test | Debug Test** → bấm *Debug Test*.
- Test tích hợp dùng database riêng `hmm_loyalty_test` (tự tạo lại mỗi lần), không đụng dữ liệu demo.

---

## 4. Debug bằng IntelliJ IDEA (nếu dùng)
1. **File → Open → chọn `05_SourceCode/backend`** (thư mục có `pom.xml`) → Trust project.
2. Mở `HmmLoyaltyApplication.java` → bấm biểu tượng con bọ cạnh hàm `main` → **Debug**.
3. **Bắt buộc:** *Run → Edit Configurations → HmmLoyaltyApplication → Working directory* = thư mục `backend` (để đọc `local.properties`).
4. Gắn vào backend chạy bằng Maven: *Run → Edit Configurations → + → Remote JVM Debug*, cổng `5005`.
5. Phím: `F8` Step Over, `F7` Step Into, `Shift+F8` Step Out, `F9` Resume.

---

## 5. Thử API bằng Swagger

Swagger UI là trang web liệt kê mọi API của backend và cho gọi thử ngay trên trình duyệt (không cần frontend, không cần Postman). Định nghĩa API sinh tự động từ code nên luôn khớp bản đang chạy.

- Mở **http://localhost:8080/swagger-ui.html** (backend phải đang chạy). Định nghĩa dạng máy đọc: `/v3/api-docs`.
- API nhóm theo controller (`auth-controller`, `pos-controller`, `report-controller`…). Ổ khóa bên phải = API cần token; API đăng nhập và danh sách quầy không cần.

**Bước 1 – Lấy token:** mở **POST /api/auth/login** → **Try it out** → sửa nội dung → **Execute** → chép giá trị `token` trong *Response body*.

| Muốn thử nhóm API | Nội dung đăng nhập |
|---|---|
| Web Portal (`/api/portal/...`) | `{"username": "admin", "password": "Demo@123"}` (hoặc `cskh`, `marketing`, `danhmuc`, `ql.hm001` để thử phân quyền) |
| POS (`/api/pos/...`) | `{"username": "tn.hm001.1", "password": "Demo@123", "terminalCode": "HM001-Q01"}` – bắt buộc có `terminalCode`; `terminalId` của quầy này là `1` |

**Bước 2 – Gắn token:** bấm nút **Authorize** (góc phải, hình ổ khóa) → dán token (chỉ dán chuỗi token, **không** gõ chữ `Bearer`) → **Authorize** → **Close**. Từ đó mọi lời gọi tự kèm header `Authorization: Bearer <token>`; token được giữ cả khi tải lại trang. Đổi tài khoản: **Authorize → Logout**, rồi dán token mới.

**Bước 3 – Gọi API:** mở API → **Try it out** → nhập tham số / nội dung → **Execute**. Kết quả hiện mã HTTP, nội dung trả về và lệnh `curl` tương đương (chép ra terminal để gọi lại).

**Ví dụ luồng POS:** đăng nhập thu ngân → `GET /api/pos/shifts/current?terminalId=1` (204 = chưa có ca) → `POST /api/pos/shifts/open` `{"terminalId": 1, "openingCash": 2000000}` → `GET /api/pos/members/lookup?q=0901234567` → `POST /api/pos/redemptions/quote` → `POST /api/pos/redemptions` → lấy OTP bằng `GET /api/pos/mock-phone?customerId=1` → `POST /api/pos/redemptions/{id}/verify` → `POST /api/pos/sales`. Mã mặt hàng (`productId`) lấy từ `GET /api/pos/products?barcode=8931002000144`.

**Lưu ý:**
- **401:** chưa bấm Authorize, token hết hạn (12 giờ) hoặc database vừa tạo lại → đăng nhập lấy token mới.
- **403:** đúng token nhưng sai vai trò, ví dụ token `admin` gọi `/api/pos/...` (cần token thu ngân), hoặc thu ngân gọi `/api/portal/...`.
- Gọi API trên Swagger là **ghi thật vào database** (`hmm_loyalty`), giống thao tác trên giao diện. Muốn dữ liệu sạch cho buổi demo thì tạo lại database sau khi thử.
- Đặc tả chi tiết từng API (tham số, kết quả, mã lỗi): tài liệu `03_Diagrams/SFT-AB-RTL-CRM-2026-001-API_Specification-NguyenAnhDuc.docx`.

---

## 6. Lỗi thường gặp

| Hiện tượng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Port 8080 was already in use` | Đã có backend khác chạy | Tắt backend kia. Tìm tiến trình: `netstat -ano \| findstr :8080` rồi `taskkill /PID <số> /F` |
| Breakpoint không dừng | Request đi vào backend khác (không phải bản Debug); hoặc request bị chặn trước khi vào hàm (401 hết phiên, 400 sai dữ liệu) | Kiểm tra chỉ còn bản Debug giữ cổng 8080; xem tab Network để biết mã lỗi |
| `Unresolved compilation problems` hoặc code chạy là bản cũ | IDE và Maven cùng ghi vào `backend/target/` | Task **Backend: build sạch** (`mvnw clean compile`), rồi chạy lại |
| `password authentication failed for user "postgres"` | Thiếu hoặc sai `local.properties`, hoặc chạy sai thư mục | Kiểm tra file `backend/local.properties`; thư mục chạy phải là `backend/` |
| Frontend báo "Không kết nối được máy chủ" | Backend chưa chạy hoặc đang khởi động | Chạy backend, chờ log `Started` |
| Đăng nhập xong bị đẩy về trang đăng nhập | Token hết hạn (12 giờ) hoặc backend đã tạo lại dữ liệu | Đăng nhập lại |
| Lỗi SQL lạ (ví dụ kiểu dữ liệu) | Cần xem câu SQL thật | Chạy **Backend: Debug + log SQL**, xem log trong terminal |
