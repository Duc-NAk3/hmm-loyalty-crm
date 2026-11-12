# B3 – Test tự động · ✅ Xong (commit `52baf9f`; bổ sung ở B6.B)

Chạy `.\mvnw.cmd test` trong `backend/` → hiện có **33 test**:

| Test | Kiểm tra |
|---|---|
| `loyalty/PointCalculatorTest` | Công thức BRL-03/04/08/09/10/11 |
| `shared/TextsTest` | Chuẩn hóa và che SĐT, chuẩn hóa họ tên |
| `loyalty/LoyaltyIntegrationTest` | Sổ cái, giữ/trả/đổi điểm FIFO, sổ cái chỉ ghi thêm, OTP, khóa đổi điểm |
| `catalog/RepositoryQueryTest` | Giá hiệu lực theo cửa hàng, duyệt giá, truy vấn lô FIFO |
| `ModularityTest` | Spring Modulith: module chỉ gọi nhau qua Facade |

**Lưu ý:**
- Test tích hợp kế thừa `PostgresTestBase`: dùng database `hmm_loyalty_test`, tạo lại mỗi lần chạy, tự bỏ qua nếu không có PostgreSQL.
- Ở B7–B9, thêm test cho luồng mới nếu có logic backend mới.
