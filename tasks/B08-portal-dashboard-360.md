# B8 – Portal: Dashboard KPI, khách hàng, Customer 360 · ✅ Xong (commit `1853564`)

**FR:** FR-51 (Dashboard), FR-22 (Customer 360), FR-18 (tìm khách trên Portal).

Backend không đổi (module `report` đã có từ B1/B6.B); chỉ làm frontend.

## File trong `frontend/src/`
| File | Vai trò |
|---|---|
| `portal/Dashboard.tsx` | Bộ lọc khoảng ngày (có sẵn 7/30/90 ngày, tháng này) và cửa hàng – một hàng phía trên. Quản lý cửa hàng: ô cửa hàng bị khóa, backend tự giới hạn. 6 thẻ KPI. Biểu đồ: doanh thu theo ngày (cột chồng thành viên / vãng lai, có nút chuyển sang **bảng số liệu**), theo cửa hàng (thanh ngang chồng; ẩn khi chỉ có 1 cửa hàng), phân bố hạng và top ngành hàng (thanh ngang một màu, kèm số và %). |
| `portal/Customers.tsx` | Tìm theo SĐT / mã / họ tên, lọc hạng, trạng thái, phân trang 20; bộ lọc lưu trên URL. Bấm dòng → Customer 360. |
| `portal/Customer360.tsx` | Hồ sơ; nút "Xem SĐT đầy đủ" (ADMIN, CSKH – có audit); số dư + dấu "khớp tổng sổ cái"; điểm sắp hết hạn; thanh tiến độ lên hạng. Tab: Hóa đơn (mở dòng để tải từng mặt hàng), Sổ cái điểm, Consent, Lịch sử hạng, Nguồn dữ liệu. |
| `shared/chart.ts` | Màu và kiểu chung cho biểu đồ. |
| `shared/format.ts` | Thêm `compactVnd` (764,1 tr / 1,2 tỷ) và `pct`. |
| `App.tsx` | Route `dashboard`, `customers`, `customers/:id`; các mục còn lại vẫn là `Placeholder`. |

## Quy tắc biểu đồ (theo skill `dataviz`)
- **Màu cố định theo thực thể:** Thành viên `#2a78d6` (slot 1), Vãng lai `#eb6834` (slot 2). Đã chạy `validate_palette.js` trên nền `#ffffff`: tất cả đạt (CVD ΔE 24,7).
- Biểu đồ một chuỗi dùng một màu; không tô đậm nhạt theo độ lớn cho danh mục không có thứ tự.
- Cột tối đa 24px, góc bo 4px ở đầu dữ liệu, khe 2px màu nền giữa hai đoạn chồng; lưới và trục là đường mảnh nhạt; không dùng hai trục Y.
- Có chú giải khi ≥ 2 chuỗi; tooltip khi rê chuột; chữ dùng màu chữ, không dùng màu chuỗi.
- Không làm chế độ tối vì toàn app (Ant Design) đang chỉ có giao diện sáng.

## Đã kiểm tra (12/11/2026)
- `npm run build` không lỗi.
- `admin`: KPI 30 ngày khớp API (764,1 tr, 68,1% hóa đơn có định danh, 66,8% doanh thu thành viên, 63,1% quay lại, 801 thành viên); tooltip theo ngày; bảng số liệu.
- Khách hàng: 801 khách, tìm và lọc chạy; Customer 360 của Lan: số dư 5.574 khớp tổng sổ cái, 148 hóa đơn, hóa đơn POS B7 (−200 / +20) mở được từng dòng hàng; sổ cái có EARN hết hạn 31/10/2027; xem SĐT đầy đủ có thông báo ghi nhật ký.
- `ql.hm001`: ô cửa hàng khóa ở HM001; gọi API xin `storeId=2` vẫn nhận HM001; không xem được SĐT đầy đủ (403).
- `cskh`: menu không có Dashboard, mở `/portal/dashboard` tự chuyển sang Khách hàng; API dashboard trả 403.

## Còn lưu ý
- Customer 360 không giới hạn theo cửa hàng với quản lý cửa hàng (danh sách thì có): khách là của toàn chuỗi, giữ như SRS.
- Dashboard: dữ liệu mẫu có thể có ngày trống tùy ngày chạy seeder.
