# SiteVoice — Active Context

Cập nhật: 2026-10-20. Đây là trạng thái hiện tại; quy tắc ở [AGENTS.md](../AGENTS.md), lịch sử ở [WORK_LOG.md](WORK_LOG.md).

## Tổng quan và trạng thái đã xác minh

- Mục tiêu: ứng dụng Android ghi âm ngoài công trường → STT → phân loại → chuẩn bị cuối ngày → LLM tổng hợp theo Mẫu → duyệt bản xem trước một lần → Google Doc tối đa 2 trang.
- Đang triển khai Giai đoạn 1: Đã khởi tạo hoàn chỉnh project Android Kotlin + Compose trong `app/` (gói `com.sitevoice.app`, compileSdk 34, minSdk 26).
- Đã hoàn thành Task 1.1 (cấu trúc 9 package, copy assets, unit test pass) và Task 1.2 (Module AudioRecorder/AudioPlayer và UI nút ghi âm nhấn giữ RecordScreen). Lệnh `.\gradlew.bat assembleDebug test` build thành công 100% ra `app-debug.apk`.
- Giai đoạn 0 đã hoàn tất phân tích và tạo file `Nhat_ky_thi_cong_mau.docx`. Toàn bộ thao tác người dùng (cắm điện thoại, dán link Drive) được gom lại làm sau theo yêu cầu.

## Kiến trúc và công nghệ dự kiến

- Kotlin + Jetpack Compose, Room, WorkManager, EncryptedSharedPreferences; các module dự kiến: `ui`, `audio`, `stt`, `llm`, `template`, `export`, `data`, `queue`, `settings`. Xem [kiến trúc](03-kien-truc.md), [dữ liệu](04-du-lieu.md).
- Giao diện UI: Chuẩn hóa theo skill `design-taste-frontend` (`.agents/skills/design-taste-frontend/SKILL.md`), tuân thủ quy tắc App Screens (tinh gọn, tương phản cao ngoài công trường, không gradient/shadow thừa).
- STT qua Groq/OpenAI Whisper; whisper.cpp offline ở giai đoạn sau. LLM qua interface, người dùng tự nhập API key và chọn provider (Claude/GPT-4o-mini/Gemini Flash theo định hướng hiện có).
- Mẫu đóng gói vào assets; Drive API copy Doc mẫu, Docs API thay placeholder. Đây là thiết kế, chưa phải tích hợp đã hoạt động.

## Đang triển khai và bàn giao

- [Lộ trình](07-lo-trinh.md) đang ở giai đoạn 0: Đã hoàn thành thử nghiệm luồng thủ công (Task 0.1) và chốt ngân sách dòng/ký tự trong `schema.json` (Task 0.2).
- Task 0.3: Đã phân tích file gốc [Nhat_ky_thi_cong.docx](../templates/nhat-ky-cong-truong/Nhat_ky_thi_cong.docx) (chuẩn 2 trang A4) và tạo bản mẫu điện tử [Nhat_ky_thi_cong_mau.docx](../templates/nhat-ky-cong-truong/Nhat_ky_thi_cong_mau.docx) chứa đầy đủ placeholder `{{...}}`. Chờ người dùng tải lên Google Drive lấy link.
- Task 0.4: Môi trường Android Studio, JDK 25, Android SDK, ADB đã sẵn sàng trên máy. Thao tác cắm cáp kết nối điện thoại tạm hoãn sang Giai đoạn 1 theo yêu cầu người dùng.
- Đã ghi nhận công việc tương lai mục H trong [danh sách mở rộng](08-cong-viec-tuong-lai.md): tự tạo Doc mẫu từ file Word người dùng cung cấp hoặc công cụ hỗ trợ gán trường/placeholder.
- Đang làm rõ yêu cầu ngày 2026-10-20 về lưu/backup/duyệt: tổ chức Google Docs để thuận tiện lấy tài liệu theo khoảng ngày (không triển khai in); có bản trên điện thoại, máy tính và Google Docs; dọn dữ liệu theo khoảng ngày; thông báo duyệt cuối ngày, chưa duyệt vẫn giữ dữ liệu.
- Đã chốt: [0006](quyet-dinh/0006-mot-luot-duyet.md) chỉ một lượt duyệt ở bản xem trước cuối cùng; không duyệt Note riêng. Backup ngày đã duyệt giữ nội dung hoàn chỉnh; ngày chưa duyệt giữ văn bản và thông tin liên quan đến khi duyệt xong, không giới hạn số ngày. Backup không kèm âm thanh; điện thoại vẫn giữ đầy đủ dữ liệu chưa duyệt và âm thanh gốc để nghe lại, không tự xóa khi duyệt xong. Nguồn ở [yêu cầu](02-yeu-cau.md#lưu-dữ-liệu-và-backup); đã cập nhật tài liệu, chưa triển khai.
- Chưa chốt: Doc theo ngày/tháng/cách gom khoảng ngày; backup máy tính thủ công/tự động, định dạng backup và phạm vi dọn dữ liệu; hành vi lúc 02:00. Chờ trả lời trước khi sửa phần phụ thuộc; không hỏi lại thời hạn giữ ngày chưa duyệt hay việc giữ âm thanh trên điện thoại đã chốt.

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

1. Triển khai Task 1.3: Xây dựng module STT qua Groq Whisper API (`GroqWhisper.kt`) và tích hợp sau khi ghi âm.
2. Triển khai Task 1.4: Xây dựng Room DAO, Repository và màn hình danh sách ghi chú trong ngày.
3. Thao tác người dùng (cắm cáp USB cài app lên điện thoại, dán link Google Doc) gom lại làm khi thuận tiện.
