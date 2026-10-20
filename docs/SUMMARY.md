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

- Yêu cầu thay đổi 01 (2026-10-20): chỉ tài liệu/cấu hình cho thiết lập form Google Doc/DOCX; CHƯA hoàn tất toàn bộ yêu cầu. Đã thêm [đặc tả](10-thiet-lap-form.md), hai quyết định mới, prompt nhận diện, khuôn schema; cập nhật kiến trúc/dữ liệu/LLM/xuất/lộ trình và Mẫu mặc định sang 13 chỗ điền trong Doc + 1 trường ẩn. Không sửa Android, assets hay DOCX; chức năng thiết lập/đo trang chưa được triển khai/xác minh.
- Đang chờ người dùng chốt: repo thiếu `CLAUDE.md` (không có quy tắc 1–9 để bảo toàn); F11 và mục H đã có nội dung khác. Chưa thêm hai chức năng vào `02-yeu-cau.md`, chưa sửa `08-cong-viec-tuong-lai.md`. Đã hỏi phương án tạo CLAUDE từ AGENTS, dùng F12/F13 và thay H cũ bằng tham chiếu rồi thêm H/I mới; chưa nhận trả lời.
- Kiểm tra thay đổi 01: Python xác nhận JSON hợp lệ, giữ metadata/nhãn, 13 mã ở phần B/C khớp schema, prompt khớp trường ghi chú; `git diff --check` đạt. Checklist bỏ placeholder cũ toàn repo chưa đạt: còn trong code/test/assets/DOCX, mẫu cá nhân, prompt đầu vào và lịch sử ngoài phạm vi. Chờ chốt ngoại lệ; không sửa lịch sử hoặc mã Android. Chi tiết [WORK_LOG](WORK_LOG.md). Bước tiếp: nhận câu trả lời, hoàn tất CLAUDE/F mới/H-I, chạy lại checklist rồi commit/push; không chạy build/test Android cho task tài liệu.
- [Lộ trình](07-lo-trinh.md): Đã hoàn thành 100% Giai đoạn 0, 1, 2, 3 và hoàn thành trọn vẹn 100% Nhóm 4.1 (Task 4.1.1 - 4.1.4) của Giai đoạn 4. Toàn bộ các nhóm mở rộng (4.2, 4.3, 4.4) đã được dời sang [08-cong-viec-tuong-lai.md](08-cong-viec-tuong-lai.md) theo yêu cầu người dùng.
- Model Harness & Benchmark ([10-do-chinh-xac.md](10-do-chinh-xac.md)): Đã hoàn thành Bước 1, 2 và 3: Xây dựng `TextNormalizer` (sửa lỗi nghe/viết tắt), `ClauseSplitter` (tách câu/mệnh đề), `DeterministicRuleEngine` (tầng quy tắc tất định 0ms offline), `SemanticGuardrails` (hậu kiểm enum và chống mâu thuẫn) và `HybridClassificationPipeline`. Tích hợp trực tiếp vào `ClassifyWorker`.
- Kết quả kiểm chuẩn tự động ([ClassificationBenchmarkTest.kt](../app/src/test/java/com/sitevoice/app/ClassificationBenchmarkTest.kt)): Độ chính xác tổng tăng vọt từ **81.67% lên đúng 95.00%** (57/60 câu chuẩn xác). Nhóm viết tắt/nghe nhầm đạt 100%, nhóm ngữ cảnh đạt 100%, nhóm câu đơn ý đạt 97.2%.
- Kiểm thử: Toàn bộ 17 test suites (> 45 unit tests) pass 100%; APK debug (11.94 MB) build thành công.

## Quyết định phải giữ

- [0001](quyet-dinh/0001-stt-lai-online-offline.md): lưu âm thanh trước; [0002](quyet-dinh/0002-mot-nut-ghi-am.md): một nút ghi âm, duyệt cuối ngày.
- [0003](quyet-dinh/0003-xuat-doc-bang-mau.md): xuất bằng bản sao Doc mẫu; [0004](quyet-dinh/0004-kotlin-compose-chi-android.md): Kotlin/Compose, chỉ Android; [0005](quyet-dinh/0005-kien-truc-theo-mau.md): lõi theo Mẫu, không viết cứng nghiệp vụ công trường.
- [0006](quyet-dinh/0006-mot-luot-duyet.md): chỉ một lượt duyệt ở bản xem trước cuối cùng; không duyệt Note riêng; điện thoại giữ nguyên âm thanh gốc và dữ liệu.
- [0007](quyet-dinh/0007-bo-stt-offline-cho-mang.md): bỏ STT offline on-device, lưu speech ngoại tuyến và chờ mạng gọi API online.
- Thay đổi 01 thêm [0006-tu-dat-placeholder](quyet-dinh/0006-tu-dat-placeholder.md) và [0007-dau-vao-gdoc-va-docx](quyet-dinh/0007-dau-vao-gdoc-va-docx.md); giữ tên file theo yêu cầu, không thay hai quyết định cũ trùng số. Các tham chiếu mới cần dùng tên đầy đủ để phân biệt.
- Luôn commit và push chi tiết từng bước lên GitHub `origin/main`.

## Ưu tiên tiếp theo

1. Cài đặt APK lên điện thoại thật khi kết nối lại USB và kiểm tra thực tế ngoài công trường.
2. Triển khai Bước 4 (nếu cần tiếp tục nâng cao): Dynamic few-shot cache từ thao tác sửa đổi của người dùng trên UI.
