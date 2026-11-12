# B1 – Khung kiến trúc backend · ✅ Xong (commit `f5ed4e7`, đã có trên `main`)

**Đã làm:**
- Spring Boot 4.0.9 (Java 21, Maven Wrapper); Initializr không còn bản 3.5.
- Flyway `V1__schema.sql`: 26/42 bảng theo ERD (tên bảng số nhiều), có index, trigger chặn sửa/xóa sổ cái, ràng buộc 1 ca mở mỗi quầy.
- Bảo mật: JWT HS256, bcrypt, phân quyền theo vai trò.
- Các API nghiệp vụ ban đầu (bản SQL thuần; đã thay bằng ORM ở B6.B).

**Ghi chú:** chạy lần đầu cần file `backend/local.properties` với dòng `DB_PASSWORD=...` và database `hmm_loyalty`.
