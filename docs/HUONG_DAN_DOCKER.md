# Hướng dẫn chạy bản demo bằng Docker

Chạy toàn bộ hệ thống (PostgreSQL, Redis, backend, frontend) bằng **một lệnh**, không cần cài Java, Maven, Node.js hay PostgreSQL.
Cách chạy local (VS Code, `mvnw`, `npm run dev`) vẫn giữ nguyên: xem `README.md` và `docs/HUONG_DAN_RUN_DEBUG.md`.

---

## 1. Khái niệm cần biết

| Khái niệm | Hiểu đơn giản | Trong project |
|---|---|---|
| **Image** | "Bản đóng gói" chứa phần mềm và mọi thứ cần để chạy, giống file cài đặt đã cài sẵn | `postgres:17`, `redis:7`, `05_sourcecode-backend`, `05_sourcecode-frontend` |
| **Container** | Một image đang chạy, như một máy tính nhỏ riêng biệt | 4 container: `postgres`, `redis`, `backend`, `frontend` |
| **Dockerfile** | Công thức tạo image | `backend/Dockerfile`, `frontend/Dockerfile` |
| **Volume** | Ổ lưu dữ liệu do Docker quản lý, không mất khi xóa container | `pgdata` – dữ liệu PostgreSQL |
| **Docker Compose** | File mô tả nhiều container chạy cùng nhau | `docker-compose.yml` ở thư mục gốc |
| **Port** | Cổng mở từ máy bạn vào container: `cổng-máy:cổng-container` | `3000:80` (giao diện), `8081:8080` (backend, Swagger) |

## 2. Cài đặt

Chỉ cần **Docker Desktop** (docker.com → Download Docker Desktop for Windows).
- Bộ cài tự bật **WSL2**; máy cần bật ảo hóa (Virtualization) trong BIOS, phần lớn máy đã bật sẵn. Cài xong khởi động lại máy nếu được yêu cầu.
- Mở Docker Desktop, chờ trạng thái **Engine running**.
- Kiểm tra: `docker --version` và `docker compose version`.

## 3. Các file Docker trong project

```
05_SourceCode/
  docker-compose.yml       ghép 4 container, cổng, biến môi trường, volume, healthcheck
  backend/Dockerfile       bước 1: JDK 21 + Maven Wrapper build file .jar; bước 2: JRE 21 chạy app.jar
  backend/.dockerignore    bỏ target/ và local.properties (mật khẩu máy cá nhân) khi build
  frontend/Dockerfile      bước 1: Node 22 chạy npm ci + npm run build; bước 2: nginx phục vụ thư mục dist/
  frontend/nginx.conf      chuyển /api sang backend:8080; đường dẫn React (/pos, /portal/...) trả về index.html
  frontend/.dockerignore   bỏ node_modules/ và dist/ (được cài, build lại trong container)
```

**Luồng gọi giữa các container** (mạng riêng do Compose tạo, gọi nhau bằng tên dịch vụ):
```
Trình duyệt ─ localhost:3000 ─► frontend (nginx :80) ─ /api ─► backend :8080 ─► postgres :5432
                                                                    └──────────► redis :6379 (OTP)
             localhost:8081 ─► backend :8080 (Swagger)
```
- Chỉ mở ra máy bạn cổng **3000** và **8081**; PostgreSQL và Redis không mở ra ngoài nên không trùng với PostgreSQL cài trên máy (5432).
- Backend chạy với profile **`docker`** (`application-docker.yml`): OTP lưu trong Redis, cho phép giao diện ở cổng 3000 (CORS).
- **Healthcheck:** backend chỉ khởi động khi PostgreSQL và Redis đã sẵn sàng (`depends_on: condition: service_healthy`).

## 4. Chạy bằng dòng lệnh (CMD / PowerShell)

Mọi lệnh chạy trong thư mục `05_SourceCode`:
```bash
cd /d D:\Job\SofiTech\NguyenAnhDuc_Assignment_Loyalty_CRM\05_SourceCode
```

