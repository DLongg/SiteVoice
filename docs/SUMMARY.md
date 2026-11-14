# SiteVoice — Active Context

Cập nhật: 2026-11-15. Đây là trạng thái hiện tại; quy tắc ở [AGENTS.md](../AGENTS.md), lịch sử ở [WORK_LOG.md](WORK_LOG.md).

## Tổng quan và trạng thái đã xác minh

- Mục tiêu: ứng dụng Android ghi âm ngoài công trường → STT (kèm hiệu đính ngữ cảnh xây dựng) → phân loại → chuẩn bị cuối ngày → LLM tổng hợp theo Mẫu → duyệt bản xem trước một lần → Google Doc tối đa 2 trang.
- Đã hoàn thành 100% Giai đoạn 0–3, Quyết định 0008, và toàn bộ 4 mục nâng cấp giao diện UI (Milestone R1, R2, R3, R4).
- Toàn bộ 223/223 unit tests pass 100%, file APK `app\build\outputs\apk\debug\app-debug.apk` đã cài đặt và chạy trơn tru trên thiết bị thật `33c4f658`, `logcat AndroidRuntime:E` sạch.

## Kiến trúc và công nghệ

- Kotlin + Jetpack Compose, Room, WorkManager, EncryptedSharedPreferences, OkHttp; các module: `ui`, `audio`, `stt`, `llm`, `template`, `export`, `data`, `queue`, `settings`, `formsetup`. Xem [kiến trúc](03-kien-truc.md), [dữ liệu](04-du-lieu.md).
- Giao diện UI: Chuẩn hóa theo skill `design-taste-frontend` (`.agents/skills/design-taste-frontend/SKILL.md`) và [design-system/sitevoice/MASTER.md](../design-system/sitevoice/MASTER.md). Đã giải quyết triệt để 3 tồn đọng: sửa giãn dòng chữ nhỏ (R1), thay emoji bằng vector icon (R2), tách chuỗi tiếng Việt vào `strings.xml` (R3).
- Ảnh chụp màn hình nghiệm thu thực tế lưu tại `docs/screenshots/` (`template_editor.png`, `daily_report.png`, `notes_list.png`).

## Đang triển khai và bàn giao

- **Hoàn thành toàn diện 4 Milestone Nâng cấp Giao diện (R1 - R4)**:
  - R1: Thêm `lineHeight = TextSizes.*Line` tương ứng tại 251 call sites trong 11 file `ui/`.
  - R2: Thay thế 100% emoji bằng bộ Material vector icons trên thanh điều hướng và các màn hình.
  - R3: Tách toàn bộ literal UI tiếng Việt hardcoded sang ~130 khóa `strings.xml`.
  - R4: Xác minh cài đặt & chạy thực tế trên thiết bị thật `33c4f658`, chụp 3 ảnh màn hình đối chiếu, cập nhật tài liệu và push Git.
- **Trạng thái**: Sẵn sàng bàn giao cho người dùng nghiệm thu tổng thể.

## Quyết định phải giữ

- [0001](quyet-dinh/0001-stt-lai-online-offline.md): lưu âm thanh trước; [0002](quyet-dinh/0002-mot-nut-ghi-am.md): một nút ghi âm, duyệt cuối ngày.
- [0003](quyet-dinh/0003-xuat-doc-bang-mau.md): xuất bằng bản sao Doc mẫu; [0004](quyet-dinh/0004-kotlin-compose-chi-android.md): Kotlin/Compose, chỉ Android; [0005](quyet-dinh/0005-kien-truc-theo-mau.md): lõi theo Mẫu, không viết cứng nghiệp vụ công trường.
- [0006](quyet-dinh/0006-mot-luot-duyet.md): chỉ một lượt duyệt ở bản xem trước cuối cùng; không duyệt Note riêng; điện thoại giữ nguyên âm thanh gốc và dữ liệu.
- [0007](quyet-dinh/0007-bo-stt-offline-cho-mang.md): bỏ STT offline on-device, lưu speech ngoại tuyến và chờ mạng gọi API online.
- [0008](quyet-dinh/0008-mot-ban-mau-tu-dong-duy-nhat.md): duy trì một bản mẫu tự động duy nhất từ Google Doc kết hợp duyệt 2 bước (`FormSetupDialog`).
- Luôn commit và push chi tiết từng bước lên GitHub `origin/main`.
