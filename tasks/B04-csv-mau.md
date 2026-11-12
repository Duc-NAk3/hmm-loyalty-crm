# B4 – CSV mẫu · ✅ Xong (commit `9707298`)

Thư mục `sample-data/`:
- 3 file khách hàng: `customers_POS_ALPHA_HM001.csv`, `..._BETA_HM002.csv`, `..._GAMMA_HM003.csv`.
- 1 file nhập mặt hàng: `products_import.csv`.

Kết quả mong đợi ghi trong `sample-data/README.md`:
| File | Tạo mới | Gộp | Lỗi |
|---|---|---|---|
| ALPHA | 32 | 3 | 5 |
| BETA | 33 | 2 | 5 |
| GAMMA | 33 | 2 | 5 |
| Mặt hàng | 6 | 1 cập nhật | 1 |

Giao diện nhập file thuộc **B9** (Portal → Chuyển đổi dữ liệu, Mặt hàng → Nhập CSV). API đã có sẵn.
