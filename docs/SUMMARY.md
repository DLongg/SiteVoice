# SiteVoice — Active Context

Cập nhật: 2026-10-25. Đây là trạng thái hiện tại; quy tắc ở [AGENTS.md](../AGENTS.md), lịch sử ở [WORK_LOG.md](WORK_LOG.md).

## Tổng quan và trạng thái đã xác minh

- Mục tiêu: ứng dụng Android ghi âm ngoài công trường → STT (kèm hiệu đính ngữ cảnh xây dựng) → phân loại → chuẩn bị cuối ngày → LLM tổng hợp theo Mẫu → duyệt bản xem trước một lần → Google Doc tối đa 2 trang.
- Đã hoàn thành 100% Giai đoạn 0–3, gói Thiết lập Biểu mẫu 2 bước (HITL) và toàn bộ gói nâng cấp **R1–R5 (Sửa lệch trường xuất Doc, Bỏ trường cứng khi tạo Mẫu, Xem/Sửa vị trí neo Google Doc, Render Prompt từ trường tổng hợp, Hiệu đính STT ngữ cảnh & Chuẩn hóa UI 9 màn hình)**.
- Lệnh `.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon` build thành công 100% (toàn bộ unit tests và adversarial tests M1–M5 pass, tạo `app-debug.apk` hoàn chỉnh).

## Kiến trúc và công nghệ dự kiến

- Kotlin + Jetpack Compose, Room, WorkManager, EncryptedSharedPreferences, OkHttp; các module: `ui`, `audio`, `stt`, `llm`, `template`, `export`, `data`, `queue`, `settings`, `formsetup`. Xem [kiến trúc](03-kien-truc.md), [dữ liệu](04-du-lieu.md).
- Giao diện UI: Chuẩn hóa 9 màn hình theo skill `design-taste-frontend` (`.agents/skills/design-taste-frontend/SKILL.md`), hiển thị rõ tọa độ neo Google Doc và huy hiệu `{{MA}}` xuyên suốt.
- STT qua Groq Whisper + hiệu đính ngữ cảnh LLM (`NoteClassifier.correctedText`); LLM tổng hợp: Gemini 3.8 Flash (qua Vilao AI), fallback Groq. Xuất Google Doc: Google Drive API v3 copy file và Google Docs API v1 batchUpdate thay placeholder kèm cơ chế quét sạch 2 lớp.

## Đang triển khai và bàn giao

- **Hoàn thành trọn gói nâng cấp R1–R5 (Commit `1d06e1d`)**:
  - **R1 (Khắc phục lệch trường & xuất Doc)**: `FormCandidateScanner` chặn lan truyền nhầm đề mục nội dòng, truyền nhãn toàn bộ cột bảng và quét thẻ `{{MA}}` gõ tay (`the_ma`); `FormLlmDescriber` ưu tiên `Y_KIEN_*` và `MAY_MOC` trước `"thi công"` và bóc sạch markdown JSON fence; `PlaceholderMapper` khớp chính xác `{{MAY_MOC}}` và `{{Y_KIEN_*}}`.
  - **R2 (Bỏ trường cứng & Quét AI khi tạo Mẫu)**: `TemplateEditorScreen` khởi tạo danh sách trường rỗng khi tạo mới, tích hợp nút **"✨ Tạo bản sao & Nhận diện trường bằng AI"** ngay tại `TemplateLibraryScreen` và `TemplateEditorScreen`.
  - **R3 (Xem & Sửa vị trí neo Google Doc)**: `TemplateSchema` & `FormSetupService` lưu/đọc `startIndex`, `endIndex`, `tieuDeGanNhat`, `truoc`, `sau`, `tinHieu`; `FormSetupDialog` & `TemplateEditorScreen` cho phép sửa vị trí neo trực tiếp trên app hoặc mở Google Doc đặt `{{MA}}` bằng tay rồi bấm **"🔄 Đồng bộ lại vị trí & trường từ Doc"**.
  - **R4 (Render Prompt từ các trường tổng hợp)**: Nút **"🔄 Render Prompt từ các trường tổng hợp"** tại Phần 3 của `TemplateEditorScreen` tự sinh lại Prompt thống nhất từ danh sách trường ở Phần 2.
  - **R5 (Hiệu đính STT theo ngữ cảnh & Huy hiệu `{{MA}}`)**: `TextNormalizer`, `NoteClassifier` (`van_ban_da_sua`) và `ClassifyWorker` tự động sửa lỗi STT nghe nhầm thuật ngữ xây dựng mà vẫn giữ nguyên 100% số liệu/tầng/trục; `DailyInputScreen` & `DailyReportPreviewScreen` gắn huy hiệu `{{MA}}` rõ ràng.
- **Ưu tiên tiếp theo**: Đã cài đặt `app\build\outputs\apk\debug\app-debug.apk` (`Success`) và mở ứng dụng trực tiếp trên điện thoại thật `33c4f658` (`model:PKS110`); sẵn sàng cho người dùng kiểm thử thực tế trên thiết bị.

## Quyết định phải giữ

- [0001](quyet-dinh/0001-stt-lai-online-offline.md): lưu âm thanh trước; [0002](quyet-dinh/0002-mot-nut-ghi-am.md): một nút ghi âm, duyệt cuối ngày.
- [0003](quyet-dinh/0003-xuat-doc-bang-mau.md): xuất bằng bản sao Doc mẫu; [0004](quyet-dinh/0004-kotlin-compose-chi-android.md): Kotlin/Compose, chỉ Android; [0005](quyet-dinh/0005-kien-truc-theo-mau.md): lõi theo Mẫu, không viết cứng nghiệp vụ công trường.
- [0006](quyet-dinh/0006-mot-luot-duyet.md): chỉ một lượt duyệt ở bản xem trước cuối cùng; không duyệt Note riêng; điện thoại giữ nguyên âm thanh gốc và dữ liệu.
- [0007](quyet-dinh/0007-bo-stt-offline-cho-mang.md): bỏ STT offline on-device, lưu speech ngoại tuyến và chờ mạng gọi API online.
- Thay đổi 01 thêm [0006-tu-dat-placeholder](quyet-dinh/0006-tu-dat-placeholder.md) và [0007-dau-vao-gdoc-va-docx](quyet-dinh/0007-dau-vao-gdoc-va-docx.md); giữ tên file theo yêu cầu, không thay hai quyết định cũ trùng số.
- Luôn commit và push chi tiết từng bước lên GitHub `origin/main`.
- Quy tắc làm việc: Nếu Chrome/hệ thống yêu cầu người dùng thì agent cũng bị yêu cầu (hỏi và chờ xác nhận); nếu prompt của người dùng đi chệch hướng với luồng/luật/định nghĩa trong tài liệu (Doc) thì agent phải đưa ra câu hỏi lựa chọn để người dùng chốt trước khi làm.
