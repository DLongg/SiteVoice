# SiteVoice — Active Context

Cập nhật: 2026-10-20. Đây là trạng thái hiện tại; quy tắc ở [AGENTS.md](../AGENTS.md), lịch sử ở [WORK_LOG.md](WORK_LOG.md).

## Tổng quan và trạng thái đã xác minh

- Mục tiêu: ứng dụng Android ghi âm ngoài công trường → STT → phân loại → chuẩn bị cuối ngày → LLM tổng hợp theo Mẫu → duyệt bản xem trước một lần → Google Doc tối đa 2 trang.
- Đã hoàn thành 100% Giai đoạn 0, Giai đoạn 1 và Giai đoạn 2: Ghi âm M4A an toàn, STT Groq Whisper, phân loại tự động LLM, danh sách ghi chú Room, dữ liệu cố định cuối ngày (kế thừa hôm trước), LLM tổng hợp báo cáo kiểm soát ngân sách dòng, màn hình xem trước và nút duyệt 1 lần duy nhất (quyết định 0006), tích hợp Google Sign-In (`play-services-auth`), sao chép Doc mẫu bằng Drive API v3 và thay thế placeholder bằng Docs API v1 `batchUpdate`.
- Lệnh `.\gradlew.bat testDebugUnitTest assembleDebug` build thành công 100% (24/24 unit tests pass, file APK sẵn sàng).

## Kiến trúc và công nghệ dự kiến

- Kotlin + Jetpack Compose, Room, WorkManager, EncryptedSharedPreferences, OkHttp; các module dự kiến: `ui`, `audio`, `stt`, `llm`, `template`, `export`, `data`, `queue`, `settings`. Xem [kiến trúc](03-kien-truc.md), [dữ liệu](04-du-lieu.md).
- Giao diện UI: Chuẩn hóa theo skill `design-taste-frontend` (`.agents/skills/design-taste-frontend/SKILL.md`), tuân thủ quy tắc App Screens (tinh gọn, tương phản cao ngoài công trường, không gradient/shadow thừa).
- STT qua Groq Whisper; LLM Groq `qwen/qwen3.8-27b` ổn định. Xuất Google Doc: Google Drive API v3 copy file và Google Docs API v1 batchUpdate thay placeholder.

## Đang triển khai và bàn giao

- [Lộ trình](07-lo-trinh.md): Đã hoàn thành 100% Giai đoạn 0, Giai đoạn 1 và Giai đoạn 2.
- Giai đoạn 2 (Ra tài liệu): Đã hoàn thành toàn bộ Task 2.1, 2.2, 2.3 (2.3.1 – 2.3.6). Mã nguồn đăng nhập Google, cấu hình Doc mẫu, Web Client ID, sao chép và điền placeholder Google Doc đã sẵn sàng.
- Đang bắt đầu Giai đoạn 3 (Chạy không cần mạng): Task 3.1 (Hàng đợi WorkManager cho STT, phân loại, xuất tài liệu) và Task 3.2 (whisper.cpp offline).

## Quyết định phải giữ

- [0001](quyet-dinh/0001-stt-lai-online-offline.md): lưu âm thanh trước, STT online/offline; [0002](quyet-dinh/0002-mot-nut-ghi-am.md): một nút ghi âm, duyệt cuối ngày.
- [0003](quyet-dinh/0003-xuat-doc-bang-mau.md): xuất bằng bản sao Doc mẫu; [0004](quyet-dinh/0004-kotlin-compose-chi-android.md): Kotlin/Compose, chỉ Android; [0005](quyet-dinh/0005-kien-truc-theo-mau.md): lõi theo Mẫu, không viết cứng nghiệp vụ công trường.
- [0006](quyet-dinh/0006-mot-luot-duyet.md): chỉ một lượt duyệt ở bản xem trước cuối cùng; không duyệt Note riêng; điện thoại giữ nguyên âm thanh gốc và dữ liệu.
- Luôn commit và push chi tiết từng bước lên GitHub `origin/main`.

## Ưu tiên tiếp theo

1. Triển khai Giai đoạn 3 — Task 3.1: Hàng đợi WorkManager xử lý các tác vụ mạng (STT, phân loại, xuất tài liệu) có ràng buộc `NetworkType.CONNECTED` và retry khi có mạng.
2. Cài đặt bản APK mới nhất lên điện thoại thật và xác minh người dùng đăng nhập tài khoản Google để xuất tài liệu thực tế.
