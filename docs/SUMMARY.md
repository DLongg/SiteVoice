# SiteVoice — Active Context

Cập nhật: 2026-10-20. Đây là trạng thái hiện tại; quy tắc ở [AGENTS.md](../AGENTS.md), lịch sử ở [WORK_LOG.md](WORK_LOG.md).

## Tổng quan và trạng thái đã xác minh

- Mục tiêu: ứng dụng Android ghi âm ngoài công trường → STT → phân loại → chuẩn bị cuối ngày → LLM tổng hợp theo Mẫu → duyệt bản xem trước một lần → Google Doc tối đa 2 trang.
- Đã hoàn thành 100% Giai đoạn 0, Giai đoạn 1, Giai đoạn 2 và Task 3.1 Giai đoạn 3: Ghi âm M4A an toàn, STT Groq Whisper, phân loại tự động LLM, danh sách ghi chú Room, dữ liệu cố định cuối ngày (kế thừa hôm trước), LLM tổng hợp báo cáo kiểm soát ngân sách dòng, màn hình xem trước và nút duyệt 1 lần duy nhất (quyết định 0006), tích hợp Google Sign-In (`play-services-auth`), sao chép Doc mẫu bằng Drive API v3 và thay thế placeholder bằng Docs API v1 `batchUpdate`. Hàng đợi ngoại tuyến WorkManager (`SttWorker`, `ClassifyWorker`, `ExportWorker`) có ràng buộc mạng và tự động retry khi có kết nối.
- Lệnh `.\gradlew.bat testDebugUnitTest assembleDebug` build thành công 100% (25/25 unit tests pass, file APK sẵn sàng).

## Kiến trúc và công nghệ dự kiến

- Kotlin + Jetpack Compose, Room, WorkManager, EncryptedSharedPreferences, OkHttp; các module: `ui`, `audio`, `stt`, `llm`, `template`, `export`, `data`, `queue`, `settings`. Xem [kiến trúc](03-kien-truc.md), [dữ liệu](04-du-lieu.md).
- Giao diện UI: Chuẩn hóa theo skill `design-taste-frontend` (`.agents/skills/design-taste-frontend/SKILL.md`), tuân thủ quy tắc App Screens (tinh gọn, tương phản cao ngoài công trường, không gradient/shadow thừa, badge hàng đợi trực quan).
- STT qua Groq Whisper; LLM Groq `qwen/qwen3.8-27b` ổn định. Xuất Google Doc: Google Drive API v3 copy file và Google Docs API v1 batchUpdate thay placeholder.

## Đang triển khai và bàn giao

- [Lộ trình](07-lo-trinh.md): Đã hoàn thành 100% Giai đoạn 0, Giai đoạn 1, Giai đoạn 2 và Task 3.1 của Giai đoạn 3.
- Giai đoạn 3 (Chạy không cần mạng): Đã hoàn thành Task 3.1 (`SttWorker`, `ClassifyWorker`, `ExportWorker`, badge hàng đợi UI `NotesListScreen`). Đang chuyển sang Task 3.2 (whisper.cpp offline).

## Quyết định phải giữ

- [0001](quyet-dinh/0001-stt-lai-online-offline.md): lưu âm thanh trước, STT online/offline; [0002](quyet-dinh/0002-mot-nut-ghi-am.md): một nút ghi âm, duyệt cuối ngày.
- [0003](quyet-dinh/0003-xuat-doc-bang-mau.md): xuất bằng bản sao Doc mẫu; [0004](quyet-dinh/0004-kotlin-compose-chi-android.md): Kotlin/Compose, chỉ Android; [0005](quyet-dinh/0005-kien-truc-theo-mau.md): lõi theo Mẫu, không viết cứng nghiệp vụ công trường.
- [0006](quyet-dinh/0006-mot-luot-duyet.md): chỉ một lượt duyệt ở bản xem trước cuối cùng; không duyệt Note riêng; điện thoại giữ nguyên âm thanh gốc và dữ liệu.
- Luôn commit và push chi tiết từng bước lên GitHub `origin/main`.

## Ưu tiên tiếp theo

1. Kết nối điện thoại thật để cài đặt bản APK mới nhất, kiểm tra trải nghiệm ngoại tuyến (tắt mạng ghi âm → xếp hàng WorkManager → bật mạng tự động xử lý STT và phân loại).
2. Triển khai Task 3.2: Tích hợp whisper.cpp (JNI/NDK) để chạy STT offline trực tiếp trên máy không phụ thuộc API.
