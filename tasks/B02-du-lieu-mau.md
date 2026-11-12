# B2 – Dữ liệu mẫu · ✅ Xong (commit `fb8189c`; ghi lại bằng JPA ở `cb0d614`)

**File:** `backend/src/main/java/vn/sofitech/hmm/demo/DemoDataSeeder.java`, `DemoCatalog.java`.

**Nội dung dữ liệu:**
- 3 cửa hàng HM001–003, 6 quầy `HMxxx-Q01/Q02`.
- 13 tài khoản / 6 vai trò, mật khẩu `Demo@123`.
- 115 mặt hàng / 13 ngành hàng.
- 800 khách; khoảng 24.000 hóa đơn từ 01/12/2025 đến hôm qua.
- Điểm, lô điểm, sổ cái và hạng tính bằng `PointCalculator`.

**Khách cố định cho kịch bản demo:**
| SĐT | Khách | Dùng để demo |
|---|---|---|
| 0901234567 | Nguyễn Thị Lan | DIAMOND, chưa xác thực – OTP lần đầu chuyển sang VERIFIED |
| 0912345678 | Trần Văn Hùng | Đang bị khóa đổi điểm |
| 0935123456 | Phạm Minh Tuấn | Khách GOLD, có ít điểm |
| 0987654321 | (chưa có) | Đăng ký mới tại quầy |
| `0869xxxxxx` | (chưa có) | Dành cho file CSV chuyển đổi |

**Lưu ý:**
- Chỉ chạy khi CSDL trống. Muốn tạo lại thì `DROP DATABASE hmm_loyalty WITH (FORCE)`, `CREATE DATABASE hmm_loyalty`, rồi chạy backend; mất khoảng 20 giây.
- Số liệu phụ thuộc ngày chạy, vì lịch sử tính tới hôm qua.
