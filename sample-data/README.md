# Dữ liệu mẫu để demo nhập dữ liệu

Các file giả lập dữ liệu "xuất từ POS cũ" (FR-58 – FR-60) và file nhập mặt hàng (FR-11).
Sinh tự động, không phải dữ liệu thật. SĐT khách mới đều có dạng `0869xxxxxx` để không trùng với dữ liệu demo.

## Khách hàng từ 3 POS cũ
Nhập tại **Portal → Chuyển đổi dữ liệu** (tài khoản `admin`): chọn đúng cửa hàng và nhà cung cấp.

| File | Cửa hàng | Nhà cung cấp | Định dạng |
|---|---|---|---|
| `customers_POS_ALPHA_HM001.csv` | HM001 | POS_ALPHA | dấu `,`; cột `ma_kh, ho_ten, so_dien_thoai, ngay_sinh (dd/MM/yyyy), email, gioi_tinh (Nam/Nữ)` |
| `customers_POS_BETA_HM002.csv` | HM002 | POS_BETA | dấu `;`; cột `CustomerCode; FullName; Mobile; Birthday (yyyy-MM-dd); Email; Gender (M/F)` |
| `customers_POS_GAMMA_HM003.csv` | HM003 | POS_GAMMA | dấu `,`; cột `id, name, phone, dob (dd-MM-yyyy), mail, sex (male/female)` |

**Mỗi file có 40 dòng, gồm các tình huống cần kiểm tra:**
- **Dòng 3 – 5:** SĐT viết dạng `+84 …`, `84…`, có dấu chấm. Hệ thống tự chuẩn hóa về `0…`.
- **Dòng 6:** họ tên viết hoa, thừa khoảng trắng. Hệ thống chuẩn hóa thành chữ hoa đầu từ.
- **Dòng 12 – 13:** trùng SĐT với khách đã có trong hệ thống, nên được gộp vào hồ sơ có sẵn:
  - ALPHA: Nguyễn Thị Lan, Trần Văn Hùng.
  - BETA: Phạm Minh Tuấn.
  - GAMMA: Nguyễn Thị Lan.
- **Dòng 22:** trùng SĐT với dòng 7 ngay trong file, nên được gộp vào hồ sơ vừa tạo.
- **Dòng 32 – 36 là dòng lỗi:** SĐT cố định, thiếu họ tên, ngày sinh không tồn tại hoặc sai định dạng, email sai, SĐT thiếu số.

**Kết quả mong đợi khi nhập lần đầu:**

| File | Tổng dòng | Tạo mới | Gộp | Lỗi |
|---|---|---|---|---|
| ALPHA | 40 | 32 | 3 | 5 |
| BETA | 40 | 33 | 2 | 5 |
| GAMMA | 40 | 33 | 2 | 5 |

Khách tạo mới ở trạng thái **Chưa xác thực**, chưa có consent marketing. Bản ghi gốc của từng dòng được lưu lại; xem ở mục "Nguồn dữ liệu" trong Customer 360.

## Mặt hàng
Nhập tại **Portal → Mặt hàng → Nhập CSV** (tài khoản `danhmuc` hoặc `admin`).

`products_import.csv` có các cột `sku, name, unit, category_code, vat_rate, barcodes, promo_restricted, point_excluded, price`:
- `barcodes`: nhiều mã vạch cách nhau bằng `|`.
- `price`: tạo giá toàn chuỗi đã duyệt, hiệu lực ngay.

Kết quả mong đợi:
- **6 mặt hàng mới:** gồm 1 mặt hàng hạn chế khuyến mại (sữa cho trẻ dưới 24 tháng) và 1 thẻ nạp không tích điểm.
- **1 mặt hàng được cập nhật:** `RAU001` đổi giá thành 19.000đ, giữ nguyên mã vạch cũ.
- **1 dòng lỗi:** ngành hàng không tồn tại.
