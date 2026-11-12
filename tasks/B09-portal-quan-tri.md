# B9 – Portal: mặt hàng & giá, chuyển đổi dữ liệu, người dùng, hộp thư SMS · ⬜ Chưa làm (làm tiếp theo)

**FR:** FR-11 (mặt hàng, nhập CSV), FR-12 (giá có duyệt), FR-58/59/60 (chuyển đổi dữ liệu), FR-65 (người dùng), FR-31 (nút chạy xét hạng).

## Backend đã có sẵn
| Nhóm | API |
|---|---|
| Mặt hàng | `GET /api/portal/categories`<br>`GET /api/portal/products?q&categoryId&page&size`<br>`POST`/`PUT /api/portal/products` (vai trò CATALOG, ADMIN)<br>`POST /api/portal/products/import` (file) |
| Giá | `GET /api/portal/products/{id}/prices`<br>`POST /api/portal/products/{id}/prices` `{ storeId?, price, effectiveFrom? }` → bản DRAFT<br>`GET /api/portal/prices/pending`<br>`POST /api/portal/prices/{id}/approve` hoặc `/reject` (ADMIN) |
| Chuyển đổi dữ liệu (ADMIN) | `GET /api/portal/migration/vendors`<br>`POST /api/portal/migration/customers?storeId&vendor` (file)<br>`GET /api/portal/migration/batches`<br>`GET /api/portal/migration/batches/{id}/errors` |
| Người dùng (ADMIN) | `GET`/`POST /api/portal/users`<br>`PUT /api/portal/users/{id}`<br>`POST /api/portal/users/{id}/status`<br>`GET /api/portal/users/roles`<br>`GET /api/portal/stores` |
| Khác | `GET /api/portal/sms-outbox?phone`<br>`GET /api/portal/audit-logs` (ADMIN)<br>`GET /api/portal/loyalty/tiers`<br>`POST /api/portal/loyalty/tier-review` (ADMIN) |

## Việc cần làm (frontend `src/portal/`)
1. **`products`:**
   - Bảng mặt hàng; form tạo/sửa (nhiều mã vạch, cờ hạn chế khuyến mại, không tích điểm).
   - Upload CSV.
   - Lịch sử giá, tạo giá mới, danh sách chờ duyệt và nút duyệt (ADMIN).
2. **`migration`:** chọn cửa hàng + nhà cung cấp + file CSV trong `sample-data/` → hiện kết quả (tạo mới / gộp / lỗi), danh sách lần nhập và dòng lỗi.
3. **`users`:** bảng người dùng, tạo, sửa, khóa/mở khóa.
4. **`sms`:** hộp thư SMS giả lập, lọc theo SĐT, tự làm mới mỗi vài giây.
5. **Hạng:** đặt bảng hạng và nút "Chạy xét hạng" (ADMIN) ở trang phù hợp, ví dụ trang dashboard hoặc một mục cấu hình.

## Kiểm tra
- Nhập 3 file CSV mẫu: kết quả đúng bảng trong `sample-data/README.md`.
- Tạo giá mới cho RAU001, duyệt → POS bán giá mới.
- `danhmuc` tạo được giá nhưng không duyệt được.
- Thu ngân không vào được Portal.
