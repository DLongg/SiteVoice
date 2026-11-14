# SiteVoice — Active Context

Cập nhật: 2026-11-15. Đây là trạng thái hiện tại; quy tắc ở [AGENTS.md](../AGENTS.md), lịch sử ở [WORK_LOG.md](WORK_LOG.md).

## Tổng quan và trạng thái đã xác minh

- Mục tiêu: ứng dụng Android ghi âm ngoài công trường → STT (kèm hiệu đính ngữ cảnh xây dựng) → phân loại → chuẩn bị cuối ngày → LLM tổng hợp theo Mẫu → duyệt bản xem trước một lần → Google Doc tối đa 2 trang.
- Đã hoàn thành 100% Giai đoạn 0–3, Quyết định 0008, và Milestone R3: Tách toàn bộ chuỗi tiếng Việt hardcoded trong 11 file UI và `MainViewModel.kt` vào `strings.xml`.
- Lệnh `.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon` chạy pass 100% (223/223 unit tests), đóng gói thành công `app\build\outputs\apk\debug\app-debug.apk`.

## Kiến trúc và công nghệ dự kiến

- Kotlin + Jetpack Compose, Room, WorkManager, EncryptedSharedPreferences, OkHttp; các module: `ui`, `audio`, `stt`, `llm`, `template`, `export`, `data`, `queue`, `settings`, `formsetup`. Xem [kiến trúc](03-kien-truc.md), [dữ liệu](04-du-lieu.md).
- Giao diện UI: Chuẩn hóa theo skill `design-taste-frontend` (`.agents/skills/design-taste-frontend/SKILL.md`) và [design-system/sitevoice/MASTER.md](../design-system/sitevoice/MASTER.md). Toàn bộ chuỗi hiển thị đã được tách vào `strings.xml`.
- STT qua Groq Whisper + hiệu đính ngữ cảnh LLM (`NoteClassifier.correctedText`); LLM tổng hợp: Gemini 3.8 Flash (qua Vilao AI), fallback Groq. Xuất Google Doc: Google Drive API v3 copy file và Google Docs API v1 batchUpdate thay placeholder kèm cơ chế quét sạch 2 lớp.

## Đang triển khai và bàn giao

- **Hoàn thành Milestone R3 (Tách chuỗi tiếng Việt vào strings.xml)**:
  - Bổ sung ~130 khóa chuỗi vào `app/src/main/res/values/strings.xml`.
  - Thay thế toàn bộ literal tiếng Việt trong 10 file UI (`MainScreen`, `RecordScreen`, `NotesListScreen`, `DailyInputScreen`, `DailyReportPreviewScreen`, `SettingsDialog`, `TemplateLibraryScreen`, `TemplateEditorScreen`, `FormSetupDialog`) và `MainViewModel.kt` bằng `stringResource` / `appContext.getString`.
  - Bảo vệ nghiêm ngặt 5 guardrails (giữ nguyên System Prompt LLM, thẻ neo `{{MA}}`, JSON keys, Room status enums).
  - 11 commits riêng biệt đã được push trực tiếp lên `origin/main` (`58b6ba5` -> `40ce73b`).
- **Ưu tiên tiếp theo (Milestone R4)**: Bàn giao cho QA chạy smoke test và xác minh trên thiết bị thật `33c4f658`.

## Quyết định phải giữ

- [0001](quyet-dinh/0001-stt-lai-online-offline.md): lưu âm thanh trước; [0002](quyet-dinh/0002-mot-nut-ghi-am.md): một nút ghi âm, duyệt cuối ngày.
- [0003](quyet-dinh/0003-xuat-doc-bang-mau.md): xuất bằng bản sao Doc mẫu; [0004](quyet-dinh/0004-kotlin-compose-chi-android.md): Kotlin/Compose, chỉ Android; [0005](quyet-dinh/0005-kien-truc-theo-mau.md): lõi theo Mẫu, không viết cứng nghiệp vụ công trường.
- [0006](quyet-dinh/0006-mot-luot-duyet.md): chỉ một lượt duyệt ở bản xem trước cuối cùng; không duyệt Note riêng; điện thoại giữ nguyên âm thanh gốc và dữ liệu.
- [0007](quyet-dinh/0007-bo-stt-offline-cho-mang.md): bỏ STT offline on-device, lưu speech ngoại tuyến và chờ mạng gọi API online.
- [0008](quyet-dinh/0008-mot-ban-mau-tu-dong-duy-nhat.md): duy trì một bản mẫu tự động duy nhất từ Google Doc kết hợp duyệt 2 bước (`FormSetupDialog`).
- Luôn commit và push chi tiết từng bước lên GitHub `origin/main`.
