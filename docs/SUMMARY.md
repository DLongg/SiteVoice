# SiteVoice — Active Context

Cập nhật: 2026-10-25. Đây là trạng thái hiện tại; quy tắc ở [AGENTS.md](../AGENTS.md), lịch sử ở [WORK_LOG.md](WORK_LOG.md).

## Tổng quan và trạng thái đã xác minh

- Mục tiêu: ứng dụng Android ghi âm ngoài công trường → STT → phân loại → chuẩn bị cuối ngày → LLM tổng hợp theo Mẫu → duyệt bản xem trước một lần → Google Doc tối đa 2 trang.
- Đã hoàn thành 100% Giai đoạn 0–3 và toàn bộ gói nâng cấp **Milestones M1–M6 (Requirements R1–R5)**: Đọc file Google Doc nhận diện đề mục mở rộng & gộp dòng chấm liên tiếp, AI tự sinh Prompt thống nhất (duyệt Human-in-the-loop 2 bước), thanh trượt Nhiệt độ (`0.0 - 0.8`) và Ghi file Google Doc quét sạch 2 lớp.
- Lệnh `.\gradlew.bat testDebugUnitTest assembleDebug` build thành công 100% (**132/132 unit tests pass**, bao gồm đầy đủ các bộ test đối kháng M2–M5, và tạo `app-debug.apk` hoàn chỉnh).

## Kiến trúc và công nghệ dự kiến

- Kotlin + Jetpack Compose, Room, WorkManager, EncryptedSharedPreferences, OkHttp; các module: `ui`, `audio`, `stt`, `llm`, `template`, `export`, `data`, `queue`, `settings`. Xem [kiến trúc](03-kien-truc.md), [dữ liệu](04-du-lieu.md).
- Giao diện UI: Chuẩn hóa theo skill `design-taste-frontend` (`.agents/skills/design-taste-frontend/SKILL.md`), tuân thủ quy tắc App Screens (tinh gọn, tương phản cao, quy trình HITL 2 bước, thanh trượt nhiệt độ rõ ràng).
- STT qua Groq Whisper; LLM độc lập: Gemini 3.8 Flash (qua Vilao AI), fallback Groq. Xuất Google Doc: Google Drive API v3 copy file và Google Docs API v1 batchUpdate thay placeholder kèm cơ chế quét sạch 2 lớp.

## Đang triển khai và bàn giao

- **Hoàn thành trọn gói R1–R5 (Milestones M1–M6)**:
  - **R1 (Tài liệu)**: Đồng bộ `docs/10-thiet-lap-form.md`, `docs/05-llm.md`, `docs/06-google-doc-mau.md`.
  - **R2 (Đọc file & Sinh Prompt)**: `FormCandidateScanner` nhận diện đề mục La Mã/chữ cái/mục con, truyền nhãn ô bảng trái sang phải, gộp nhiều dòng chấm liên tiếp thành 1 trường `danh_sach`; `FormLlmDescriber` khử trùng lặp mã `ma` và tự sinh Prompt thống nhất; `FormSetupService` & `TemplateSchema` tương thích song ngữ VI/EN không văng `JSONException`.
  - **R3 (Tổng hợp động & Temperature)**: `SecurePreferences` lưu `llmTemperature` (`0.0f..0.8f`, mặc định `0.2f`); `ReportSynthesizer` sinh cấu trúc JSON đích động khớp 1-1 với trường của Mẫu và truyền `temperature` vào `LlmClient`.
  - **R4 (Ghi file Google Doc 2 lớp)**: `PlaceholderMapper` gán mặc định `""` cho toàn bộ trường trong schema; `GoogleDocsExporter` bổ sung Lớp 2 (`documents.get` + regex `\{\{[^{}\r\n]{1,100}\}\}` + `batchUpdate`) xóa sạch 100% mọi tag `{{...}}` chưa có dữ liệu.
  - **R5 (Giao diện HITL 2 bước & Cài đặt)**: `FormSetupDialog` 2 bước (Bước 1 duyệt thẻ neo `{{MA}}` → Bước 2 xem/sửa Prompt tự sinh & chỉnh Nhiệt độ) và thanh trượt Nhiệt độ trong `SettingsDialog`.
- **Ưu tiên tiếp theo**: Kiểm thử thực tế luồng Thiết lập Form 2 bước và xuất Google Doc với biểu mẫu tùy chỉnh trên điện thoại.

## Quyết định phải giữ

- [0001](quyet-dinh/0001-stt-lai-online-offline.md): lưu âm thanh trước; [0002](quyet-dinh/0002-mot-nut-ghi-am.md): một nút ghi âm, duyệt cuối ngày.
- [0003](quyet-dinh/0003-xuat-doc-bang-mau.md): xuất bằng bản sao Doc mẫu; [0004](quyet-dinh/0004-kotlin-compose-chi-android.md): Kotlin/Compose, chỉ Android; [0005](quyet-dinh/0005-kien-truc-theo-mau.md): lõi theo Mẫu, không viết cứng nghiệp vụ công trường.
- [0006](quyet-dinh/0006-mot-luot-duyet.md): chỉ một lượt duyệt ở bản xem trước cuối cùng; không duyệt Note riêng; điện thoại giữ nguyên âm thanh gốc và dữ liệu.
- [0007](quyet-dinh/0007-bo-stt-offline-cho-mang.md): bỏ STT offline on-device, lưu speech ngoại tuyến và chờ mạng gọi API online.
- Thay đổi 01 thêm [0006-tu-dat-placeholder](quyet-dinh/0006-tu-dat-placeholder.md) và [0007-dau-vao-gdoc-va-docx](quyet-dinh/0007-dau-vao-gdoc-va-docx.md); giữ tên file theo yêu cầu, không thay hai quyết định cũ trùng số.
- Luôn commit và push chi tiết từng bước lên GitHub `origin/main`.
- Quy tắc làm việc: Nếu Chrome/hệ thống yêu cầu người dùng thì agent cũng bị yêu cầu (hỏi và chờ xác nhận); nếu prompt của người dùng đi chệch hướng với luồng/luật/định nghĩa trong tài liệu (Doc) thì agent phải đưa ra câu hỏi lựa chọn để người dùng chốt trước khi làm.

