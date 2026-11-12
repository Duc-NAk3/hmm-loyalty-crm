# Danh sách công việc – bản demo HMM Loyalty CRM

Mỗi bước là một file `Bxx-*.md`. Trạng thái ghi ở dòng đầu mỗi file và trong bảng dưới.
✅ = xong và đã commit trên `Dev` · 🔄 = đang làm · ⬜ = chưa làm.

**Quy trình:**
- Làm trên nhánh `Dev`; mỗi bước nhỏ là một commit, push `origin Dev`.
- Chỉ merge vào `main` khi người dùng duyệt.
- Xong bước nào thì đổi trạng thái trong file của bước đó và trong bảng này, ghi mã commit, rồi commit cùng code.

| Bước | Nội dung | Trạng thái | Commit |
|---|---|---|---|
| [B1](B01-khung-backend.md) | Khung kiến trúc backend | ✅ (đã lên `main`) | `f5ed4e7` |
| [B2](B02-du-lieu-mau.md) | Dữ liệu mẫu DemoDataSeeder | ✅ | `fb8189c` |
| [B3](B03-test-logic-diem.md) | Test logic điểm, sổ cái, Modulith | ✅ | `52baf9f` |
| [B4](B04-csv-mau.md) | CSV mẫu 3 POS cũ + nhập mặt hàng | ✅ | `9707298` |
| [B5](B05-khung-frontend.md) | Khung frontend, đăng nhập, menu theo vai trò | ✅ | `de2019a` |
| [B6](B06-pos-ban-hang.md) | POS: ca, bán hàng, thanh toán tiền mặt | ✅ | `70a5d39`, `eeedf59` |
| [B6.B](B06B-orm-design-pattern.md) | Chuyển sang ORM (JPA) + design pattern | ✅ | `76f5744`, `182f93f`, `3bbdf39`, `cb0d614` |
| [B7](B07-pos-thanh-vien-doi-diem.md) | POS: thành viên, đăng ký, đổi điểm OTP | ✅ | `3035ae3` |
| [B8](B08-portal-dashboard-360.md) | Portal: Dashboard KPI, khách hàng, Customer 360 | ✅ | `1853564` |
| [B9](B09-portal-quan-tri.md) | Portal: mặt hàng & giá, chuyển đổi dữ liệu, người dùng, hộp thư SMS, xét hạng | ✅ | `e4a366c` |
| [B10](B10-readme-kich-ban-demo.md) | README đầy đủ + kịch bản demo ~10 phút | ⬜ **làm tiếp theo** | |
| [B11](B11-docker.md) | Docker compose + hướng dẫn Docker | ⬜ | |
| [Sau demo](B12-sau-demo.md) | Merge `main`, tài liệu Word còn lại (test case, rủi ro, thuyết trình) | ⬜ | |

Các commit lẻ khác: `c32eabc` thêm CLAUDE.md; `bc902a7` tách Repository ra package `repository/`; `bd796e8` sửa lỗi đăng ký thành viên thiếu `marketingConsent`.

## Tiếp tục công việc (sau khi compact hoặc mở phiên mới)
1. Đọc `../CLAUDE.md` (lệnh, kiến trúc, quy ước) và file này.
2. Mở file của bước ⬜ đầu tiên: trong đó có phạm vi, API backend đã có sẵn, file cần sửa và cách kiểm tra.
3. Kiểm tra trạng thái repo: `git status`, `git log --oneline -5`, đang ở nhánh `Dev`.
4. Chạy server để thử: dùng cấu hình preview `backend` và `frontend` trong `D:\Job\SofiTech\.claude\launch.json` (công cụ preview_start), hoặc chạy lệnh tay theo CLAUDE.md.

**Nguồn thông tin khác:**
| Cần gì | Ở đâu |
|---|---|
| Kế hoạch tổng thể đã duyệt | `C:\Users\DUC\.claude\plans\scalable-foraging-garden.md` |
| Yêu cầu FR (19 FR có cột Demo) | `../../01_BA_BRD_SRS/SFT-AB-RTL-CRM-2026-001-SRS-NguyenAnhDuc.docx` |
| Mô hình dữ liệu | `../../02_Architecture_Design/...ERD_Database_Design...docx` và `backend/src/main/resources/db/migration/` |
| Kiến trúc | `../../02_Architecture_Design/...Architecture_Proposal...docx` |
| Hướng dẫn test POS (3 thu ngân, 20 mặt hàng, 10 lượt) | `../docs/HUONG_DAN_TEST_POS.txt` |
| Dữ liệu CSV mẫu | `../sample-data/README.md` |
| Phạm vi và quy ước toàn bộ bài | `../../CLAUDE.md` (gốc thư mục bài) |
