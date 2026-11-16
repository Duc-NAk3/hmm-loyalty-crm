# B11 – Docker · ✅ Xong (16/11/2026, `cc41acb`)

Người dùng tự học Docker, tự viết `backend/Dockerfile`, `frontend/Dockerfile`, `docker-compose.yml` theo mẫu; Claude bổ sung phần còn thiếu và kiểm tra.

**Đã làm:**
1. **`backend/Dockerfile`:** build nhiều bước – JDK 21 + Maven Wrapper build `.jar`, JRE 21 chạy `app.jar`.
2. **`frontend/Dockerfile`:** Node 22 `npm ci` + `npm run build`, nginx phục vụ `dist/`.
3. **`frontend/nginx.conf`:** `/api/` → `http://backend:8080`; `try_files … /index.html` cho đường dẫn React; `client_max_body_size 10m` cho file CSV.
4. **`.dockerignore`:** backend bỏ `target`, `local.properties`; frontend bỏ `node_modules`, `dist` (thiếu file này lần build đầu gửi 226 MB, mất 230 giây).
5. **`docker-compose.yml`:** `postgres:17` (volume `pgdata`, healthcheck `pg_isready`), `redis:7` (healthcheck `redis-cli ping`), `backend` (profile `docker`, cổng `8081:8080` cho Swagger, chờ postgres + redis healthy), `frontend` (cổng `3000:80`); `restart: unless-stopped`.
6. **`docs/HUONG_DAN_DOCKER.md`:** khái niệm, cài Docker Desktop, lệnh, giao diện Docker Desktop, khi nào build lại, chia sẻ (git, registry, `docker save`), lỗi thường gặp. README có mục 0 "Chạy nhanh bằng Docker".

**Kiểm tra (16/11/2026, Docker 29.9):**
- `docker compose config` hợp lệ.
- Chạy từ đầu bằng project riêng `-p hmmtest` (không đụng dữ liệu người dùng): postgres, redis healthy trước khi backend khởi động; backend lên trong 13 giây, sinh dữ liệu mẫu 18 giây (800 khách, khoảng 24.500 hóa đơn).
- http://localhost:3000/pos → 200; Swagger http://localhost:8081 → 200; đăng nhập, tra cứu thành viên, báo giá, gửi OTP qua nginx → 200; OTP nằm trong Redis (`otp:<id>`).
- Đã xóa project thử (`down -v`), tạo lại bộ của người dùng theo compose mới bằng `up -d`; dữ liệu cũ giữ nguyên (không sinh lại).
