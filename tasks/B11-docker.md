# B11 – Docker · ⬜ Chưa làm

Người dùng chưa cài Docker và chưa biết Docker. Họ muốn có hướng dẫn học nhanh, dưới 5 giờ.

**Việc cần làm:**
1. **`backend/Dockerfile`:** build nhiều bước, Maven → JRE 21.
2. **`frontend/Dockerfile`:** build bằng Node, chạy bằng nginx; nginx chuyển `/api` sang backend.
3. **`docker-compose.yml`:**
   - Các dịch vụ: `postgres:17` (có volume), `redis`, `backend` (`SPRING_PROFILES_ACTIVE=docker`, `DB_HOST=postgres`, `DB_PASSWORD`), `frontend` (cổng 3000).
   - Profile `docker` đã có trong `application-docker.yml`: OTP lưu Redis qua `OtpStore.Redis`; CORS cho cổng 3000.
4. **`HUONG_DAN_DOCKER.md`:**
   - Khái niệm image, container, volume, compose.
   - Cài Docker Desktop trên Windows 10 (cần WSL2, người dùng tự cài).
   - Các lệnh `docker compose up -d`, `logs`, `ps`, `down`, `down -v`.
   - Cách tạo lại dữ liệu demo; lỗi thường gặp (trùng cổng 5432/8080 với PostgreSQL và backend chạy local).

**Kiểm tra:** máy chưa có Docker nên chưa chạy thử được. Khi người dùng cài xong thì chạy thử cùng nhau. Có thể kiểm tra cú pháp bằng `docker compose config`.