**Lần đầu, hoặc sau khi sửa code** – build image và chạy nền:
```bash
docker compose up -d --build
```
Lần đầu mất vài phút (tải image, thư viện Maven, npm); các lần sau nhanh nhờ cache.

**Chờ dữ liệu mẫu** – CSDL trong container còn trống nên backend tự sinh dữ liệu (khoảng 20 giây). Xem log đến khi có dòng `Demo data seeded`, rồi Ctrl+C để thoát xem log (container vẫn chạy):
```bash
docker compose logs -f backend
```

**Mở ứng dụng:**
- Giao diện: http://localhost:3000 – chọn POS (quầy `HM001-Q01`, thu ngân `tn.hm001.1`) hoặc Web Portal (`admin`). Mật khẩu `Demo@123`.
- Swagger: http://localhost:8081/swagger-ui.html – gọi `POST /api/auth/login`, copy `token`, bấm **Authorize**, dán token.

**Lệnh thường dùng:**

| Việc | Lệnh |
|---|---|
| Xem container đang chạy | `docker compose ps` |
| Xem log (thêm `-f` để theo dõi) | `docker compose logs backend` |
| Dừng tạm, giữ container | `docker compose stop` |
| Chạy lại sau khi dừng tạm | `docker compose start` |
| Tắt và xóa container, **giữ dữ liệu** | `docker compose down` |
| Tắt và **xóa cả dữ liệu** (lần sau sinh dữ liệu mẫu mới) | `docker compose down -v` |
| Build và chạy lại riêng backend sau khi sửa Java | `docker compose up -d --build backend` |
| Build và chạy lại riêng frontend sau khi sửa React | `docker compose up -d --build frontend` |
| Áp dụng thay đổi trong `docker-compose.yml` | `docker compose up -d` |
| Vào CSDL trong container | `docker compose exec postgres psql -U postgres -d hmm_loyalty` |
| Xem OTP đang lưu trong Redis | `docker compose exec redis redis-cli --scan` |

**Tạo lại dữ liệu demo sạch** (nên làm trước buổi demo):
```bash
docker compose down -v
```
```bash
docker compose up -d
```

## 5. Dùng giao diện Docker Desktop

Docker Desktop **không có nút build và chạy file `docker-compose.yml` lần đầu** – lần đầu (và mỗi khi sửa code) chạy `docker compose up -d --build` như mục 4. Có thể gõ lệnh ngay trong Docker Desktop: nút **Terminal** ở góc dưới bên phải.

Sau đó mọi việc làm bằng chuột:
1. Tab **Containers** → nhóm **`05_sourcecode`** gồm `postgres`, `redis`, `backend`, `frontend`.
2. Nút ▶ / ■ ở dòng nhóm: chạy / dừng cả hệ thống; ở từng dòng: từng container.
3. Dòng `frontend` có link **3000:80**, dòng `backend` có link **8081:8080** – bấm để mở trình duyệt.
4. Bấm tên container → tab **Logs** xem log (chờ `Demo data seeded` ở `backend`); tab **Exec** gõ lệnh bên trong container.
5. Biểu tượng thùng rác ở dòng nhóm = `docker compose down` (dữ liệu vẫn còn). Xóa dữ liệu: tab **Volumes** → xóa `05_sourcecode_pgdata`.
6. Tab **Images**: các image đã build; xóa image cũ để giải phóng ổ đĩa.

## 6. Khi nào cần build lại

| Bạn sửa | Cần làm |
|---|---|
| Code Java trong `backend/` | `docker compose up -d --build backend` |
| Code React trong `frontend/`, hoặc `nginx.conf` | `docker compose up -d --build frontend` |
| `docker-compose.yml` (cổng, biến môi trường) | `docker compose up -d` |
| Không sửa gì, chỉ bật lại | Nút ▶ trong Docker Desktop hoặc `docker compose start` |

Khi đang sửa code liên tục, chạy local (`mvnw spring-boot:run` + `npm run dev`) nhanh hơn vì không phải build image. Docker dùng cho buổi demo và để người khác chạy thử.

