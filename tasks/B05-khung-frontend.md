# B5 – Khung frontend · ✅ Xong (commit `de2019a`)

**Công nghệ:** Vite 8, React 19, TypeScript 7, Ant Design 6 (vi_VN), react-router 7, Recharts (đã cài, chưa dùng).

**Các file chính trong `frontend/src`:**
| File | Vai trò |
|---|---|
| `App.tsx` | Khai báo route |
| `Home.tsx` | Trang chọn khu vực POS / Portal |
| `shared/api.ts` | Gọi API kèm token theo khu vực; lỗi tiếng Việt |
| `shared/session.ts` | Phiên đăng nhập; quầy của máy lưu ở `hmm.pos.terminal` |
| `shared/format.ts` | Định dạng tiền và ngày; tên vai trò, hạng, trạng thái |
| `pos/TerminalSetup.tsx`, `pos/PosLogin.tsx` | Chọn quầy, đăng nhập POS |
| `portal/PortalLogin.tsx`, `portal/PortalLayout.tsx` | Đăng nhập Portal, khung trang |
| `portal/menu.tsx` | Menu theo vai trò, kèm bước sẽ làm từng mục |
| `portal/Placeholder.tsx` | Trang chờ cho chức năng chưa làm |

**Thêm trang Portal mới:** thay `Placeholder` bằng component thật trong `App.tsx`, giữ `key` của `PORTAL_MENU`.
