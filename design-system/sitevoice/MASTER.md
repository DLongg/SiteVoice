# SiteVoice — Design System (MASTER)

Cập nhật: 2026-11-14. Nguồn chính cho token giao diện (màu, chữ, khoảng cách, bo góc, trạng thái) của app Android Compose.

- Nguyên tắc thẩm mỹ bắt buộc vẫn là skill `design-taste-frontend` và mục *Thiết kế giao diện* trong [docs/03-kien-truc.md](../../docs/03-kien-truc.md). File này chỉ cụ thể hóa thành token; khi mâu thuẫn, hai nguồn kia thắng.
- Đối chiếu dữ liệu skill `ui-ux-pro-max` (style *Minimalism & Swiss*, stack `jetpack-compose`: "Material3 tokens, không hardcode màu/sp"). Phần landing page/GSAP/CSS của kết quả skill không áp dụng cho app.
- Phạm vi đã chốt với người dùng: **giữ bảng màu slate + xanh hiện có**, **font hệ thống (Roboto)**, **chỉ giao diện sáng** (dark mode để sau).
- Trạng thái (2026-11-14): **đã áp dụng toàn bộ `ui/`**. `ui/theme/` (`SiteVoiceTheme`, `TextSizes`, `Spacing`) bọc toàn app; cả 11 file màn hình không còn `Color(0x…)`, `x.sp`, `RoundedCornerShape(x.dp)` viết cứng.
- Tồn đọng đã biết (chưa xử lý):
  - Text chỉ đặt `fontSize` (không đặt `style`/`lineHeight`) kế thừa dòng 24sp của `bodyLarge` nên chữ 12–14sp giãn rộng. Không dùng `lineHeight` dạng `em` cho `bodyLarge`: làm crash `OutlinedTextField` (nội suy nhãn em↔sp). Cách sửa: thêm `lineHeight = TextSizes.*Line` tại call site hoặc chuyển sang `style`.
  - Hai nút hành động chính "Tổng hợp nhật ký thi công" và "Đồng bộ lại vị trí" giữ nền mực tối (`onSurface`) từ thiết kế cũ, chưa theo quy tắc nút chính = `primary`.
  - Emoji còn dùng làm icon (thanh điều hướng, nút) và chuỗi viết cứng ngoài `strings.xml`.
- Trang có quy tắc riêng đặt ở `design-system/sitevoice/pages/<ten-man-hinh>.md` và ghi đè file này cho màn hình đó.

## 1. Màu (Material3 `lightColorScheme`)

Tỉ lệ tương phản đo theo WCAG 2.x (script tính độ sáng tương đối, 2026-11-14).

| Token M3 | Hex | Dùng cho | Tương phản |
|---|---|---|---|
| `primary` | `#2563EB` | Nút chính, tab đang chọn, nút ghi âm, liên kết | chữ trắng 5.17:1 |
| `onPrimary` | `#FFFFFF` | Chữ/icon trên primary | — |
| `primaryContainer` | `#EFF6FF` | Nền chip/huy hiệu `{{MA}}`, dòng được chọn | — |
| `onPrimaryContainer` | `#1E40AF` | Chữ trên primaryContainer | 8.01:1 |
| `inversePrimary` / trạng thái nhấn | `#1D4ED8` | Primary khi nhấn | chữ trắng 6.70:1 |
| `background` | `#F8FAFC` | Nền màn hình | — |
| `onBackground` | `#0F172A` | Chữ chính | 17.06:1 |
| `surface` | `#FFFFFF` | Card, dialog, sheet | — |
| `onSurface` | `#0F172A` | Chữ chính trên surface | 17.85:1 |
| `surfaceVariant` / `surfaceContainer` | `#F1F5F9` | Bước tông (tone step) phân vùng | — |
| `onSurfaceVariant` | `#475569` | Chữ phụ, nhãn, mô tả | 6.92:1 trên `#F1F5F9`, 7.58:1 trên trắng |
| `outline` | `#64748B` | Viền ô nhập, viền thành phần tương tác | 4.76:1 (≥3:1) |
| `outlineVariant` | `#E2E8F0` | Đường chia trang trí (không mang thông tin) | trang trí |
| `error` | `#DC2626` | Lỗi, nút xóa | chữ trắng 4.83:1 |
| `onError` | `#FFFFFF` | — | — |
| `errorContainer` | `#FEF2F2` | Nền thông báo lỗi | — |
| `onErrorContainer` | `#B91C1C` | Chữ trên errorContainer | 5.91:1 |

