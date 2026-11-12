# B6.B – ORM và design pattern · ✅ Xong

**Commit (đều trên `Dev`):**
| Commit | Nội dung |
|---|---|
| `76f5744` B6.B-1 | Spring Data JPA, entity và repository cho 26 bảng, `V2__jpa_sequences.sql` |
| `182f93f` B6.B-2a | Backend Facade / Service / Repository / DTO (camelCase); bỏ JdbcClient |
| `3bbdf39` B6.B-2b | Frontend POS đổi sang camelCase |
| `cb0d614` B6.B-3 | Seeder ghi bằng JPA, `PostgresTestBase`, `RepositoryQueryTest`, cập nhật tài liệu |

**Kiến trúc hiện tại:** xem mục Backend trong `../CLAUDE.md`.
- Mỗi module có `<Module>Facade` ở gốc package, cùng các thư mục `web/`, `service/`, `domain/`.
- Pattern dùng: Layered, Repository, DTO, Facade, rich domain + State (`PointRedemption`), Strategy (`OtpStore`, `VendorFormat`), Observer (`SmsRequestedEvent`), entity bất biến (`PointLedger`, `AuditLog`).

**Bài học kỹ thuật (đọc trước khi viết code mới):**
- **Thứ tự ghi khi tham chiếu khác module:** liên kết khác module chỉ là ID, Hibernate không biết thứ tự khóa ngoại. Phải `saveAndFlush` bản ghi cha trước khi ghi bản ghi tham chiếu (ví dụ `SaleService` ghi hóa đơn trước sổ cái).
- **Tham số null trong JPQL:** truyền null vào `function(...)` làm PostgreSQL không đoán được kiểu (lỗi "Unknown Types value"). Tách thành hai truy vấn, như `OutboxMessageRepository`.
- **So sánh thời gian trong test:** PostgreSQL làm tròn tới micro giây và trả về theo UTC. So bằng `toInstant()`, dữ liệu test cắt tới giây.
- **Chỉ ghi thêm:** `@Immutable` cho entity chỉ ghi thêm. Test dọn dữ liệu bằng `TRUNCATE ... CASCADE`, vì trigger chặn DELETE trên sổ cái.
