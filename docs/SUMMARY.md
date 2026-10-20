# SiteVoice — Active Context

Cập nhật: 2026-10-20. Đây là trạng thái hiện tại; quy tắc ở [AGENTS.md](../AGENTS.md), lịch sử ở [WORK_LOG.md](WORK_LOG.md).

## Tổng quan và trạng thái đã xác minh

- Mục tiêu: ứng dụng Android ghi âm ngoài công trường → STT → phân loại → chuẩn bị cuối ngày → LLM tổng hợp theo Mẫu → duyệt bản xem trước một lần → Google Doc tối đa 2 trang.
- Đã hoàn thành toàn bộ Giai đoạn 1 (Lõi ứng dụng chạy được: Task 1.1 - 1.5): Ghi âm nhấn giữ, STT Groq Whisper, Room Database bền vững, danh sách ghi chú hôm nay, sửa/xóa soft delete, phân loại tự động bằng Groq LLM và đổi nhãn thủ công.
- Lệnh `.\gradlew.bat test assembleDebug` build thành công 100% (70 tasks pass, 13/13 unit tests pass, sinh file APK 11.5 MB). Sẵn sàng cho Giai đoạn 2 (Ra tài liệu).
- Giai đoạn 0 đã hoàn tất 100%: đã lưu link Google Doc mẫu tại `doc-mau.md`, môi trường Android SDK/JDK 17 sẵn sàng, kết nối điện thoại thật `33c4f658` và cài app thành công.

## Kiến trúc và công nghệ dự kiến

- Kotlin + Jetpack Compose, Room, WorkManager, EncryptedSharedPreferences; các module dự kiến: `ui`, `audio`, `stt`, `llm`, `template`, `export`, `data`, `queue`, `settings`. Xem [kiến trúc](03-kien-truc.md), [dữ liệu](04-du-lieu.md).
- Giao diện UI: Chuẩn hóa theo skill `design-taste-frontend` (`.agents/skills/design-taste-frontend/SKILL.md`), tuân thủ quy tắc App Screens (tinh gọn, tương phản cao ngoài công trường, không gradient/shadow thừa).
- STT qua Groq/OpenAI Whisper; whisper.cpp offline ở giai đoạn sau. LLM qua interface, người dùng tự nhập API key và chọn provider (Claude/GPT-4o-mini/Gemini Flash theo định hướng hiện có).
- Mẫu đóng gói vào assets; Drive API copy Doc mẫu, Docs API thay placeholder. Đây là thiết kế, chưa phải tích hợp đã hoạt động.

## Đang triển khai và bàn giao

- [Lộ trình](07-lo-trinh.md) đang ở giai đoạn 0: Đã hoàn thành thử nghiệm luồng thủ công (Task 0.1) và chốt ngân sách dòng/ký tự trong `schema.json` (Task 0.2).
- Task 0.3: Đã lưu link Google Doc mẫu (ID: `1qe5AOo45Q9N1XenU7gsdAQkbbmSZ5guO`) vào `doc-mau.md`.
- Task 0.4 & 1.1.2: Kết nối thiết bị thật qua ADB, cài đặt thành công `app-debug.apk` và khởi chạy trên điện thoại.
- Đã ghi nhận công việc tương lai mục H trong [danh sách mở rộng](08-cong-viec-tuong-lai.md): tự tạo Doc mẫu từ file Word người dùng cung cấp hoặc công cụ hỗ trợ gán trường/placeholder.
- Đang làm rõ yêu cầu ngày 2026-10-20 về lưu/backup/duyệt: tổ chức Google Docs để thuận tiện lấy tài liệu theo khoảng ngày (không triển khai in); có bản trên điện thoại, máy tính và Google Docs; dọn dữ liệu theo khoảng ngày; thông báo duyệt cuối ngày, chưa duyệt vẫn giữ dữ liệu.
- Đã chốt: [0006](quyet-dinh/0006-mot-luot-duyet.md) chỉ một lượt duyệt ở bản xem trước cuối cùng; không duyệt Note riêng. Backup ngày đã duyệt giữ nội dung hoàn chỉnh; ngày chưa duyệt giữ văn bản và thông tin liên quan đến khi duyệt xong, không giới hạn số ngày. Backup không kèm âm thanh; điện thoại vẫn giữ đầy đủ dữ liệu chưa duyệt và âm thanh gốc để nghe lại, không tự xóa khi duyệt xong. Nguồn ở [yêu cầu](02-yeu-cau.md#lưu-dữ-liệu-và-backup); đã cập nhật tài liệu, chưa triển khai.
- Chưa chốt: Doc theo ngày/tháng/cách gom khoảng ngày; backup máy tính thủ công/tự động, định dạng backup và phạm vi dọn dữ liệu; hành vi lúc 02:00. Chờ trả lời trước khi sửa phần phụ thuộc; không hỏi lại thời hạn giữ ngày chưa duyệt hay việc giữ âm thanh trên điện thoại đã chốt.

## Tồn đọng cần lưu ý

- Đã có link Google Doc mẫu và cài đặt app lên thiết bị thật.
- Khi triển khai dữ liệu/phân loại, cần thống nhất nhãn `unclassified` trong [thiết kế Room](04-du-lieu.md) với `chua_phan_loai` trong schema/prompt; chưa có quyết định ánh xạ.
- Yêu cầu hàng đợi mạng áp dụng chung, nhưng lộ trình đặt WorkManager ở giai đoạn 3; cần làm rõ cách triển khai theo giai đoạn khi task mạng được giao, không tự bỏ yêu cầu.

## Quyết định phải giữ

- [0001](quyet-dinh/0001-stt-lai-online-offline.md): lưu âm thanh trước, STT online/offline; [0002](quyet-dinh/0002-mot-nut-ghi-am.md): một nút ghi âm, duyệt cuối ngày.
- [0003](quyet-dinh/0003-xuat-doc-bang-mau.md): xuất bằng bản sao Doc mẫu; [0004](quyet-dinh/0004-kotlin-compose-chi-android.md): Kotlin/Compose, chỉ Android; [0005](quyet-dinh/0005-kien-truc-theo-mau.md): lõi theo Mẫu, không viết cứng nghiệp vụ công trường.
- Quy tắc context có nguồn chính ở AGENTS; từ nay chỉ thêm lịch sử vào WORK_LOG, giữ nhật ký cũ để tra cứu. Không có hook tự lưu hoặc phát hiện context window.
- Quy tắc hỏi khi bước chưa rõ và hướng dẫn/chờ bước người dùng thao tác đã được bổ sung tại [AGENTS.md — Coding, phạm vi và kiểm tra](../AGENTS.md#coding-phạm-vi-và-kiểm-tra) theo yêu cầu ngày 2026-10-20.

## Ưu tiên tiếp theo

1. Triển khai Giai đoạn 2 — Task 2.1: Entity & DAO cho DailyInput và Màn hình nhập dữ liệu cố định cuối ngày (thời tiết, nhân lực, thiết bị).
2. Triển khai Giai đoạn 2 — Task 2.2: LLM tổng hợp báo cáo theo Mẫu nhật ký công trường và kiểm soát ngân sách dòng/ký tự.
3. Triển khai Giai đoạn 2 — Task 2.3: Màn hình xem trước và duyệt báo cáo 1 lần duy nhất (quyết định 0006).