## 7. Chia sẻ cho người khác chạy

Người nhận **chỉ cần cài Docker Desktop**. Dữ liệu demo không được chép từ máy bạn: lần đầu chạy, backend tự sinh dữ liệu mẫu vào volume trên máy người nhận.

| Cách | Bạn làm | Người nhận làm |
|---|---|---|
| **Mã nguồn** (khuyên dùng) | Cấp quyền repo GitHub (hoặc gửi file zip mã nguồn) | `git clone …`, vào thư mục, `docker compose up -d --build` |
| **Registry** (Docker Hub, ghcr.io) | `docker tag` + `docker push` image backend, frontend; gửi bản `docker-compose.yml` dùng `image:` thay cho `build:` | Đặt file compose vào một thư mục bất kỳ, `docker compose up -d` (image private thì `docker login` trước) |
| **File image** | `docker save -o hmm-images.tar 05_sourcecode-backend 05_sourcecode-frontend`, gửi kèm compose dùng `image:` | `docker load -i hmm-images.tar`, rồi `docker compose up -d` |

**Image đã public trên Docker Hub (16/11/2026):** `ducnak3/hmm-backend:1.0`, `ducnak3/hmm-frontend:1.0` (kèm tag `latest`). File compose cho người nhận **`docker-compose.yml`** được Developer DucNA gửi riêng (dùng `image:`, có hướng dẫn ở đầu file): người nhận đặt file vào một thư mục trống rồi chạy `docker compose up -d`. Khi sửa code, build lại rồi đẩy bản mới:
```bash
docker compose build
```
```bash
docker tag 05_sourcecode-backend ducnak3/hmm-backend:1.1 && docker push ducnak3/hmm-backend:1.1
```
(tương tự cho frontend), rồi đổi số phiên bản trong file compose gửi người nhận.

Image chỉ chứa file `.jar` và file web đã build, không chứa mã nguồn hay `local.properties`. Mật khẩu `postgres` trong compose chỉ dùng cho demo; khi triển khai thật cần đưa mật khẩu, `JWT_SECRET` ra file `.env` (không đưa lên git).

## 8. Lỗi thường gặp

| Hiện tượng | Nguyên nhân và cách xử lý |
|---|---|
| `"/nginx.conf": not found` khi build frontend | Thiếu `frontend/nginx.conf` (Dockerfile có lệnh `COPY nginx.conf`) |
| Build frontend rất lâu, `transferring context: 2xx MB` | Thiếu `frontend/.dockerignore` nên gửi cả `node_modules`; thiếu file này còn làm `node_modules` bản Windows đè lên bản Linux |
| `port is already allocated` | Cổng 3000 hoặc 8081 đang bị chương trình khác dùng → tắt chương trình đó hoặc đổi vế trái trong `ports:` (ví dụ `"3001:80"`) |
| Mở localhost:3000 báo "Không kết nối được máy chủ" | Backend chưa xong khởi động hoặc đang sinh dữ liệu → `docker compose logs -f backend`, chờ `Demo data seeded` |
| Backend dừng ngay khi bật, log có lỗi kết nối CSDL | Xem `docker compose ps`: postgres phải ở trạng thái `healthy`; chạy lại `docker compose up -d` |
| `has no healthcheck configured` khi chạy `docker compose start` | Container được tạo từ bản `docker-compose.yml` cũ → chạy `docker compose up -d` để tạo lại container theo cấu hình mới (dữ liệu trong volume vẫn giữ) |
| Sửa code nhưng giao diện / API không đổi | Chưa build lại → `docker compose up -d --build <dịch vụ>` |
| Docker Desktop báo lỗi WSL hoặc không khởi động | Cập nhật WSL (`wsl --update` trong PowerShell quyền quản trị), bật ảo hóa trong BIOS, khởi động lại máy |
| Muốn xem Swagger qua cổng 3000 | nginx chỉ chuyển `/api`; dùng cổng 8081 |
