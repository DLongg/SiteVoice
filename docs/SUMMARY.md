# SiteVoice — Active Context

Cập nhật: 2026-10-20. Đây là trạng thái hiện tại; quy tắc ở [AGENTS.md](../AGENTS.md), lịch sử ở [WORK_LOG.md](WORK_LOG.md).

## Tổng quan và trạng thái đã xác minh

- Mục tiêu: ứng dụng Android ghi âm ngoài công trường → STT → phân loại → chuẩn bị cuối ngày → LLM tổng hợp theo Mẫu → duyệt bản xem trước một lần → Google Doc tối đa 2 trang.
- Đã hoàn thành 100% Giai đoạn 0, Giai đoạn 1 và Giai đoạn 2 (Task 2.1, 2.2, 2.3.1): Ghi âm M4A an toàn, STT Groq Whisper, phân loại tự động LLM, danh sách ghi chú Room, dữ liệu cố định cuối ngày (kế thừa hôm trước), LLM tổng hợp báo cáo kiểm soát ngân sách dòng (ReportSynthesizer, BudgetValidator), màn hình xem trước và nút duyệt 1 lần duy nhất (DailyReportPreviewScreen) đã chạy mượt trên điện thoại thật `33c4f658`.
- Lệnh `.\gradlew.bat testDebugUnitTest assembleDebug` build thành công 100% (21/21 unit tests pass, đã cài đặt và khởi chạy APK trên máy thật).

## Kiến trúc và công nghệ dự kiến

- Kotlin + Jetpack Compose, Room, WorkManager, EncryptedSharedPreferences; các module dự kiến: `ui`, `audio`, `stt`, `llm`, `template`, `export`, `data`, `queue`, `settings`. Xem [kiến trúc](03-kien-truc.md), [dữ liệu](04-du-lieu.md).
- Giao diện UI: Chuẩn hóa theo skill `design-taste-frontend` (`.agents/skills/design-taste-frontend/SKILL.md`), tuân thủ quy tắc App Screens (tinh gọn, tương phản cao ngoài công trường, không gradient/shadow thừa).
- STT qua Groq Whisper; LLM Groq `qwen/qwen3.8-27b` ổn định. Mẫu đóng gói vào assets; Drive API copy Doc mẫu, Docs API thay placeholder.

## Đang triển khai và bàn giao

- [Lộ trình](07-lo-trinh.md) đang ở Giai đoạn 2 (Task 2.3: Xuất Google Doc).
- Đã hoàn thành Task 2.3.1: Màn hình xem trước `DailyReportPreviewScreen`, hiển thị đầy đủ thông tin hiện trường, 4 khối tổng hợp, cảnh báo riêng ghi chú chưa phân loại; nút duyệt 1 lần duy nhất chuyển sang `APPROVED` (quyết định 0006).
- Chưa triển khai: Task 2.3.2 (Google Sign-In) → 2.3.3 (Cài đặt link Doc mẫu) → 2.3.4 (Copy Doc mẫu bằng Drive API) → 2.3.5 (Thay placeholder bằng Docs API) → 2.3.6 (Kiểm tra & lưu link).

## Quyết định phải giữ

- [0001](quyet-dinh/0001-stt-lai-online-offline.md): lưu âm thanh trước, STT online/offline; [0002](quyet-dinh/0002-mot-nut-ghi-am.md): một nút ghi âm, duyệt cuối ngày.
- [0003](quyet-dinh/0003-xuat-doc-bang-mau.md): xuất bằng bản sao Doc mẫu; [0004](quyet-dinh/0004-kotlin-compose-chi-android.md): Kotlin/Compose, chỉ Android; [0005](quyet-dinh/0005-kien-truc-theo-mau.md): lõi theo Mẫu, không viết cứng nghiệp vụ công trường.
- [0006](quyet-dinh/0006-mot-luot-duyet.md): chỉ một lượt duyệt ở bản xem trước cuối cùng; không duyệt Note riêng; điện thoại giữ nguyên âm thanh gốc và dữ liệu.
- Luôn commit và push chi tiết từng bước lên GitHub `origin/main`.

## Ưu tiên tiếp theo

1. Triển khai Task 2.3.2 & 2.3.3: Tích hợp xác thực Google Sign-In (xin quyền drive.file, documents) & Cài đặt cấu hình Doc mẫu.
2. Triển khai Task 2.3.4 & 2.3.5: Module `export/` copy Doc mẫu bằng Drive API và thay thế placeholder bằng Docs API batchUpdate.
3. Triển khai Giai đoạn 3: Hàng đợi WorkManager xử lý offline và retry khi có mạng.
