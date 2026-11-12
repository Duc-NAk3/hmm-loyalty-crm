# B7 – POS: thành viên và đổi điểm · ⬜ Chưa làm (làm tiếp theo)

**FR:** FR-03 (gắn thành viên), FR-18 (tra cứu), FR-19 (đăng ký nhanh), FR-20 (xác thực SĐT bằng OTP lần đầu), FR-30 (đổi điểm), FR-04 (thanh toán tiền mặt và điểm).

## Backend đã có sẵn – chỉ cần làm frontend
| API | Mô tả |
|---|---|
| `GET /api/pos/members/lookup?q=` | SĐT (+84/84/0) hoặc mã HM… → `MemberSummary`: `customerId`, `memberCode`, `fullName`, `phoneMasked`, `status`, `verified`, `redeemLocked`, `tierName`, `tierMultiplier`, `pointBalance`, `pointValueVnd`, `expiringPoints`, `expiringAt`, `nextTierName`, `spendToNextTier`. Không có thì trả 404 "Chưa là thành viên". |
| `POST /api/pos/members` | `{ phone, fullName, gender?, dateOfBirth?, marketingConsent }` → `MemberSummary`. Đồng thời gửi SMS chào mừng giả lập. |
| `POST /api/pos/redemptions/quote` | `{ customerId, lines }` → `{ allowed, maxPoints, maxAmount, reason, redeemableAmount }` |
| `POST /api/pos/redemptions` | `{ customerId, terminalId, points, lines }` → `{ redemptionId, points, amountVnd, status: "QUOTED", phoneMasked, otpTtlMinutes }`. Đồng thời gửi OTP giả lập. |
| `POST /api/pos/redemptions/{id}/verify` | `{ terminalId, otp }` → `{ status: "HELD", holdExpiresAt, balanceAfterHold, phoneVerified }`. Sai OTP trả 400; sai 3 lần thì hủy yêu cầu. |
| `POST /api/pos/redemptions/{id}/cancel` | `{ terminalId }` → trả lại điểm |
| `GET /api/pos/mock-phone?customerId=` | 5 SMS mới nhất gửi tới khách (công cụ demo: "điện thoại khách giả lập") |
| `POST /api/pos/sales` | Thêm `customerId` và `redemptionId` vào body; hóa đơn trả về có `memberCode`, `pointsEarned`, `pointsRedeemed`, `pointBalance`, `tierName`, `upgradedTo` |

## Việc cần làm (frontend `src/pos/`)
1. **Khung thành viên trong `SaleScreen.tsx`:** ô SĐT/mã + nút tra cứu; hiện thẻ thành viên gồm tên, SĐT che, hạng, điểm, điểm sắp hết hạn, nhãn Chưa xác thực / bị khóa; nút bỏ gắn.
2. **Không tìm thấy:** mở modal đăng ký nhanh (SĐT điền sẵn, họ tên bắt buộc, giới tính và ngày sinh tùy chọn, ô consent marketing).
3. **Đổi điểm:**
   - Nút "Đổi điểm" → gọi quote → chọn số điểm (mặc định tối đa, thấp nhất 50).
   - Gửi OTP → nhập OTP → HELD, hiện đồng hồ đếm 15 phút → nút hủy (gọi cancel).
   - Khi đổi giỏ hàng sau khi đã giữ điểm: hủy yêu cầu và báo đổi lại.
4. **Thanh toán:** truyền `pointsAmount` vào `PaymentModal`; body gửi `customerId` và `redemptionId`.
5. **Panel "Điện thoại khách (giả lập)":** đọc `mock-phone` để lấy OTP khi demo; ghi rõ đây là công cụ demo.
6. **Phím tắt gợi ý:** F4 tra cứu thành viên.
7. **Cập nhật tài liệu:** `docs/HUONG_DAN_TEST_POS.txt` thêm lượt test có thành viên (Lan lần đầu OTP → VERIFIED; Hùng bị khóa; đăng ký 0987654321); cập nhật ghi chú B7 trong `tasks/B06-pos-ban-hang.md`.

## Kiểm tra
- `npm run build` không lỗi.
- Trên trình duyệt (quầy HM001-Q01, `tn.hm001.1`):
  - Gắn Lan, đổi 200 điểm (OTP lấy từ panel giả lập), thanh toán tiền mặt cộng điểm; hóa đơn hiện điểm; Customer 360 / DB có ledger HOLD, RELEASE, REDEEM, EARN.
  - Hùng: báo "tạm khóa".
  - Đăng ký 0987654321 rồi bán: được tích điểm, có SMS chào mừng.
- SQL: số dư bằng tổng sổ cái cho mọi khách (kết quả 0 dòng lệch).
