# B9 – Portal: mặt hàng & giá, chuyển đổi dữ liệu, người dùng, hộp thư SMS, hạng · ✅ Xong (commit `e4a366c`)

**FR:** FR-11 (mặt hàng, nhập CSV), FR-12 (giá có duyệt), FR-58/59/60 (chuyển đổi dữ liệu), FR-65 (người dùng), FR-31 (bảng hạng, nút chạy xét hạng).

Backend không đổi; chỉ làm frontend. Portal không còn trang `Placeholder` (đã xóa).

## File trong `frontend/src/portal/`
| File | Menu | Vai trò được vào | Nội dung |
|---|---|---|---|
| `Products.tsx` | Mặt hàng và giá | mọi vai trò Portal (sửa: CATALOG, ADMIN; duyệt: ADMIN) | Tab **Danh sách**: tìm, lọc ngành hàng, phân trang; drawer tạo/sửa (nhiều mã vạch, VAT, cờ hạn chế KM, không tích điểm, đang bán) + **lịch sử giá** + tạo bản giá Nháp (toàn chuỗi hoặc một cửa hàng, hiệu lực từ). Nút **Nhập CSV**. Tab **Giá chờ duyệt** (badge số lượng) với nút Duyệt / Hủy cho ADMIN. |
| `Migration.tsx` | Chuyển đổi dữ liệu | ADMIN | Chọn cửa hàng + POS cũ (hiện dấu phân cách, định dạng ngày, cột) + file → kết quả tổng / tạo mới / gộp / lỗi và bảng dòng lỗi kèm dữ liệu gốc; danh sách các lần nhập, bấm số lỗi để xem lại. |
| `Users.tsx` | Người dùng | ADMIN | Lọc vai trò; tạo / sửa (vai trò, cửa hàng bắt buộc với thu ngân và quản lý cửa hàng, đặt lại mật khẩu); khóa / mở khóa (không tự khóa mình). |
| `Tiers.tsx` (**mục menu mới** `tiers`) | Hạng thành viên | ADMIN, STORE_MANAGER, CSKH, MARKETING | Bảng hạng (ngưỡng, hệ số, số thành viên, quyền lợi), chính sách điểm đang áp dụng; ADMIN có nút **Chạy xét hạng**. |
| `SmsInbox.tsx` | Hộp thư SMS giả lập | mọi vai trò Portal | 100 tin mới nhất, lọc theo SĐT, tự làm mới 5 giây (tắt được). |

## Đã kiểm tra (12/11/2026)
- `npm run build` không lỗi.
- **Giá:** `danhmuc` tạo bản giá 20.000đ cho RAU001 → vào danh sách chờ; `danhmuc` gọi duyệt bị 403. `admin` duyệt trên giao diện → API POS trả RAU001 = 20.000đ ở HM001 và HM003.
- **Chuyển đổi dữ liệu:** ALPHA nhập qua giao diện, BETA và GAMMA qua cùng API: 40/32/3/5, 40/33/2/5, 40/33/2/5 – đúng bảng trong `sample-data/README.md`.
- **Nhập mặt hàng CSV:** 6 tạo mới, 1 cập nhật (RAU001 giá 19.000đ), 1 lỗi "ngành hàng KHONGCO không tồn tại" – đúng README.
- **Người dùng:** khóa `tn.hm003.2` → đăng nhập báo "Tài khoản đã bị khóa"; mở khóa → đăng nhập được; tự khóa mình và tạo thu ngân không có cửa hàng đều bị từ chối; form báo thiếu trường bằng tiếng Việt.
- **Xét hạng:** chạy trên giao diện, kiểm tra 899 khách (801 + 98 vừa nhập).
- **Hộp thư SMS:** hiện OTP và tin chào mừng từ các lượt test B7.
- Thu ngân gọi API Portal bị 403.

**Lưu ý dữ liệu:** các lượt test trên đã sửa database local (98 khách nhập từ CSV, 6 mặt hàng mới, giá RAU001 hiện là 19.000đ theo file CSV, bản giá nháp 19.100đ của dữ liệu mẫu vẫn chờ duyệt). Trước buổi demo nên tạo lại database `hmm_loyalty` (xem `../CLAUDE.md`).
