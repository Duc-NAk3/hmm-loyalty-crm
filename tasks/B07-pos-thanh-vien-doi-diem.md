# B7 – POS: thành viên và đổi điểm · ✅ Xong (commit `__HASH__`)

**FR:** FR-03 (gắn thành viên), FR-18 (tra cứu), FR-19 (đăng ký nhanh), FR-20 (xác thực SĐT bằng OTP lần đầu), FR-30 (đổi điểm), FR-04 (thanh toán tiền mặt và điểm).

Backend không đổi (API đã có từ B1/B6.B); chỉ làm frontend.

## File trong `frontend/src/pos/`
| File | Vai trò |
|---|---|
| `MemberPanel.tsx` | Ô tra cứu SĐT/mã (F4, chọn hết chữ cũ khi focus); thẻ thành viên (hạng × hệ số, điểm ≈ tiền, sắp hết hạn, còn thiếu để lên hạng, nhãn Chưa xác thực / Tạm khóa đổi điểm); nút Đổi điểm; khung "Đang giữ" có đồng hồ đếm ngược và nút hủy. Không tìm thấy SĐT → gọi `onNotFound` mở đăng ký. |
| `RegisterModal.tsx` | Đăng ký nhanh: SĐT điền sẵn, họ tên bắt buộc, giới tính (FEMALE/MALE/OTHER), ngày sinh, consent SMS. |
| `RedeemModal.tsx` | 3 bước: báo giá → chọn điểm (mặc định tối đa, tối thiểu 50) → gửi OTP → `Input.OTP` 6 ô, nhập đủ thì tự xác nhận; sai thì báo lỗi và focus lại; OTP hết hạn / sai quá 3 lần thì quay về bước chọn điểm. Đóng modal hoặc "Đổi số điểm" khi đang chờ OTP thì gọi cancel. |
| `MockPhone.tsx` | Drawer "Điện thoại khách (giả lập)" – công cụ demo, tự làm mới 3 giây, tô đậm mã 6 số. Không có mask để vừa xem vừa nhập OTP. |
| `SaleScreen.tsx` | Giữ state `member`, `hold`; gửi `customerId`, `redemptionId` khi thanh toán; `PaymentModal` nhận `pointsAmount`. |
| `types.ts` | Thêm `MemberSummary`, `RedeemQuote`, `RedemptionRequested`, `RedemptionHeld`, `SmsView`. |

## Quy tắc ở frontend
- **Giỏ hàng đổi sau khi đã giữ điểm** (thêm, xóa dòng, sửa số lượng): tự hủy yêu cầu (backend ghi RELEASE) và báo đổi lại, vì số điểm tối đa phụ thuộc giỏ hàng. So sánh bằng `cartKey` (productId × quantity) lưu lúc giữ điểm.
- **Hủy hóa đơn đang bán** khi đang giữ điểm → trả lại điểm.
- **Đồng hồ giữ điểm về 0** → bỏ phần trả bằng điểm và đọc lại thẻ thành viên (backend tự chuyển EXPIRED trong ≤ 30 giây).
- **Thanh toán báo `REDEEM_EXPIRED`** → bỏ hold, đọc lại thẻ; thu ngân đổi lại hoặc thu tiền mặt.
- Khi đang giữ điểm thì ẩn nút "Bỏ gắn" thành viên. Sau khi hóa đơn hoàn tất, "Hóa đơn mới" bỏ gắn thành viên.
- Tải lại trang khi đang giữ điểm: giỏ hàng mất, điểm giữ được tác vụ nền trả lại sau 15 phút.

## API dùng (đều đã có ở backend)
| API | Mô tả |
|---|---|
| `GET /api/pos/members/lookup?q=` | SĐT (+84/84/0) hoặc mã HM… → `MemberSummary`. Không có thì 404 "Chưa là thành viên". |
| `POST /api/pos/members` | `{ phone, fullName, gender?, dateOfBirth?, marketingConsent }` → `MemberSummary`; SMS chào mừng giả lập. |
| `POST /api/pos/redemptions/quote` | `{ customerId, lines }` → `{ allowed, maxPoints, maxAmount, reason, redeemableAmount }` |
| `POST /api/pos/redemptions` | `{ customerId, terminalId, points, lines }` → `{ redemptionId, points, amountVnd, status: "QUOTED", phoneMasked, otpTtlMinutes }`; gửi OTP giả lập. |
| `POST /api/pos/redemptions/{id}/verify` | `{ terminalId, otp }` → `{ status: "HELD", holdExpiresAt, balanceAfterHold, phoneVerified }`. Sai: 400 `OTP_WRONG`; hết hạn / sai quá số lần: `OTP_EXPIRED` / `OTP_TOO_MANY_ATTEMPTS` và yêu cầu bị hủy. |
| `POST /api/pos/redemptions/{id}/cancel` | `{ terminalId }` → trả lại điểm (RELEASED). |
| `GET /api/pos/mock-phone?customerId=` | 5 SMS mới nhất (`SMS_OTP`, `SMS_WELCOME`, `SMS_TIER`). |
| `POST /api/pos/sales` | Thêm `customerId`, `redemptionId`. |

## Đã kiểm tra (12/11/2026, quầy HM001-Q01, `tn.hm001.1`)
- `npm run build` không lỗi.
- Lan 0901234567: đổi 200 điểm, OTP sai báo lỗi, OTP đúng → giữ điểm, hồ sơ VERIFIED; thanh toán 107.000 tiền mặt + 20.000 điểm; hóa đơn −200 / +20, số dư 5.574. Ledger: HOLD −200, RELEASE +200, 16 dòng REDEEM theo lô FIFO (tổng −200), EARN +20.
- Hùng 0912345678: nhãn và thông báo "tạm khóa đổi điểm".
- 0987654321: đăng ký nhanh → HM10000801, SMS chào mừng hiện trên điện thoại giả lập; bán 75.000 → +7 điểm.
- Tuấn 0935123456: giữ 154 điểm rồi quét thêm hàng → tự trả điểm; giữ lại rồi bấm hủy → trả điểm. Ledger HOLD/RELEASE khớp.
- SQL số dư ≠ tổng sổ cái: 0 khách.

**Lưu ý dữ liệu:** sau khi test, 0987654321 đã là thành viên trong database `hmm_loyalty` local. Trước buổi demo nên tạo lại database (xem `../CLAUDE.md`).

**Tài liệu:** `docs/HUONG_DAN_TEST_POS.txt` mục 4B (TV1–TV4) và danh sách API.
