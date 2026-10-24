# SiteVoice — Active Context

Cập nhật: 2026-10-24. Đây là trạng thái hiện tại; quy tắc ở [AGENTS.md](../AGENTS.md), lịch sử ở [WORK_LOG.md](WORK_LOG.md).

## Tổng quan và trạng thái đã xác minh

- Mục tiêu: ứng dụng Android ghi âm ngoài công trường → STT → phân loại → chuẩn bị cuối ngày → LLM tổng hợp theo Mẫu → duyệt bản xem trước một lần → Google Doc tối đa 2 trang.
- Đã hoàn thành Giai đoạn 0 đến Giai đoạn 3 và Milestone M1 (Cập nhật đồng bộ Tài liệu Thiết kế). Đã tự kiểm thử thực tế trên điện thoại thật Xiaomi 1080x2412 (id 33c4f658): ghi âm, hiển thị ghi chú mới nhất, kế thừa thời tiết, khống chế số dòng, duyệt và xuất Google Doc tự động lên Google Drive.
- Lệnh `.\gradlew.bat testDebugUnitTest assembleDebug` build thành công 100% (52/52 tests pass, APK 11.94 MB đã cài đặt và chạy hoàn hảo trên máy 33c4f658).

## Kiến trúc và công nghệ dự kiến

- Kotlin + Jetpack Compose, Room, WorkManager, EncryptedSharedPreferences, OkHttp; các module: `ui`, `audio`, `stt`, `llm`, `template`, `export`, `data`, `queue`, `settings`. Xem [kiến trúc](03-kien-truc.md), [dữ liệu](04-du-lieu.md).
- Giao diện UI: Chuẩn hóa theo skill `design-taste-frontend` (`.agents/skills/design-taste-frontend/SKILL.md`), tuân thủ quy tắc App Screens (tinh gọn, tương phản cao, quy trình HITL 2 bước, thanh trượt nhiệt độ rõ ràng).
- STT qua Groq Whisper; LLM độc lập: Gemini 3.8 Flash (qua Vilao AI), fallback Groq. Xuất Google Doc: Google Drive API v3 copy file và Google Docs API v1 batchUpdate thay placeholder kèm cơ chế quét sạch 2 lớp.

## Đang triển khai và bàn giao

- **Đã hoàn thành Milestone M1 (Requirement R1) — Cập nhật đồng bộ Tài liệu Thiết kế**:
  - `docs/10-thiet-lap-form.md`: Bổ sung regex đề mục mở rộng (La Mã, chữ cái, mục con, dấu `:`), truyền nhãn bảng trái sang phải, gộp dòng chấm liên tiếp thành `danh_sach`, bảo tồn cụm chấm nội dòng, quy trình Human-in-the-loop 2 bước (Bước 1 duyệt thẻ neo, Bước 2 duyệt Prompt thống nhất & thanh trượt nhiệt độ), và chuẩn hóa tương thích `schemaJson` song ngữ.
  - `docs/05-llm.md`: Bổ sung cấu hình `llmTemperature` (`0.0 - 0.8`, mặc định `0.2f`) trong `SecurePreferences`, cơ chế tổng hợp JSON động 1-1 theo `summary_fields` của Mẫu trong `ReportSynthesizer`, và cấu trúc System Prompt thống nhất do AI sinh (Pha 4b).
  - `docs/06-google-doc-mau.md`: Bổ sung cơ chế xuất Google Doc quét sạch 2 lớp (Lớp 1: map-based replace; Lớp 2: documents.get quét regex `\{\{[^{}]+\}\}` và batchUpdate xóa sạch 100% tag rỗng), cùng giá trị mặc định chuỗi rỗng `""` trong `PlaceholderMapper`.
- **Kế hoạch tiếp theo**:
  - Milestone M2: Nâng cấp `FormCandidateScanner`, `FormLlmDescriber`, `FormSetupService`, và `TemplateSchema` theo tài liệu thiết kế đã chuẩn hóa.

## Quyết định phải giữ

- [0001](quyet-dinh/0001-stt-lai-online-offline.md): lưu âm thanh trước; [0002](quyet-dinh/0002-mot-nut-ghi-am.md): một nút ghi âm, duyệt cuối ngày.
- [0003](quyet-dinh/0003-xuat-doc-bang-mau.md): xuất bằng bản sao Doc mẫu; [0004](quyet-dinh/0004-kotlin-compose-chi-android.md): Kotlin/Compose, chỉ Android; [0005](quyet-dinh/0005-kien-truc-theo-mau.md): lõi theo Mẫu, không viết cứng nghiệp vụ công trường.
- [0006](quyet-dinh/0006-mot-luot-duyet.md): chỉ một lượt duyệt ở bản xem trước cuối cùng; không duyệt Note riêng; điện thoại giữ nguyên âm thanh gốc và dữ liệu.
- [0007](quyet-dinh/0007-bo-stt-offline-cho-mang.md): bỏ STT offline on-device, lưu speech ngoại tuyến và chờ mạng gọi API online.
- Thay đổi 01 thêm [0006-tu-dat-placeholder](quyet-dinh/0006-tu-dat-placeholder.md) và [0007-dau-vao-gdoc-va-docx](quyet-dinh/0007-dau-vao-gdoc-va-docx.md); giữ tên file theo yêu cầu, không thay hai quyết định cũ trùng số.
- Luôn commit và push chi tiết từng bước lên GitHub `origin/main`.
- Quy tắc làm việc: Nếu Chrome/hệ thống yêu cầu người dùng thì agent cũng bị yêu cầu (hỏi và chờ xác nhận); nếu prompt của người dùng đi chệch hướng với luồng/luật/định nghĩa trong tài liệu (Doc) thì agent phải đưa ra câu hỏi lựa chọn để người dùng chốt trước khi làm.

