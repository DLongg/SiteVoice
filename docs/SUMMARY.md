# SiteVoice — Active Context

Cập nhật: 2026-11-14. Đây là trạng thái hiện tại; quy tắc ở [AGENTS.md](../AGENTS.md), lịch sử ở [WORK_LOG.md](WORK_LOG.md).

## Tổng quan và trạng thái đã xác minh

- Mục tiêu: ứng dụng Android ghi âm ngoài công trường → STT (kèm hiệu đính ngữ cảnh xây dựng) → phân loại → chuẩn bị cuối ngày → LLM tổng hợp theo Mẫu → duyệt bản xem trước một lần → Google Doc tối đa 2 trang.
- Đã hoàn thành 100% Giai đoạn 0–3, gói Thiết lập Biểu mẫu 2 bước (HITL), gói nâng cấp **R1–R5** và **Quyết định 0008 (Một Bản Mẫu Tự Động Duy Nhất từ Google Doc kết hợp Duyệt 2 Bước)**.
- Lệnh `.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon` chạy pass 100% toàn bộ unit tests & adversarial tests, đóng gói thành công `app\build\outputs\apk\debug\app-debug.apk`.

## Kiến trúc và công nghệ dự kiến

- Kotlin + Jetpack Compose, Room, WorkManager, EncryptedSharedPreferences, OkHttp; các module: `ui`, `audio`, `stt`, `llm`, `template`, `export`, `data`, `queue`, `settings`, `formsetup`. Xem [kiến trúc](03-kien-truc.md), [dữ liệu](04-du-lieu.md).
- Giao diện UI: Chuẩn hóa theo skill `design-taste-frontend` (`.agents/skills/design-taste-frontend/SKILL.md`), hiển thị rõ tọa độ neo Google Doc và huy hiệu `{{MA}}` xuyên suốt.
- STT qua Groq Whisper + hiệu đính ngữ cảnh LLM (`NoteClassifier.correctedText`); LLM tổng hợp: Gemini 3.8 Flash (qua Vilao AI), fallback Groq. Xuất Google Doc: Google Drive API v3 copy file và Google Docs API v1 batchUpdate thay placeholder kèm cơ chế quét sạch 2 lớp.

## Đang triển khai và bàn giao

- **Hoàn thành Quyết định 0008 & Đồng bộ Mẫu Tự Động Duy Nhất (Phương án A)**:
  - **Một Bản Mẫu Tự Động Duy Nhất (`SINGLE_AUTO_TEMPLATE_ID = "auto_single_template"`)**: `MainActivity` gọi `purgeBuiltInAndEnsureSingleActive()` dọn sạch 2 mẫu mặc định cũ và chỉ giữ duy nhất 1 bản mẫu tự động đang kích hoạt; mở thẳng `TemplateEditorScreen` cho bản mẫu duy nhất này.
  - **Duyệt 2 Bước (`FormSetupDialog`) trước khi ghi Google Doc**: Khi dán link/ID Google Doc, ứng dụng tạo bản sao làm việc trên Google Drive, quét chỗ điền bằng code (`FormCandidateScanner`), gọi AI đề xuất thẻ `{{MA}}` & System Prompt rồi mở hộp thoại **Thiết lập Biểu mẫu 2 Bước** để người dùng duyệt trước khi ghi placeholder vào bản sao Google Doc và lưu `saveSingleAutoTemplate`.
  - **Đồng bộ ID xuyên suốt (`TemplateItemEntity` ↔ `schemaJson` ↔ `DailyReportEntity` ↔ `ExportWorker`)**: `FormSetupService` và `DefaultTemplateRepository.normalizeSchemaId()` chuẩn hóa `"id"` trong `schemaJson` về `"auto_single_template"`; `DailyReportEntity` và `MainViewModel.synthesizeDailyReport` lưu đúng `SINGLE_AUTO_TEMPLATE_ID` để `ExportWorker` luôn tra cứu chính xác bản mẫu trong Room.
- **Ưu tiên tiếp theo**: Đã cài đặt `app\build\outputs\apk\debug\app-debug.apk` (`Success`) và mở ứng dụng trực tiếp trên điện thoại thật `33c4f658` (`model:PKS110`); sẵn sàng cho người dùng kiểm thử thực tế luồng tạo/duyệt mẫu tự động duy nhất.

## Quyết định phải giữ

- [0001](quyet-dinh/0001-stt-lai-online-offline.md): lưu âm thanh trước; [0002](quyet-dinh/0002-mot-nut-ghi-am.md): một nút ghi âm, duyệt cuối ngày.
- [0003](quyet-dinh/0003-xuat-doc-bang-mau.md): xuất bằng bản sao Doc mẫu; [0004](quyet-dinh/0004-kotlin-compose-chi-android.md): Kotlin/Compose, chỉ Android; [0005](quyet-dinh/0005-kien-truc-theo-mau.md): lõi theo Mẫu, không viết cứng nghiệp vụ công trường.
- [0006](quyet-dinh/0006-mot-luot-duyet.md): chỉ một lượt duyệt ở bản xem trước cuối cùng; không duyệt Note riêng; điện thoại giữ nguyên âm thanh gốc và dữ liệu.
- [0007](quyet-dinh/0007-bo-stt-offline-cho-mang.md): bỏ STT offline on-device, lưu speech ngoại tuyến và chờ mạng gọi API online.
- Thay đổi 01 thêm [0006-tu-dat-placeholder](quyet-dinh/0006-tu-dat-placeholder.md) và [0007-dau-vao-gdoc-va-docx](quyet-dinh/0007-dau-vao-gdoc-va-docx.md); giữ tên file theo yêu cầu, không thay hai quyết định cũ trùng số.
- [0008](quyet-dinh/0008-mot-ban-mau-tu-dong-duy-nhat.md): bỏ 2 mẫu mặc định trong danh sách, duy trì một bản mẫu tự động duy nhất từ Google Doc kết hợp duyệt 2 bước (`FormSetupDialog`).
- Luôn commit và push chi tiết từng bước lên GitHub `origin/main`.
- Quy tắc làm việc: Nếu Chrome/hệ thống yêu cầu người dùng thì agent cũng bị yêu cầu (hỏi và chờ xác nhận); nếu prompt của người dùng đi chệch hướng với luồng/luật/định nghĩa trong tài liệu (Doc) thì agent phải đưa ra câu hỏi lựa chọn để người dùng chốt trước khi làm.
