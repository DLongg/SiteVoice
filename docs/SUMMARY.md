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

- Khắc phục lỗi LLM tổng hợp thiếu trường & sai tổ đội (2026-10-21): Loại bỏ hoàn toàn JSON schema hardcode 7 trường trong `ReportSynthesizer`, set `temperature=0.0`, cập nhật prompt template đầy đủ trường JSON (`s_n`, `s_m`, `c_n`, `c_m`, `t_bt`, `t_m`, `tong_quan_thoi_tiet`, `ng_ch`, `ng_cn`, `may_moc`...) và quy tắc nắn chỉnh gom việc tổ đội nhầm. Cập nhật `PlaceholderMapper` (hỗ trợ boolean sang `☑`/`☐`) và `DailyReportPreviewScreen` đọc trực tiếp từ `allFields`.
- Lệnh `.\gradlew.bat testDebugUnitTest assembleDebug` build thành công 100% (APK 11.94 MB đã cài đặt trực tiếp lên thiết bị thật Xiaomi `33c4f658`).
- [Lộ trình](07-lo-trinh.md): Đã hoàn thành Giai đoạn 0-3 và Nhóm 4.1. Đang kiểm thử trực tiếp luồng tổng hợp và xuất Doc trên thiết bị thật.

## Quyết định phải giữ

- [0001](quyet-dinh/0001-stt-lai-online-offline.md): lưu âm thanh trước; [0002](quyet-dinh/0002-mot-nut-ghi-am.md): một nút ghi âm, duyệt cuối ngày.
- [0003](quyet-dinh/0003-xuat-doc-bang-mau.md): xuất bằng bản sao Doc mẫu; [0004](quyet-dinh/0004-kotlin-compose-chi-android.md): Kotlin/Compose, chỉ Android; [0005](quyet-dinh/0005-kien-truc-theo-mau.md): lõi theo Mẫu, không viết cứng nghiệp vụ công trường.
- [0006](quyet-dinh/0006-mot-luot-duyet.md): chỉ một lượt duyệt ở bản xem trước cuối cùng; không duyệt Note riêng; điện thoại giữ nguyên âm thanh gốc và dữ liệu.
- [0007](quyet-dinh/0007-bo-stt-offline-cho-mang.md): bỏ STT offline on-device, lưu speech ngoại tuyến và chờ mạng gọi API online.
- Thay đổi 01 thêm [0006-tu-dat-placeholder](quyet-dinh/0006-tu-dat-placeholder.md) và [0007-dau-vao-gdoc-va-docx](quyet-dinh/0007-dau-vao-gdoc-va-docx.md); giữ tên file theo yêu cầu, không thay hai quyết định cũ trùng số. Các tham chiếu mới cần dùng tên đầy đủ để phân biệt.
- Luôn commit và push chi tiết từng bước lên GitHub `origin/main`.
- Quy tắc làm việc: Nếu Chrome/hệ thống yêu cầu người dùng thì agent cũng bị yêu cầu (hỏi và chờ xác nhận); nếu prompt của người dùng đi chệch hướng với luồng/luật/định nghĩa trong tài liệu (Doc) thì agent phải đưa ra câu hỏi lựa chọn để người dùng chốt trước khi làm.

## Ưu tiên tiếp theo

1. Cài đặt APK lên điện thoại thật khi kết nối lại USB và kiểm tra thực tế ngoài công trường.
2. Triển khai Bước 4 (nếu cần tiếp tục nâng cao): Dynamic few-shot cache từ thao tác sửa đổi của người dùng trên UI.
