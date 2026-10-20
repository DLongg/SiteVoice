# SiteVoice — Active Context

Cập nhật: 2026-10-20. Đây là trạng thái hiện tại; quy tắc ở [AGENTS.md](../AGENTS.md), lịch sử ở [WORK_LOG.md](WORK_LOG.md).

## Tổng quan và trạng thái đã xác minh

- Mục tiêu: ứng dụng Android ghi âm ngoài công trường → STT → phân loại → chuẩn bị cuối ngày → LLM tổng hợp theo Mẫu → duyệt bản xem trước một lần → Google Doc tối đa 2 trang.
- Đã hoàn thành 100% Giai đoạn 0, Giai đoạn 1, Giai đoạn 2 và Giai đoạn 3: Ghi âm M4A an toàn, STT Groq Whisper, phân loại tự động LLM, danh sách ghi chú Room, dữ liệu cố định cuối ngày (kế thừa hôm trước), LLM tổng hợp báo cáo kiểm soát ngân sách dòng, màn hình xem trước và nút duyệt 1 lần duy nhất (quyết định 0006), tích hợp Google Sign-In (`play-services-auth`), sao chép Doc mẫu bằng Drive API v3 và thay thế placeholder bằng Docs API v1 `batchUpdate`. Hàng đợi ngoại tuyến WorkManager (`SttWorker`, `ClassifyWorker`, `ExportWorker`).
- Theo Quyết định 0007: Bỏ STT offline on-device, chuyển hoàn toàn sang cơ chế lưu speech ngoại tuyến an toàn trên điện thoại và xếp hàng chờ mạng gọi Groq Whisper API (Whisper Large-v3-Turbo). APK giảm xuống 11.94 MB.
- Lệnh `.\gradlew.bat testDebugUnitTest assembleDebug` build thành công 100% (APK 11.94 MB đã cài đặt trên máy thật `33c4f658`). Đã sửa lỗi mất file âm thanh trong `AudioRecorder`, bổ sung bộ lọc hallucination Whisper khi im lặng, hỗ trợ LLM bóc tách đa mục cho bản ghi âm gộp nhiều ý, đảo thứ tự danh sách ghi chú mới nhất lên đầu, mở rộng Google Drive scope sang `drive` và tích hợp tự động hiển thị màn hình cấp quyền Google OAuth (`UserRecoverableAuthException`).

## Kiến trúc và công nghệ dự kiến

- Kotlin + Jetpack Compose, Room, WorkManager, EncryptedSharedPreferences, OkHttp; các module: `ui`, `audio`, `stt`, `llm`, `template`, `export`, `data`, `queue`, `settings`. Xem [kiến trúc](03-kien-truc.md), [dữ liệu](04-du-lieu.md).
- Giao diện UI: Chuẩn hóa theo skill `design-taste-frontend` (`.agents/skills/design-taste-frontend/SKILL.md`), tuân thủ quy tắc App Screens (tinh gọn, tương phản cao, thẻ trạng thái hàng đợi `⏳ Chờ mạng` trực quan).
- STT online qua Groq Whisper (`whisper-large-v3-turbo`); LLM Groq `qwen/qwen3.8-27b` ổn định. Xuất Google Doc: Google Drive API v3 copy file và Google Docs API v1 batchUpdate thay placeholder.

## Đang triển khai và bàn giao

- [Lộ trình](07-lo-trinh.md): Đã hoàn thành 100% Giai đoạn 0, 1, 2, 3 và hoàn thành trọn vẹn 100% Nhóm 4.1 (Task 4.1.1 - 4.1.4) của Giai đoạn 4. Toàn bộ các nhóm mở rộng (4.2, 4.3, 4.4) đã được dời sang [08-cong-viec-tuong-lai.md](08-cong-viec-tuong-lai.md) theo yêu cầu người dùng.
- Nâng cao độ chính xác phân loại bằng Model Harness ([10-do-chinh-xac.md](10-do-chinh-xac.md)): Đã hoàn thành Bước 1: Tài liệu hóa kiến trúc giàn giáo phân loại đa tầng, cập nhật [05-llm.md](05-llm.md), xây dựng Bộ dữ liệu vàng 60 mẫu (`golden_dataset.json`), và thiết lập bộ kiểm chuẩn tự động `ClassificationBenchmarkTest.kt` đo điểm chuẩn ban đầu (Baseline: 81.67%).
- Kiểm thử: 34/34 unit tests pass 100%; APK debug (11.94 MB) đã cài đặt và chạy ổn định trên máy thật `33c4f658`.

## Quyết định phải giữ

- [0001](quyet-dinh/0001-stt-lai-online-offline.md): lưu âm thanh trước; [0002](quyet-dinh/0002-mot-nut-ghi-am.md): một nút ghi âm, duyệt cuối ngày.
- [0003](quyet-dinh/0003-xuat-doc-bang-mau.md): xuất bằng bản sao Doc mẫu; [0004](quyet-dinh/0004-kotlin-compose-chi-android.md): Kotlin/Compose, chỉ Android; [0005](quyet-dinh/0005-kien-truc-theo-mau.md): lõi theo Mẫu, không viết cứng nghiệp vụ công trường.
- [0006](quyet-dinh/0006-mot-luot-duyet.md): chỉ một lượt duyệt ở bản xem trước cuối cùng; không duyệt Note riêng; điện thoại giữ nguyên âm thanh gốc và dữ liệu.
- [0007](quyet-dinh/0007-bo-stt-offline-cho-mang.md): bỏ STT offline on-device, lưu speech ngoại tuyến và chờ mạng gọi API online.
- Luôn commit và push chi tiết từng bước lên GitHub `origin/main`.

## Ưu tiên tiếp theo

1. Triển khai Bước 2 của Model Harness: Xây dựng `TextNormalizer.kt` (chuẩn hóa từ điển) và `ClauseSplitter.kt` (tách câu/mệnh đề công trường) để nâng điểm benchmark nhóm `multi_intent` và `dialect_and_typo`.
2. Triển khai Bước 3: Tầng quy tắc tất định 0ms offline (`DeterministicRuleEngine.kt`) và hậu kiểm `SemanticGuardrails.kt`.
