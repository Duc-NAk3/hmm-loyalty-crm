# B8 – Portal: Dashboard KPI, khách hàng, Customer 360 · ⬜ Chưa làm (làm tiếp theo)

**FR:** FR-51 (Dashboard), FR-22 (Customer 360), FR-18 (tìm khách).

## Backend đã có sẵn (module `report`)
| API | Mô tả |
|---|---|
| `GET /api/portal/dashboard?from&to&storeId` | Vai trò ADMIN, STORE_MANAGER, MARKETING; quản lý cửa hàng tự bị giới hạn cửa hàng của mình. Trả `{ kpi, tiers, byStore, byDay, categories }`. |
| `GET /api/portal/customers?q&tier&status&storeId&page&size` | `{ total, items[] }`; SĐT đã che |
| `GET /api/portal/customers/{id}/360` | `{ profile, stats, transactions, ledger, consents, tierHistory, sources }` |
| `GET /api/portal/customers/{id}/transactions/{txnId}/lines` | Dòng hàng của một hóa đơn |
| `POST /api/portal/customers/{id}/reveal-phone` | Xem SĐT đầy đủ (ADMIN, CSKH); có ghi audit |

## Việc cần làm (frontend `src/portal/`)
1. **Trang `dashboard`:**
   - Bộ lọc khoảng ngày và cửa hàng.
   - Thẻ KPI: thành viên, tỷ lệ định danh, tỷ trọng doanh thu thành viên, tỷ lệ quay lại, giá trị giỏ.
   - Biểu đồ Recharts: doanh thu theo ngày (thành viên / vãng lai), theo cửa hàng, phân bố hạng, top ngành hàng.
   - **Trước khi vẽ biểu đồ phải đọc skill `dataviz`.**
2. **Trang `customers`:** bảng tìm kiếm (từ khóa, hạng, trạng thái, phân trang) → bấm vào mở Customer 360.
3. **Customer 360:** hồ sơ, tiến độ lên hạng, các tab Hóa đơn (mở xem dòng hàng) / Sổ cái điểm / Consent / Lịch sử hạng / Nguồn dữ liệu; nút "Xem SĐT đầy đủ".
4. Thay `Placeholder` trong `App.tsx`.

## Kiểm tra
- `npm run build` không lỗi.
- Thử trên trình duyệt bằng `admin` và `ql.hm001` (chỉ thấy HM001), `cskh` (không thấy dashboard).
- Customer 360 của Lan: tổng sổ cái bằng số dư.
