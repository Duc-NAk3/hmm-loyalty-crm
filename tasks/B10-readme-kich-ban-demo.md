# B10 – README và kịch bản demo · ✅ Xong (commit `66fbe35`)

## Đã làm
1. **`README.md`** viết lại đầy đủ:
   - yêu cầu cài đặt; 5 bước chạy lần đầu (local.properties, tạo database, backend, frontend, mở trang); tạo lại dữ liệu sạch;
   - bảng 13 tài khoản demo và 4 SĐT khách dùng để demo; mô tả dữ liệu sinh sẵn;
   - chạy test (36 test); kiến trúc tóm tắt (module, Facade, cấu trúc thư mục trong module);
   - bảng 19 FR Demo và màn hình tương ứng; giới hạn bản demo; cấu trúc thư mục; liên kết tới các tài liệu khác.
2. **`docs/KICH_BAN_DEMO.md`**: kịch bản khoảng 10 phút, 9 bước theo mục "Kiểm tra" của kế hoạch tổng thể:
   mở ca → khách mới đăng ký nhanh → Lan đổi điểm có OTP → khóa / hủy đổi điểm → Customer 360 → đổi giá có duyệt → nhập CSV → dashboard → phân quyền.
   Có phần chuẩn bị (3 cửa sổ trình duyệt), bảng mã vạch, số liệu mong đợi từng bước, xử lý sự cố.
3. `docs/HUONG_DAN_TEST_POS.txt`: không cần sửa thêm (đã cập nhật ở B7 và lần đổi tên DTO / controller).

## Đã kiểm tra (13/11/2026)
- **Làm theo README trên một database trống riêng** (`hmm_loyalty_b10`, không đụng `hmm_loyalty`): Flyway tạo 26 bảng, seeder chạy 19 giây; số liệu khớp README (3 cửa hàng, 6 quầy, 13 tài khoản, 13 ngành / 115 mặt hàng, 800 khách, 24.200 hóa đơn, 0 khách lệch sổ cái).
- **Chạy lại toàn bộ kịch bản qua đúng các API giao diện gọi** trên database đó: 31/31 mục đúng, gồm
  khách mới +12 điểm; Lan báo giá 109.000 / tối đa 545 điểm; OTP sai báo lỗi; giữ 200 điểm và xác thực lần đầu; hóa đơn 181.000 trừ 200 cộng 16; Hùng bị khóa;
  Customer 360 khớp sổ cái, sổ cái có HOLD/RELEASE/REDEEM/EARN; danhmuc tạo giá nhưng không duyệt được, admin duyệt, POS bán 20.000;
  nhập ALPHA 40/32/3/5 và Lan có nguồn POS_ALPHA; dashboard đổi theo bộ lọc; ql.hm001 chỉ thấy HM001; thu ngân không vào Portal; cskh không xem dashboard.
- **Mọi nhãn nút / thông báo** kịch bản nhắc tới đều có đúng chữ trong code frontend.
- Database thử đã xóa sau khi kiểm tra.

**Lưu ý:** số điểm của Lan ("khoảng 5.000") và hạng của một số khách phụ thuộc ngày chạy seeder (lịch sử tính tới hôm qua). Các con số trong từng bước kịch bản (127.000, +12, 545, −200/+16, 40/32/3/5) không phụ thuộc ngày.
