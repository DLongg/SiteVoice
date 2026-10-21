# SiteVoice — Active Context

Cập nhật: 2026-10-21. Đây là trạng thái hiện tại; quy tắc ở [AGENTS.md](../AGENTS.md), lịch sử ở [WORK_LOG.md](WORK_LOG.md).

## Tổng quan và trạng thái đã xác minh

- Mục tiêu: ứng dụng Android ghi âm ngoài công trường → STT → phân loại → chuẩn bị cuối ngày → LLM tổng hợp theo Mẫu → duyệt bản xem trước một lần → Google Doc tối đa 2 trang.
- Đã hoàn thành 100% Giai đoạn 0, Giai đoạn 1, Giai đoạn 2 và Giai đoạn 3. Đã tự kiểm thử thực tế và xác minh toàn bộ luồng trực tiếp trên điện thoại thật Xiaomi 1080x2412 (id 33c4f658): ghi âm, hiển thị ghi chú mới nhất lên đầu, kế thừa thời tiết hôm trước, xem trước báo cáo khống chế số dòng, xem/sửa live JSON trong Developer Mode, phê duyệt và xuất Google Doc tự động lên Google Drive.
- Lệnh `.\gradlew.bat testDebugUnitTest assembleDebug` build thành công 100% (52/52 tests pass, APK 11.94 MB đã cài đặt và chạy hoàn hảo trên máy 33c4f658). Bổ sung shorthand aliases (`s_n`, `c_n`, `t_bt`, `ng_ch`, `ng_cn`, `thi_cong_trong_ngay`...) và `matchCase = false` cho xuất Google Docs.

## Kiến trúc và công nghệ dự kiến

- Kotlin + Jetpack Compose, Room, WorkManager, EncryptedSharedPreferences, OkHttp; các module: `ui`, `audio`, `stt`, `llm`, `template`, `export`, `data`, `queue`, `settings`. Xem [kiến trúc](03-kien-truc.md), [dữ liệu](04-du-lieu.md).
- Giao diện UI: Chuẩn hóa theo skill `design-taste-frontend` (`.agents/skills/design-taste-frontend/SKILL.md`), tuân thủ quy tắc App Screens (tinh gọn, tương phản cao, thẻ trạng thái hàng đợi `⏳ Chờ mạng` trực quan).
- STT online qua Groq Whisper (`whisper-large-v3-turbo`); LLM Groq `qwen/qwen3.8-27b` ổn định. Xuất Google Doc: Google Drive API v3 copy file và Google Docs API v1 batchUpdate thay placeholder.

## Đang triển khai và bàn giao

- Yêu cầu thay đổi 01 (2026-10-20): đã cập nhật đủ phần tài liệu/cấu hình cho thiết lập form Google Doc/DOCX: [đặc tả](10-thiet-lap-form.md), hai quyết định mới, prompt nhận diện, khuôn schema, kiến trúc/dữ liệu/LLM/xuất/lộ trình và Mẫu mặc định 13 chỗ điền trong Doc + 1 trường ẩn. Đây là đặc tả; Android/assets/DOCX không đổi, chức năng thiết lập/đo trang chưa được triển khai/xác minh.
- Người dùng đã chốt: dùng `GEMINI.md` hoặc `AGENTS.md` thay CLAUDE và F12/F13. Đã giữ GEMINI làm điểm dẫn tới docs/10, bổ sung quy tắc Mẫu/code tìm vị trí/bảo vệ bản gốc ở AGENTS, giữ F11 backup, thêm F12/F13; H/I mô tả mở rộng/dự phòng, phần thiết lập cơ bản dẫn tới docs/10. Không còn vướng thiếu CLAUDE hoặc trùng F11.
- Kiểm tra thay đổi 01: Python xác nhận 5 file mới, JSON/metadata/giới hạn, 13 mã B/C khớp schema, prompt/luật/link/F12-F13 đúng; `git diff --check` đạt. Checklist loại mã cũ toàn repo vẫn CHƯA ĐẠT (13 file code/test/assets/DOCX, mẫu cá nhân, prompt đầu vào và lịch sử). Đã giải thích ngoại lệ là giữ phạm vi tài liệu, không coi người dùng đã phê duyệt đổi checklist hoặc mở rộng sửa mã. Danh sách file/kết quả ở [WORK_LOG](WORK_LOG.md). Các câu hỏi sản phẩm/ký tự thiếu giữ trong docs/10; bước 8b và chuẩn bị bài kiểm tra chuẩn vẫn chưa làm. Không chạy build/test Android cho task tài liệu.
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
- Quy tắc làm việc: Nếu Chrome/trình duyệt/hệ thống yêu cầu người dùng thì agent cũng bị yêu cầu tương tự (hỏi và chờ xác nhận); nếu xung đột giữa luật và Doc thì bắt buộc hỏi người dùng để chốt, tuyệt đối không tự ý quyết định.

## Ưu tiên tiếp theo

1. Cài đặt APK lên điện thoại thật khi kết nối lại USB và kiểm tra thực tế ngoài công trường.
2. Triển khai Bước 4 (nếu cần tiếp tục nâng cao): Dynamic few-shot cache từ thao tác sửa đổi của người dùng trên UI.
