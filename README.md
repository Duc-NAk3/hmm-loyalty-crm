# Hoa Mai Mart – Loyalty CRM (bản demo)

Bản demo chứng minh khái niệm của SofiTech cho hệ thống Loyalty CRM có phân hệ bán hàng tại quầy (POS) của chuỗi Hoa Mai Mart.
Phạm vi: 19 yêu cầu chức năng có đánh dấu Demo trong tài liệu SRS.

| Thành phần | Công nghệ |
|---|---|
| `backend/` | Spring Boot 4 (Java 21), PostgreSQL, Flyway, Spring Security (JWT), Spring Modulith (kiểm tra ranh giới module) |
| `frontend/` | React 19 + Vite + TypeScript + Ant Design (POS và Web Portal) |

## Chạy local (tạm thời – bản đầy đủ ở bước README)
Hướng dẫn chạy và debug bằng VS Code / IntelliJ (breakpoint, log SQL, lỗi thường gặp): [docs/HUONG_DAN_RUN_DEBUG.md](docs/HUONG_DAN_RUN_DEBUG.md). Cấu hình VS Code có sẵn trong `.vscode/`.

1. **Backend:** tạo file `backend/local.properties` với dòng `DB_PASSWORD=<mật khẩu postgres>` và tạo database `hmm_loyalty`. Sau đó chạy `cd backend` rồi `.\mvnw.cmd spring-boot:run` → http://localhost:8080 (lần đầu tự tạo dữ liệu mẫu).
2. **Frontend:** `cd frontend`, chạy `npm install` (lần đầu), rồi `npm run dev` → http://localhost:5173.

> Đang phát triển. Tài khoản demo và kịch bản demo sẽ được bổ sung khi hoàn thiện.
