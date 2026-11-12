# B6 – POS: ca và bán hàng · ✅ Xong (commit `70a5d39`; tài liệu test `eeedf59`)

**File trong `frontend/src/pos/`:**
| File | Vai trò |
|---|---|
| `PosShell.tsx` | Khung màn thu ngân; tải ca hiện tại (API trả 204 khi quầy chưa có ca) |
| `OpenShift.tsx`, `CloseShiftModal.tsx` | Mở ca, đóng ca |
| `SaleScreen.tsx` | Giỏ hàng; phím tắt F2 (ô quét), F9 (thanh toán); sinh `receiptNo` |
| `ProductSearch.tsx` | Ô quét mã: Enter = quét/tìm, phím mũi tên = chọn gợi ý |
| `PaymentModal.tsx` | Thanh toán tiền mặt; đã có tham số `pointsAmount` cho B7 |
| `ReceiptView.tsx` | Xem trước hóa đơn; đã hiện điểm và hạng nếu có thành viên |
| `types.ts` | Kiểu dữ liệu (camelCase) |

**Tài liệu test:** `docs/HUONG_DAN_TEST_POS.txt` gồm 3 thu ngân, 20 mặt hàng, 10 lượt thanh toán có tổng tiền tính sẵn, giải thích màn hình, thay đổi CSDL và luồng code.

**Đang để chỗ chờ:** khung "Thành viên" trong `SaleScreen.tsx` hiện ghi chú "sẽ có ở bước B7".