Màu ngữ nghĩa bổ sung (M3 không có sẵn; khai báo trong `SiteVoiceColors` riêng qua `CompositionLocal`):

| Token | Hex | Container / chữ trên container | Dùng cho |
|---|---|---|---|
| `success` | `#15803D` (trắng 5.02:1) | `#DCFCE7` / `#166534` (6.49:1) | Đã xuất Doc, đã đồng bộ |
| `warning` | `#B45309` (trên trắng 5.02:1) | `#FEF3C7` / `#92400E` (6.37:1) | Chờ mạng, đang retry, chưa phân loại |

Quy tắc màu:
- Không gradient, không glow, không bóng đổ trang trí (theo `design-taste-frontend`). Phân vùng bằng bước tông `background` → `surface` → `surfaceVariant`.
- Màu trạng thái luôn đi kèm icon hoặc chữ, không dùng màu đơn thuần để truyền nghĩa.
- **Cấm dùng cho chữ:** `#94A3B8`, `#CBD5E1` (tương phản 2.56:1 và 1.48:1). Chỉ dùng cho trạng thái disabled cùng ngữ nghĩa disabled.
- **Không dùng `#64748B` cho chữ trên `#F1F5F9`** (4.34:1 < 4.5:1). Chữ phụ dùng `onSurfaceVariant` `#475569`.
- Màu ngoài bảng đang tồn tại trong code (`#4F46E5`, `#0369A1`, `#0284C7`, `#111827`, `#6B7280`, `#4B5563`, `#3B82F6`, `#16A34A`, `#D97706`…) gộp về token gần nhất khi áp dụng.

## 2. Kiểu chữ

Font hệ thống (`FontFamily.Default`, Roboto). Hiển thị đủ dấu tiếng Việt, không thêm file font.

5 cấp cỡ (đúng giới hạn 3–5 cấp trong `docs/03-kien-truc.md`):

| Cấp | Token M3 | Cỡ / dòng | Độ đậm | Dùng cho |
|---|---|---|---|---|
| Display | `displaySmall` | 28sp / 36sp | 600 | Đồng hồ ghi âm, số liệu nổi bật duy nhất của màn hình |
| Title | `titleLarge` | 20sp / 28sp | 600 | Tiêu đề màn hình, tiêu đề dialog |
| Subtitle | `titleMedium` / `bodyLarge` | 16sp / 24sp | 600 / 400 | Tiêu đề card, nội dung ghi chú, ô nhập |
| Body | `bodyMedium` / `labelLarge` | 14sp / 20sp | 400 / 500 | Văn bản thường, nhãn nút |
| Caption | `bodySmall` / `labelMedium` | 12sp / 16sp | 400 / 500 | Thời gian, metadata, huy hiệu `{{MA}}`, tọa độ neo |

Quy tắc chữ:
- **Cỡ tối thiểu 12sp.** Hiện code dùng 10sp (24 chỗ) và 11sp (71 chỗ); khi áp dụng nâng lên Caption 12sp.
- Không viết cứng `fontSize = x.sp` trong màn hình; dùng `MaterialTheme.typography.*`. Để cỡ chữ theo `sp` cho phép người dùng phóng to hệ thống; bố cục phải chịu được cỡ chữ 200% mà không cắt chữ quan trọng.
- Chữ chính dùng màu mực đầy đủ `#0F172A`; không dùng xám nhạt cho nội dung cần đọc.
- Số đo thời gian (đồng hồ ghi âm, thời lượng) dùng `FontFeatureSettings "tnum"` để không nhảy bố cục.

## 3. Khoảng cách, bo góc, kích thước

- **Lưới 4dp.** Thang dùng: `4, 8, 12, 16, 24, 32`. Gộp các giá trị lẻ hiện có (`2, 6, 10, 14, 20, 28, 36`) về bậc gần nhất khi áp dụng.
  - Lề hai bên màn hình: 16dp, đối xứng.
  - Khoảng cách giữa phần tử trong card: 8dp; giữa các card/nhóm: 12dp; giữa các khu vực lớn: 24dp.
