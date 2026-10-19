# SiteVoice — Active Context

Cập nhật: 2026-10-20. Đây là trạng thái hiện tại; quy tắc ở [AGENTS.md](../AGENTS.md), lịch sử ở [WORK_LOG.md](WORK_LOG.md).

## Tổng quan và trạng thái đã xác minh

- Mục tiêu: ứng dụng Android ghi âm ngoài công trường → STT → phân loại → duyệt cuối ngày → LLM tổng hợp theo Mẫu → Google Doc tối đa 2 trang.
- Hiện ở bước chuẩn bị: `app/` chỉ có README; chưa có mã nguồn Android, Gradle wrapper/cấu hình build hay test. Chưa có chức năng ứng dụng nào được triển khai trong thư mục hiện tại.
- Đã có tài liệu yêu cầu/thiết kế, 5 quyết định chấp nhận trong `quyet-dinh/`, prompt phân loại/rút gọn và bộ `schema.json`/`prompt.md`/`doc-mau.md` cho nhật ký công trường. Chưa có bằng chứng kiểm chứng luồng thực tế.
- Báo cáo tuần và nhật ký cá nhân mới có README ý tưởng. Đã kết nối Git với repository GitHub `DLongg/SiteVoice` (nhánh `main`).

## Kiến trúc và công nghệ dự kiến

- Kotlin + Jetpack Compose, Room, WorkManager, EncryptedSharedPreferences; các module dự kiến: `ui`, `audio`, `stt`, `llm`, `template`, `export`, `data`, `queue`, `settings`. Xem [kiến trúc](03-kien-truc.md), [dữ liệu](04-du-lieu.md).
- Giao diện UI: Chuẩn hóa theo skill `design-taste-frontend` (`.agents/skills/design-taste-frontend/SKILL.md`), tuân thủ quy tắc App Screens (tinh gọn, tương phản cao ngoài công trường, không gradient/shadow thừa).
- STT qua Groq/OpenAI Whisper; whisper.cpp offline ở giai đoạn sau. LLM qua interface, người dùng tự nhập API key và chọn provider (Claude/GPT-4o-mini/Gemini Flash theo định hướng hiện có).
- Mẫu đóng gói vào assets; Drive API copy Doc mẫu, Docs API thay placeholder. Đây là thiết kế, chưa phải tích hợp đã hoạt động.

## Đang triển khai và bàn giao

- [Lộ trình](07-lo-trinh.md) đang đánh dấu giai đoạn 0: thử luồng thủ công. Các task trong [kế hoạch chi tiết](ke-hoach-thuc-hien.md) đều đang ghi chưa làm; chưa có bằng chứng thử thực địa, thiết lập môi trường hay task lập trình đang dở.
- Task quản lý context đã thiết lập AGENTS/SUMMARY/WORK_LOG và đồng bộ các điểm dẫn cũ; chi tiết và kiểm tra ở WORK_LOG. Không thay đổi source/dependencies. Không còn bước triển khai context đang dở.
- Đã ghi nhận công việc tương lai mục H trong [danh sách mở rộng](08-cong-viec-tuong-lai.md): tự tạo Doc mẫu từ file Word người dùng cung cấp hoặc công cụ hỗ trợ gán trường/placeholder. Chưa triển khai, chưa chọn công cụ; không đổi ưu tiên giai đoạn 0.

## Tồn đọng cần lưu ý

- `templates/nhat-ky-cong-truong/doc-mau.md` chưa có link Google Doc; prompt/ngân sách ký tự và giới hạn 2 trang cần thử thực tế trước khi chốt.
- Chưa thể build/test ứng dụng vì chưa có project Gradle; trạng thái Android Studio/điện thoại chưa được kiểm chứng.
- Khi triển khai dữ liệu/phân loại, cần thống nhất nhãn `unclassified` trong [thiết kế Room](04-du-lieu.md) với `chua_phan_loai` trong schema/prompt; chưa có quyết định ánh xạ.
- Yêu cầu hàng đợi mạng áp dụng chung, nhưng lộ trình đặt WorkManager ở giai đoạn 3; cần làm rõ cách triển khai theo giai đoạn khi task mạng được giao, không tự bỏ yêu cầu.

## Quyết định phải giữ

- [0001](quyet-dinh/0001-stt-lai-online-offline.md): lưu âm thanh trước, STT online/offline; [0002](quyet-dinh/0002-mot-nut-ghi-am.md): một nút ghi âm, duyệt cuối ngày.
- [0003](quyet-dinh/0003-xuat-doc-bang-mau.md): xuất bằng bản sao Doc mẫu; [0004](quyet-dinh/0004-kotlin-compose-chi-android.md): Kotlin/Compose, chỉ Android; [0005](quyet-dinh/0005-kien-truc-theo-mau.md): lõi theo Mẫu, không viết cứng nghiệp vụ công trường.
- Quy tắc context có nguồn chính ở AGENTS; từ nay chỉ thêm lịch sử vào WORK_LOG, giữ nhật ký cũ để tra cứu. Không có hook tự lưu hoặc phát hiện context window.
- Quy tắc hỏi khi bước chưa rõ và hướng dẫn/chờ bước người dùng thao tác đã được bổ sung tại [AGENTS.md — Coding, phạm vi và kiểm tra](../AGENTS.md#coding-phạm-vi-và-kiểm-tra) theo yêu cầu ngày 2026-10-20.

## Ưu tiên tiếp theo

1. Theo giai đoạn 0: thử thủ công 2–3 ngày (task 0.1), ghi bằng chứng, chốt prompt/schema (0.2), tạo và in thử Doc mẫu (0.3).
2. Kiểm chứng môi trường Android/thiết bị (0.4), rồi mới khởi tạo Kotlin/Compose (1.1.1) theo phạm vi được giao.
3. Phiên sau đọc AGENTS + file này; chỉ mở task/tài liệu liên quan. Không tự coi các ưu tiên trên là việc đã hoàn thành hoặc đã được giao thực hiện.
