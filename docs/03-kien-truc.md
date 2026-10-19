# 03. Kiến trúc

## Module (trong `app/src/main/java/com/sitevoice/app/`)
| Thư mục | Vai trò |
|---|---|
| ui/ | Màn hình Compose: Ghi âm, Danh sách, Duyệt cuối ngày, Xem trước, Cài đặt |
| audio/ | Ghi âm, lưu file, phát lại |
| stt/ | Interface `SpeechToText`; cài đặt: `GroqWhisper`, `OpenAiWhisper`, `WhisperCpp` |
| llm/ | Interface `LlmClient`; phân loại, tổng hợp, rút gọn; đếm ký tự |
| template/ | Đọc Mẫu từ `templates/` (đóng gói vào assets), kiểm tra schema |
| export/ | Google Sign-In, Drive (copy mẫu), Docs (replaceAllText) |
| data/ | Room: entity, DAO, repository |
| queue/ | WorkManager: hàng đợi STT, phân loại, xuất |
| settings/ | EncryptedSharedPreferences |

## Luồng dữ liệu
```
Nhấn giữ → audio/ lưu file → Note: RECORDED
   → SttWorker → stt/ → Note: TRANSCRIBED
   → ClassifyWorker → llm/ → Note: LABELED
Cuối ngày: ui/ duyệt → Note: APPROVED
   → llm/ tổng hợp theo Mẫu → kiểm tra ký tự → DailyReport: DRAFT
   → ui/ xem trước → DailyReport: APPROVED
   → ExportWorker → export/ → DailyReport: EXPORTED (lưu link Doc)
```

## Nguyên tắc
- Mỗi bước lưu trạng thái vào Room; bước sau chỉ đọc kết quả bước trước. Tắt app giữa chừng không mất gì.
- STT và LLM nằm sau interface để đổi nhà cung cấp mà không sửa UI.
- Mẫu nạp từ file cấu hình; code không biết "nhật ký công trường" là gì.
- Bước cần mạng xếp hàng, có mạng tự chạy (ràng buộc `NetworkType.CONNECTED`).

## Thiết kế giao diện (UI Design)
- Mọi màn hình Compose trong module `ui/` (Ghi âm, Danh sách, Duyệt cuối ngày, Xem trước, Cài đặt) bắt buộc tuân thủ nguyên tắc thiết kế từ skill `design-taste-frontend` (`.agents/skills/design-taste-frontend/SKILL.md`), áp dụng quy tắc dành cho App Screens:
  - **Tinh gọn, chống AI-slop**: Không dùng gradient làm nền hoặc nút, không hiệu ứng phát sáng (glow) hay bóng đổ vô cớ, không dùng badge/pill thừa.
  - **Kiểu chữ (Typography)**: Tối đa 2 font, 3–5 cấp kích cỡ, văn bản chính dùng màu mực đầy đủ (độ tương phản cao, dễ nhìn ngoài công trường nắng gắt), không dùng xám nhạt khó đọc.
  - **Bố cục & Khung**: Lưới nhất quán, lề hai bên đối xứng, phân chia bề mặt bằng bước chuyển tông màu nhẹ (tone step) thay vì viền mảnh hairline hoặc bóng mờ.
  - **Trạng thái & Phản hồi**: Mọi thành phần tương tác (nút bấm, ghi âm, card, chip lọc) phải có phản hồi thị giác rõ rệt khi nhấn (press), rỗng (empty), đang tải (loading) và lỗi (error).