- **Bo góc 3 bậc:** `small 6dp` (chip, huy hiệu, ô nhập), `medium 8dp` (nút, card), `large 12dp` (dialog, bottom sheet). Nút ghi âm hình tròn là ngoại lệ.
- **Vùng chạm tối thiểu 48×48dp** (Material), cách nhau ≥8dp. Icon nhỏ hơn phải mở rộng vùng chạm.
- Nút ghi âm nhấn giữ là hành động chính duy nhất trên màn hình Ghi âm, kích thước lớn, đặt trong tầm ngón cái, chừa vùng an toàn của thanh điều hướng hệ thống.
- Elevation: 0dp cho card (phân vùng bằng tông màu); chỉ dialog/sheet dùng elevation mặc định của M3 kèm scrim.

## 4. Trạng thái và phản hồi

Mọi thành phần tương tác phải có đủ trạng thái (`design-taste-frontend` + `ui-ux-pro-max` §2, §8):

| Trạng thái | Cách thể hiện |
|---|---|
| Nhấn (pressed) | Ripple/state layer M3, phản hồi < 100ms; không đổi kích thước bố cục |
| Disabled | Ngữ nghĩa disabled + độ mờ 0.38; không nhận chạm |
| Đang tải | Nút bị khóa + chỉ báo tiến trình trong nút; tác vụ > 1s hiện trạng thái rõ (ví dụ "Đang tổng hợp…") |
| Rỗng | Câu giải thích + một hành động tiếp theo (ví dụ chưa có ghi chú → hướng dẫn nhấn giữ để ghi) |
| Lỗi | Thông báo nêu nguyên nhân + cách khắc phục + nút thử lại; lỗi ô nhập hiện ngay dưới ô |
| Thành công | Phản hồi ngắn (snackbar 3–5s, không cướp focus) |
| Chờ mạng / retry | Màu `warning` + chữ trạng thái của WorkManager; không im lặng |
| Hành động phá hủy | Hộp xác nhận, nút dùng màu `error`, tách khỏi hành động chính |

## 5. Chuyển động

- Tối giản: chỉ chuyển cảnh và đổi trạng thái có ý nghĩa; 150–250ms vào, ra ngắn hơn (~70%).
- Tôn trọng cài đặt giảm chuyển động của hệ thống (animator duration scale = 0 → bỏ animation, hiện trạng thái cuối).
- Không animation trang trí, không chặn thao tác trong lúc animation chạy.

## 6. Trợ năng

- Icon-only button phải có `contentDescription` bằng chuỗi trong `strings.xml`; icon trang trí cạnh chữ dùng `contentDescription = null`.
- Thứ tự focus của TalkBack trùng thứ tự thị giác; trạng thái (đang ghi, đã chọn, mở rộng) được đọc qua `semantics`.
- Huy hiệu `{{MA}}` và tọa độ neo Google Doc phải đọc được đầy đủ; cắt chữ thì có cách xem toàn văn.

## 7. Hướng triển khai trong code (khi được giao task áp dụng)

Chưa thực hiện. Gợi ý cấu trúc, cần người dùng duyệt trước:

- `app/src/main/java/com/sitevoice/app/ui/theme/Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt` (`SiteVoiceTheme` bọc `MaterialTheme` + `LocalSiteVoiceColors` cho `success`/`warning`).
- Bọc nội dung trong `MainActivity` bằng `SiteVoiceTheme`; thay dần `Color(0x…)`, `fontSize = x.sp`, `RoundedCornerShape(x.dp)` theo từng màn hình, mỗi màn hình một commit.
- Kiểm tra: build + test bằng Gradle wrapper, xác minh trên điện thoại thật (ngoài trời nếu có thể), bật cỡ chữ lớn nhất và TalkBack.

## 8. Checklist trước khi giao UI

- [ ] Không còn `Color(0x…)`, `x.sp`, bo góc tự đặt trong file màn hình mới/sửa.
- [ ] Chữ ≥12sp; chữ thường ≥4.5:1; viền tương tác ≥3:1.
- [ ] Vùng chạm ≥48dp, cách nhau ≥8dp.
- [ ] Đủ trạng thái nhấn/disabled/tải/rỗng/lỗi.
- [ ] Không gradient, glow, bóng đổ trang trí, badge thừa.
- [ ] Chuỗi UI nằm trong `strings.xml`.
- [ ] Đã xem trên điện thoại thật với cỡ chữ lớn và TalkBack.
