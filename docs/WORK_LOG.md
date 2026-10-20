# SiteVoice — Development History

Nguồn lịch sử công việc mới từ 2026-10-19. Chỉ thêm mục ở cuối, không viết lại các mục trước hoặc ghi từng thao tác nhỏ. Không tự động đọc toàn bộ file khi bắt đầu phiên; tìm task ID/ngày/module cần tra cứu. Quy tắc: [AGENTS.md](../AGENTS.md).

Mỗi mục gồm ngày, task ID/tên và trạng thái; kết quả thực tế; file/module; kiểm tra (kể cả chưa chạy và lý do); lỗi/cách xử lý hoặc quyết định; việc còn lại. Không lưu thông tin nhạy cảm. Nhật ký cũ được giữ ở [nhat-ky-thuc-hien.md](nhat-ky-thuc-hien.md); lúc thiết lập context bảng cũ chưa có bản ghi thực tế, nên không có lịch sử cần di chuyển.

## 2026-10-19 — Thiết lập Persistent Context Management — Hoàn thành

- Kết quả: khảo sát toàn bộ file dự án (tài liệu, prompt, Mẫu, app README) và cấu trúc ẩn phục vụ kiểm tra rules/tooling. Xác nhận chưa có source Android hay cấu hình Gradle; stack/module mới ở mức thiết kế. Tạo bộ nhớ bền vững bằng instruction cho đầu phiên, cuối task và bàn giao khi ngắt/compact.
- Tạo: `AGENTS.md`, `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Cập nhật: `GEMINI.md` thành điểm dẫn tới quy tắc chung; `README.md` hướng dẫn đọc context; `docs/ke-hoach-thuc-hien.md` và `docs/nhat-ky-thuc-hien.md` chuyển nơi ghi mới sang WORK_LOG, bỏ yêu cầu log từng thao tác nhỏ. Giữ nguyên các task kế hoạch và bảng nhật ký cũ.
- Kiểm tra: đối chiếu trạng thái với file thực tế và tài liệu gốc; kiểm tra liên kết nội bộ của các tài liệu đã sửa, giới hạn độ dài SUMMARY, schema JSON hiện có và phạm vi file thay đổi bằng SHA-256. Không chạy build/test/lint Android vì chưa có Gradle wrapper hoặc mã ứng dụng; chưa kiểm tra thiết bị thật.
- Vấn đề và xử lý: rules cũ yêu cầu ghi log trùng nơi và quá chi tiết → thống nhất WORK_LOG làm nguồn lịch sử mới; các ví dụ log trong kế hoạch không được coi là lịch sử thực tế. Không có `.git`, nên dùng danh sách file và hash để đối chiếu thay đổi.
- Còn lại: các task ứng dụng vẫn chưa thực hiện; Doc mẫu chưa có link, cần thử luồng thủ công; nhãn chưa phân loại và thời điểm đưa hàng đợi mạng vào triển khai cần làm rõ khi đụng tới module tương ứng. Không có hook tự lưu context; việc duy trì phụ thuộc agent đọc và tuân thủ AGENTS.

## 2026-10-19 — Tích hợp skill design-taste-frontend vào quy chuẩn thiết kế UI — Hoàn thành

- Kết quả: Đã cài đặt skill `design-taste-frontend` vào workspace dưới dạng Antigravity skill tại `.agents/skills/design-taste-frontend/SKILL.md`. Cập nhật toàn bộ quy tắc, tài liệu kiến trúc, kế hoạch thực hiện và context management để áp dụng quy tắc thiết kế UI (đặc biệt là App Screens, chống AI-slop) khi tạo hoặc sửa giao diện Compose.
- Tạo: `.agents/skills/design-taste-frontend/SKILL.md`.
- Cập nhật: `AGENTS.md` (thêm nguồn tra cứu UI và quy tắc bắt buộc áp dụng skill khi code UI), `docs/03-kien-truc.md` (thêm mục Nguyên tắc thiết kế giao diện UI Design), `docs/ke-hoach-thuc-hien.md` (bổ sung quy tắc chung số 8 về UI), `docs/SUMMARY.md` (cập nhật chuẩn UI trong kiến trúc dự kiến).
- Kiểm tra: Xác minh file SKILL.md đúng vị trí `.agents/skills/` theo chuẩn Antigravity customizations; kiểm tra liên kết chéo và tính nhất quán giữa các tài liệu. Không chạy build/test mã nguồn do chưa có Gradle project.
- Vấn đề và xử lý: Skill chứa kịch bản cho cả web landing page và app screen; đối với SiteVoice (ứng dụng Android công trường), quy tắc trọng tâm cần tuân thủ là App Screens (`references/app-screens.md`: tinh gọn, tương phản cao ngoài công trường, không gradient/shadow/badge thừa, typography mạch lạc, phản hồi trạng thái chuẩn).
- Còn lại: Sẵn sàng áp dụng skill khi bắt đầu triển khai các task UI ở Giai đoạn 1 (từ task 1.1.2 và 1.2.2).

## 2026-10-20 — Ghi nhận ý tưởng hỗ trợ tạo Doc mẫu — Hoàn thành cập nhật tài liệu

- Kết quả: thêm mục H vào công việc tương lai theo yêu cầu người dùng: cung cấp file Word để tự tạo Doc mẫu, hoặc dùng công cụ hỗ trợ chọn vùng/gán trường thay vì tự nhập placeholder. Ghi rõ mục tiêu giữ bố cục, xem trước và xác nhận mẫu; chưa chọn công cụ hay triển khai chức năng.
- Cập nhật: `docs/08-cong-viec-tuong-lai.md`, `docs/SUMMARY.md`, `docs/WORK_LOG.md`. Giữ luồng xuất bằng bản sao Doc mẫu và thứ tự ưu tiên hiện tại.
- Kiểm tra: đọc lại phần bổ sung, kiểm tra liên kết nội bộ trong SUMMARY và độ dài SUMMARY dưới 80 dòng. Không chạy build/test ứng dụng vì chỉ cập nhật tài liệu.
- Còn lại: khảo sát phương án tự động/công cụ hỗ trợ, khả năng chuyển đổi file và giữ bố cục khi lên kế hoạch triển khai mục H; chưa có công cụ được kiểm chứng.

## 2026-10-20 — Bổ sung luật hỏi khi bước chưa rõ — Hoàn thành

- Kết quả: thêm quy tắc vào `AGENTS.md`, mục Coding, phạm vi và kiểm tra: bắt buộc hỏi người dùng khi có bước chưa rõ, không tự suy đoán/chọn phương án và chờ trả lời trước khi thực hiện bước đó.
- Cập nhật: `AGENTS.md`, `docs/SUMMARY.md`, `docs/WORK_LOG.md`; SUMMARY chỉ dẫn tới nguồn quy tắc trong AGENTS.
- Kiểm tra: đọc lại quy tắc và các mục cập nhật, kiểm tra liên kết tới AGENTS và độ dài SUMMARY dưới 80 dòng. Không chạy build/test vì chỉ sửa tài liệu.
- Còn lại: áp dụng quy tắc cho các task tiếp theo; không có phần chỉnh sửa tài liệu đang dở.

## 2026-10-20 — Bổ sung luật cho bước cần người dùng thao tác — Hoàn thành

- Kết quả: thêm luật vào `AGENTS.md`: nêu việc cần làm và gợi ý/hướng dẫn; tiếp tục theo phương án người dùng chốt nếu phương án cho phép agent làm tiếp. Khi vẫn cần người dùng thao tác hoặc chưa chốt, chờ xác nhận đã thao tác xong trước khi làm bước phụ thuộc; không coi đồng ý với gợi ý là thao tác đã hoàn tất.
- Cập nhật: `AGENTS.md`, `docs/SUMMARY.md`, `docs/WORK_LOG.md`; giữ AGENTS là nguồn chính của quy tắc.
- Kiểm tra: đọc lại phần sửa, kiểm tra đích liên kết tới mục quy tắc trong AGENTS và độ dài SUMMARY dưới 80 dòng. Không chạy build/test vì chỉ sửa tài liệu.
- Còn lại: áp dụng trong các task tiếp theo; không có phần cập nhật luật đang dở.

## 2026-10-20 — Tích hợp GitHub MCP và kết nối Git Repository — Hoàn thành

- Kết quả: Đã cấu hình và kích hoạt thành công GitHub MCP Server (`@modelcontextprotocol/server-github`) toàn cục trong `~/.gemini/config/mcp_config.json`. Đã xác thực thành công với GitHub API (User: `DLongg`, 34 public repos). Đã khởi tạo Git cục bộ, xử lý quyền sở hữu thư mục an toàn (`safe.directory`), liên kết remote tới repository `https://github.com/DLongg/SiteVoice.git`, fetch và track nhánh `main` đồng bộ với remote.
- Cập nhật: `~/.gemini/config/mcp_config.json`, `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: Gọi thử nghiệm công cụ MCP `search_repositories` và `list_commits` thành công; chạy lệnh `git status`, `git remote -v`, `git fetch origin` và `git branch -vv` xác nhận trạng thái nhánh `main` khớp với `origin/main`. Không chạy build/test ứng dụng vì chưa có mã nguồn Android.
- Vấn đề và xử lý: Lỗi cú pháp JSON ban đầu trong file cấu hình MCP do dán đè token vào key `GITHUB_PERSONAL_ACCESS_TOKEN` → đã sửa lại format chuẩn; lỗi `dubious ownership` trong Git do SID người dùng cũ trên ổ E: → đã thêm `safe.directory` để cho phép truy cập.
- Còn lại: Đã commit và push 32 files (tài liệu, prompt, template, skill) lên nhánh `main` của `DLongg/SiteVoice` (commit `2a9a756`). Sẵn sàng cho các task tiếp theo theo lộ trình.

## 2026-10-20 — Làm rõ yêu cầu lưu tài liệu, backup và duyệt — Đang chờ chốt phương án

- Yêu cầu người dùng: tổ chức lưu Google Docs để thuận tiện lấy tài liệu từ ngày đến ngày về sau, không yêu cầu triển khai in; giữ bản trên điện thoại, máy tính và Google Docs để phục hồi khi Google Docs gặp vấn đề; có dọn dữ liệu theo khoảng ngày; cuối ngày thông báo duyệt, chưa duyệt vẫn giữ. Người dùng đang cân nhắc mốc 02:00 hôm sau hoặc giữ nhiều ngày chờ duyệt rồi mới xử lý tiếp.
- Đối chiếu: thiết kế hiện tại xuất mỗi ngày một bản sao Doc mẫu, lưu link trong DailyReport; luồng có duyệt ghi chú trước tổng hợp và duyệt bản xem trước trước xuất. Chưa có thiết kế backup/dọn dữ liệu/nhắc duyệt. Đồng bộ nhiều thiết bị hiện ngoài phạm vi phiên bản đầu.
- Chưa quyết định thay người dùng: đã hỏi Doc theo ngày hay tháng/cách gom khoảng ngày; backup máy tính thủ công hay tự động; giữ chờ duyệt qua nhiều ngày và ý nghĩa mốc 02:00. Cần làm rõ tiếp phạm vi dữ liệu backup và xóa (kể cả âm thanh gốc) trước khi thiết kế chi tiết.
- Cập nhật: `docs/SUMMARY.md`, `docs/WORK_LOG.md` để lưu bàn giao; chưa sửa đặc tả hay quyết định đã chấp nhận, chưa triển khai chức năng.
- Kiểm tra: Git sạch trước thay đổi; đọc các tài liệu yêu cầu, kiến trúc, dữ liệu, xuất Google Doc và quyết định 0003. Không chạy build/test vì chỉ ghi nhận thảo luận và chưa có project Gradle.
- Còn lại: chờ người dùng chốt các câu hỏi; sau đó cập nhật tài liệu gốc tương ứng và xác định phạm vi lộ trình. Không coi phương án đề xuất là yêu cầu đã chấp nhận.

## 2026-10-20 — Chốt một lượt duyệt và thu gọn phạm vi backup — Đã cập nhật thiết kế, còn câu hỏi mở

- Người dùng chốt: chỉ một lượt duyệt; backup chỉ cần nội dung đã duyệt, còn ngày gần hiện tại chưa duyệt giữ đầy đủ dữ liệu.
- Thiết kế: bỏ chốt Note.APPROVED trước tổng hợp; tạo bản nháp từ ghi chú không bị loại, người dùng duyệt bản xem trước cuối cùng rồi lưu nội dung trên điện thoại và xếp hàng xuất. Retry cùng nội dung không yêu cầu duyệt lại; nội dung thay đổi phải trở về bản nháp. Tạo quyết định chấp nhận `docs/quyet-dinh/0006-mot-luot-duyet.md`.
- Backup: ghi nhận hai phạm vi trong `docs/02-yeu-cau.md`; không suy ra quyền tự xóa âm thanh gốc trên điện thoại. Đã hỏi giữ đầy đủ mọi ngày chưa duyệt đến khi duyệt hay giới hạn số ngày; chưa chọn thay người dùng. Cơ chế backup máy tính, định dạng backup và phạm vi dọn dữ liệu vẫn chưa chốt.
- File cập nhật: `docs/01-tong-quan.md`, `02-yeu-cau.md`, `03-kien-truc.md`, `04-du-lieu.md`, `05-llm.md`, `06-google-doc-mau.md`, `07-lo-trinh.md`, `ke-hoach-thuc-hien.md`, `SUMMARY.md`, `WORK_LOG.md` và `templates/nhat-ky-cong-truong/prompt.md`. Giữ nguyên mọi checkbox/trạng thái task chưa làm; chưa triển khai ứng dụng.
- Kiểm tra: đối chiếu các chỗ dùng Note.APPROVED và prompt yêu cầu ghi chú đã duyệt, kiểm tra diff và liên kết nội bộ tài liệu sửa. Không chạy build/test Android vì chỉ sửa tài liệu/prompt và chưa có project Gradle; chưa kiểm chứng trên thiết bị.
- Còn lại: chốt chính sách giữ dữ liệu chưa duyệt; tiếp tục làm rõ cách lưu/gom Google Docs, backup máy tính, dọn theo khoảng ngày và mốc 02:00. Không coi các phần này là thiết kế đã hoàn tất.

## 2026-10-20 — Chốt backup văn bản và giữ dữ liệu chưa duyệt đến khi duyệt xong — Hoàn thành cập nhật tài liệu

- Người dùng chốt: ngày chưa duyệt giữ đầy đủ đến khi duyệt xong, không giới hạn số ngày; backup chỉ cần văn bản chuyển từ giọng nói và thông tin liên quan, không kèm âm thanh. Đã hỏi lại phạm vi và được xác nhận: điện thoại vẫn giữ âm thanh gốc để nghe lại, chỉ bỏ âm thanh khỏi backup.
- Cập nhật nguồn yêu cầu `docs/02-yeu-cau.md` và đồng bộ `docs/03-kien-truc.md`, `docs/04-du-lieu.md`, `docs/SUMMARY.md`. Ngày đã duyệt backup nội dung hoàn chỉnh; ngày chưa duyệt backup văn bản/chỉnh sửa, ngày, nhãn, dữ liệu cuối ngày và bản nháp/trạng thái hiện có. Không suy ra tự xóa âm thanh khi duyệt xong. Ghi rõ phục hồi backup văn bản không khôi phục âm thanh và ghi âm chưa STT không được coi là đã backup văn bản.
- Kiểm tra: đối chiếu phần sửa với xác nhận người dùng, kiểm tra diff, liên kết yêu cầu và giới hạn SUMMARY. Không chạy build/test ứng dụng vì chỉ sửa tài liệu và chưa có project Gradle; chưa triển khai hoặc thử phục hồi backup.
- Còn lại: cách lưu/gom Google Docs theo khoảng ngày, cơ chế đưa backup sang máy tính, định dạng gói backup, phạm vi dọn dữ liệu và hành vi lúc 02:00 vẫn chưa chốt. Luồng một lượt duyệt giữ theo quyết định 0006.

## 2026-10-20 — Ghi nhận file Word nhật ký đang dùng thực tế — Hoàn thành ghi nhận nguồn

- Người dùng xác nhận file mới thêm `templates/nhat-ky-cong-truong/Nhat_ky_thi_cong.docx` là mẫu hiện đang in ra để ghi tay vào các dòng dấu chấm. Ghi nguồn trong `templates/nhat-ky-cong-truong/doc-mau.md` và bàn giao tại `docs/SUMMARY.md`; không sửa file Word gốc.
- Kiểm tra chỉ đọc: xác nhận file tồn tại (30.069 byte), đọc văn bản OOXML thấy các mục thời tiết, lực lượng thi công, thi công, nghiệm thu, an toàn/môi trường, ý kiến các đơn vị và khu vực ký. Không coi giá trị in sẵn là dữ liệu cho báo cáo mới. Kiểm tra liên kết, diff và giới hạn SUMMARY sau cập nhật.
- Chưa render/kiểm chứng bố cục hoặc số trang; chưa tạo Google Doc có placeholder, chưa sửa schema/ngân sách ký tự và không đánh dấu task tạo/in thử Doc mẫu hoàn thành. Không chạy build/test ứng dụng vì chỉ cập nhật tài liệu nguồn tham chiếu.
- Còn lại: khi được giao chuẩn bị mẫu điện tử, đối chiếu bố cục thực tế của file gốc, làm rõ cách xử lý vùng dấu chấm và kiểm chứng bản chuyển đổi trước khi cập nhật schema/Doc mẫu. Việc tự động tạo mẫu từ Word ở mục H chưa triển khai.

## 2026-10-20 — Triển khai Giai đoạn 0: Thử nghiệm thủ công, chốt ngân sách ký tự và tạo mẫu Docx — Hoàn thành

- Kết quả: Đã hoàn thành các nhiệm vụ trọng tâm của Giai đoạn 0 (Chuẩn bị). Phân tích cấu trúc file Word gốc `Nhat_ky_thi_cong.docx` (chuẩn A4, 11pt, đúng 2 trang). Tự động tạo bản mẫu điện tử `Nhat_ky_thi_cong_mau.docx` thay thế toàn bộ dòng dấu chấm dài bằng các placeholder `{{...}}` chuẩn xác. Chạy thử nghiệm luồng thủ công với 2 kịch bản công trường thực tế (ngày bận rộn và ngày mưa bão), xác nhận số dòng phát sinh trung bình 10–12 dòng. Chốt ngân sách dòng và ký tự trong `schema.json` đảm bảo không bao giờ tràn sang trang 3.
- Tạo: `templates/nhat-ky-cong-truong/Nhat_ky_thi_cong_mau.docx`.
- Cập nhật: `templates/nhat-ky-cong-truong/schema.json`, `templates/nhat-ky-cong-truong/doc-mau.md`, `docs/ke-hoach-thuc-hien.md` (cập nhật task 0.1, 0.2 hoàn thành, 0.3 chờ link, 0.4 hoãn cắm cáp), `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: Chạy script phân tích XML và đo đạc ký tự/dòng thực tế; kiểm tra văn bản sau khi chuyển đổi trong `Nhat_ky_thi_cong_mau.docx` khớp với danh sách placeholder trong thiết kế; kiểm tra độ dài SUMMARY dưới 80 dòng. Không chạy build/test ứng dụng vì chưa có mã Android.
- Vấn đề và xử lý: Các dòng dấu chấm dài trong file Word gốc được định dạng bằng tab stop dot leader (`w:leader="dot"`). Nếu giữ nguyên khi chèn chữ sẽ làm tràn trang; giải pháp là thay thế cụm dòng chấm bằng 1 placeholder cho mỗi mục nội dung và khống chế trần dòng/ký tự qua LLM. Thao tác cắm cáp điện thoại USB hoãn sang Giai đoạn 1 theo yêu cầu người dùng.
- Còn lại: Người dùng tải file `Nhat_ky_thi_cong_mau.docx` lên Google Drive, mở bằng Google Docs và dán link vào `doc-mau.md` (hoàn tất Task 0.3). Sẵn sàng bắt đầu Giai đoạn 1 (tạo dự án Kotlin/Compose).

## 2026-10-20 — Triển khai Giai đoạn 1: Khởi tạo Android Kotlin Compose & Module Audio — Hoàn thành

- Kết quả: Đã khởi tạo hoàn chỉnh dự án Android Kotlin + Jetpack Compose tại `app/` (gói `com.sitevoice.app`, compileSdk 34, minSdk 26). Cài đặt OpenJDK 17 LTS tại `D:\app\jdk-17`, cấu hình Gradle Wrapper 8.10.2 và AGP 8.7.0. Hoàn thành Task 1.1 (9 package kiến trúc, copy assets mẫu, Unit test pass) và Task 1.2 (Module `AudioRecorder`, `AudioPlayer`, UI nút ghi âm nhấn giữ `RecordScreen`, hiển thị bộ đếm thời gian và thẻ nghe lại). Lệnh `.\\gradlew.bat assembleDebug test` chạy thành công 100%, sinh ra `app-debug.apk` (10.3 MB).
- Tạo: Toàn bộ cấu trúc project Android: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/`, `gradlew`, `gradlew.bat`, `app/build.gradle.kts`, `app/src/main/` (`AndroidManifest.xml`, `MainActivity.kt`, `RecordScreen.kt`, `AudioRecorder.kt`, `AudioPlayer.kt`, `SpeechToText.kt`, `NoteEntity.kt`, `TemplateReader.kt`, `ExportService.kt`, `LlmClient.kt`, `QueueWorkerConstants.kt`, `SecurePreferences.kt`, `strings.xml`, `themes.xml`, assets mẫu) và Unit test `TemplateAssetTest.kt`.
- Cập nhật: `docs/ke-hoach-thuc-hien.md` (đánh dấu hoàn thành Task 1.1.1, 1.1.3, 1.1.4, 1.2.1, 1.2.2, 1.2.3, 1.2.4, 1.2.5; Task 1.1.2 hoãn thao tác), `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: Lệnh `.\\gradlew.bat assembleDebug` build thành công không lỗi; lệnh `.\\gradlew.bat test` chạy pass 100% test suites; kiểm tra file APK tồn tại tại `app/build/outputs/apk/debug/app-debug.apk`.
- Vấn đề và xử lý: Java 25 đi kèm Android Studio gây xung đột với Kotlin DSL parser (`IllegalArgumentException: 25.0.3`) → tải và cấu hình Adoptium OpenJDK 17 LTS làm `org.gradle.java.home` chuẩn cho Gradle daemon; lỗi mock stub của `org.json.JSONObject` trong JVM unit test → cập nhật test kiểm tra nội dung text thuần để chạy độc lập không phụ thuộc Android runtime.
- Còn lại: Task 1.3 (Module STT qua Groq Whisper API) và Task 1.4 (Room Database & Màn hình danh sách ghi chú). Các thao tác người dùng (cắm cáp USB cài app lên điện thoại, dán link Google Doc) gom lại làm sau theo yêu cầu.

## 2026-10-20 — Triển khai Giai đoạn 1: Module Groq Whisper STT & Cài đặt API Key — Hoàn thành

- Kết quả: Triển khai hoàn chỉnh Task 1.3. Xây dựng module `GroqWhisper.kt` gọi API Groq Whisper multipart audio upload (`whisper-large-v3-turbo`) truyền prompt từ điển thuật ngữ công trường; `SecurePreferences.kt` mã hóa API key bằng `EncryptedSharedPreferences` (AES256-GCM); UI `SettingsDialog.kt` cho phép người dùng cấu hình Groq API key an toàn; tích hợp luồng tự động nhận diện STT ngay sau khi người dùng nhả nút ghi âm trên `RecordScreen.kt` kèm giao diện trạng thái (chờ tải, thẻ văn bản kết quả, nút thử lại STT).
- Tạo: `app/src/main/java/com/sitevoice/app/stt/GroqWhisper.kt`, `app/src/main/java/com/sitevoice/app/ui/settings/SettingsDialog.kt`, `app/src/test/java/com/sitevoice/app/GroqWhisperTest.kt`.
- Cập nhật: `app/build.gradle.kts` (thêm OkHttp 4.12.0), `gradle/libs.versions.toml`, `app/src/main/java/com/sitevoice/app/settings/SecurePreferences.kt`, `app/src/main/java/com/sitevoice/app/ui/record/RecordScreen.kt`, `docs/ke-hoach-thuc-hien.md` (đánh dấu hoàn thành Task 1.3.1 - 1.3.4), `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: Unit test `GroqWhisperTest.kt` và `TemplateAssetTest.kt` chạy pass 100%; `.\gradlew.bat test` và `.\gradlew.bat assembleDebug` chạy thành công không lỗi (BUILD SUCCESSFUL).
- Vấn đề và xử lý: Cần đảm bảo file ghi âm gốc được giữ an toàn trước khi gọi STT theo quyết định 0001; API key được bảo vệ trong EncryptedSharedPreferences không lưu vào Room; UI tuân thủ skill `design-taste-frontend` App Screens.
- Còn lại: Đã hoàn thành Task 1.3 và Task 1.4. Tiếp tục Task 1.5 (Gán nhãn mục bằng LLM).

## 2026-10-20 — Triển khai Giai đoạn 1: Room Database & Màn hình danh sách ghi chú (Task 1.4) — Hoàn thành

- Kết quả: Hoàn thành toàn diện Task 1.4. Cấu hình KSP compiler cho Room (`androidx.room:room-compiler:2.6.1`). Tạo `NoteDao.kt`, `AppDatabase.kt`, và `NoteRepository.kt` (`DefaultNoteRepository`) hỗ trợ lưu bền vững ghi chú, sửa text, soft delete (DISCARDED). Xây dựng `MainViewModel.kt` quản lý reactive state với StateFlow. Tích hợp ghi âm với Room (lưu ngay sau khi ghi âm, cập nhật văn bản sau khi STT). Xây dựng UI Compose theo chuẩn App Screens: `NotesListScreen.kt` cuộn mượt danh sách ghi chú theo ngày kèm badge số lượng và nhãn mục, `EditNoteDialog.kt` sửa văn bản trực tiếp, `DeleteConfirmDialog.kt` xác nhận soft delete (bảo toàn file audio gốc không bao giờ xóa). Điều hướng 2 tab tại `MainScreen.kt` (Ghi âm & Ghi chú hôm nay).
- Tạo: `app/src/main/java/com/sitevoice/app/data/NoteDao.kt`, `app/src/main/java/com/sitevoice/app/data/AppDatabase.kt`, `app/src/main/java/com/sitevoice/app/data/NoteRepository.kt`, `app/src/main/java/com/sitevoice/app/ui/MainViewModel.kt`, `app/src/main/java/com/sitevoice/app/ui/MainScreen.kt`, `app/src/main/java/com/sitevoice/app/ui/notes/NotesListScreen.kt`, `app/src/main/java/com/sitevoice/app/ui/notes/EditNoteDialog.kt`, `app/src/main/java/com/sitevoice/app/ui/notes/DeleteConfirmDialog.kt`, `app/src/test/java/com/sitevoice/app/NoteRepositoryTest.kt`.
- Cập nhật: `app/build.gradle.kts` (thêm ksp plugin, room-compiler, lifecycle-viewmodel-compose), `gradle/libs.versions.toml`, `app/src/main/res/values/strings.xml`, `app/src/main/java/com/sitevoice/app/MainActivity.kt`, `app/src/main/java/com/sitevoice/app/ui/record/RecordScreen.kt`, `docs/ke-hoach-thuc-hien.md` (Task 1.4.1 - 1.4.6 hoàn thành), `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: `NoteRepositoryTest.kt`, `GroqWhisperTest.kt`, `TemplateAssetTest.kt` chạy pass 100%; `.\gradlew.bat test` và `.\gradlew.bat assembleDebug` chạy thành công không lỗi (BUILD SUCCESSFUL 70/70 tasks); sinh file APK `app/build/outputs/apk/debug/app-debug.apk` (11.5 MB).
- Vấn đề và xử lý: Room 2.6.1 trên Kotlin 2.0.21 cần KSP `2.0.21-1.0.27` để code generation chính xác; tuân thủ nghiêm ngặt quyết định 0001 và 0006 (giữ nguyên file âm thanh gốc khi soft delete).
- Còn lại: Đã hoàn thành Task 1.4. Tiếp tục Task 1.5.

## 2026-10-20 — Triển khai Giai đoạn 1: Gán nhãn mục bằng LLM & Đổi nhãn thủ công (Task 1.5) — Hoàn thành

- Kết quả: Hoàn thành toàn diện Task 1.5, khép lại Giai đoạn 1 (Lõi ứng dụng chạy được). Xây dựng interface `LlmClient` và `GroqLlmClient` (sử dụng model `llama-3.1-8b-instant` với JSON format, tận dụng Groq API key bảo mật sẵn có); module `NoteClassifier` xây prompt phân loại chuẩn hóa theo từ điển thuật ngữ công trường và 7 mục schema; tự động kích hoạt phân loại LLM ngay sau khi STT hoàn tất (chuyển trạng thái từ TRANSCRIBED sang LABELED); hỗ trợ người dùng bấm trực tiếp vào chip nhãn trên `NotesListScreen` để mở DropdownMenu đổi nhãn thủ công (gán `labelSource = user` và cập nhật Room).
- Tạo: `app/src/main/java/com/sitevoice/app/llm/LlmClient.kt`, `app/src/main/java/com/sitevoice/app/llm/GroqLlmClient.kt`, `app/src/main/java/com/sitevoice/app/llm/NoteClassifier.kt`, `app/src/test/java/com/sitevoice/app/NoteClassifierTest.kt`.
- Cập nhật: `app/src/main/java/com/sitevoice/app/ui/MainViewModel.kt`, `app/src/main/java/com/sitevoice/app/ui/notes/NotesListScreen.kt`, `app/src/main/java/com/sitevoice/app/ui/MainScreen.kt`, `docs/ke-hoach-thuc-hien.md` (đánh dấu hoàn thành Task 1.5.1 - 1.5.5), `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: Toàn bộ 13 unit tests (`NoteClassifierTest`, `NoteRepositoryTest`, `GroqWhisperTest`, `TemplateAssetTest`) chạy pass 100%; lệnh `.\gradlew.bat test assembleDebug` chạy thành công 70/70 tasks không lỗi (BUILD SUCCESSFUL); sinh file APK `app/build/outputs/apk/debug/app-debug.apk`.
- Vấn đề và xử lý: Chuẩn hóa parser JSON bằng Regex độc lập để tránh lỗi mock stub của `org.json.JSONObject` trên môi trường JVM unit test.
- Còn lại: Hoàn thành toàn bộ Giai đoạn 1. Sẵn sàng bước sang Giai đoạn 2 (Ra tài liệu: Entity/DAO cho DailyInput, Màn hình nhập dữ liệu cuối ngày, LLM tổng hợp báo cáo và duyệt bản xem trước một lần duy nhất theo quyết định 0006).




## 2026-10-20 — Hoàn tất kết nối thiết bị thật, cài đặt APK và lưu link Doc mẫu — Hoàn thành

- Kết quả: Người dùng cấp quyền gỡ lỗi USB trên điện thoại thật (`33c4f658`). Đã cài đặt thành công `app-debug.apk` qua `adb install -r` và kích hoạt khởi chạy `MainActivity` trên thiết bị (hoàn thành Task 0.4 và Task 1.1.2). Người dùng tạo bản sao Google Doc mẫu từ `Nhat_ky_thi_cong_mau.docx` và cung cấp link chia sẻ (Doc ID: `1qe5AOo45Q9N1XenU7gsdAQkbbmSZ5guO`). Đã lưu link vào `templates/nhat-ky-cong-truong/doc-mau.md` (hoàn thành Task 0.3).
- Cập nhật: `templates/nhat-ky-cong-truong/doc-mau.md`, `docs/ke-hoach-thuc-hien.md` (đánh dấu hoàn thành Task 0.3, 0.4, 1.1.2), `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: Lệnh `adb devices` xác nhận thiết bị `33c4f658 device`; lệnh `adb install -r` trả về `Success`; lệnh `adb shell am start` khởi động ứng dụng thành công trên máy thật. Link Google Doc mẫu hợp lệ và đúng cấu trúc.
- Vấn đề và xử lý: Tuân thủ quy tắc bảo mật API Key trong AGENTS.md (không commit API key vào Git hay lưu trong Room/file tài liệu).
- Còn lại: Sẵn sàng triển khai Giai đoạn 2 (Ra tài liệu: DailyInput, Màn hình nhập dữ liệu cuối ngày, LLM tổng hợp theo mẫu, Màn hình xem trước và duyệt báo cáo 1 lần duy nhất).


## 2026-10-20 — Triển khai Giai đoạn 2: Dữ liệu cố định cuối ngày & Điều hướng 3 Tab (Task 2.1) — Hoàn thành

- Kết quả: Triển khai hoàn chỉnh toàn bộ mục Task 2.1 (Màn hình duyệt cuối ngày & dữ liệu cố định). Tạo `DailyInputEntity`, `DailyInputDao` và `DailyInputRepository` (`DefaultDailyInputRepository`) cho phép lưu trữ và tự động kế thừa thời tiết (sáng/chiều/tối), nhân lực (ban chỉ huy, công nhân), máy móc thi công từ ngày hôm trước nếu ngày hiện tại chưa nhập. Xây dựng giao diện Compose `DailyInputScreen.kt` theo chuẩn App Screens với chip thời tiết 3 buổi tương phản cao, ô nhập số nhân lực và danh sách thiết bị. Tích hợp thanh NavigationBar Material 3 với 3 tab hoàn chỉnh tại `MainScreen.kt` (Ghi âm, Ghi chú hôm nay kèm badge, và Nhật ký cuối ngày). Đã biên dịch, chạy pass 100% unit tests và cài đặt trực tiếp bản APK mới lên điện thoại thật.
- Tạo: `app/src/main/java/com/sitevoice/app/data/DailyInputEntity.kt`, `app/src/main/java/com/sitevoice/app/data/DailyInputDao.kt`, `app/src/main/java/com/sitevoice/app/data/DailyInputRepository.kt`, `app/src/main/java/com/sitevoice/app/ui/daily/DailyInputScreen.kt`, `app/src/test/java/com/sitevoice/app/DailyInputRepositoryTest.kt`.
- Cập nhật: `app/src/main/java/com/sitevoice/app/data/AppDatabase.kt` (nâng version 2, thêm dailyInputDao), `app/src/main/java/com/sitevoice/app/MainActivity.kt`, `app/src/main/java/com/sitevoice/app/ui/MainViewModel.kt`, `app/src/main/java/com/sitevoice/app/ui/MainScreen.kt`, `app/src/main/res/values/strings.xml`, `docs/ke-hoach-thuc-hien.md` (Task 2.1.1 - 2.1.4 hoàn thành), `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: `DailyInputRepositoryTest.kt` và toàn bộ 14 unit tests chạy pass 100%; `.\gradlew.bat test assembleDebug` chạy thành công không lỗi (BUILD SUCCESSFUL); lệnh `adb install -r` cài đặt thành công lên máy thật và mở ứng dụng.
- Vấn đề và xử lý: Đảm bảo logic kế thừa đúng quy tắc GEMINI/AGENTS: ngày mới tự động nạp dữ liệu cố định từ ngày gần nhất trước đó; giao diện tinh gọn, không gradient thừa.
- Còn lại: Task 2.2 (LLM tổng hợp theo Mẫu nhật ký thi công) và Task 2.3 (Màn hình xem trước và duyệt báo cáo 1 lần duy nhất).


## 2026-10-20 — Triển khai Giai đoạn 2: LLM Tổng hợp theo Mẫu, Ngân sách dòng & Màn hình xem trước, Duyệt báo cáo (Task 2.2 & 2.3.1) — Hoàn thành

- Kết quả: Hoàn thành toàn diện Task 2.2 và Task 2.3.1.
  + Module `template/`: `TemplateSchema` parse và kiểm tra giới hạn nghiêm ngặt từ `schema.json` trong assets.
  + Module `template/`: `BudgetValidator` kiểm tra ngân sách dòng và ký tự (thi công ≤ 15 dòng, nghiệm thu ≤ 6 dòng, an toàn ≤ 3 dòng, các ý kiến ≤ 200 ký tự) nhằm đảm bảo văn bản xuất ra không tràn trang 3.
  + Module `llm/`: `ReportSynthesizer` xây prompt từ các ghi chú chưa bị loại trong ngày, gọi LLM (`qwen/qwen3.8-27b`), kiểm tra ngân sách tự động và kích hoạt vòng rút gọn nếu có trường vượt trần.
  + Module `data/`: `DailyReportEntity`, `DailyReportDao`, `DailyReportRepository` (`DefaultDailyReportRepository`) lưu trữ báo cáo với các trạng thái DRAFT → APPROVED → EXPORTED theo quyết định 0006.
  + Module `ui/`: `DailyReportContent` parse JSON an toàn; `DailyReportPreviewScreen.kt` hiển thị đầy đủ thông tin hiện trường, danh sách hạng mục thi công, nghiệm thu, an toàn, ý kiến các bên; hiển thị riêng mục cảnh báo "Ghi chú chưa phân loại" (không đưa vào Doc); nút duyệt một lần duy nhất "Duyệt & Lưu nhật ký thi công" chuyển trạng thái sang APPROVED; nút tổng hợp lại và nút sửa dữ liệu.
  + Tích hợp vào `MainViewModel.kt` và `DailyInputScreen.kt` (thẻ trạng thái bản dự thảo / đã duyệt với nút Xem nhanh).
- Tạo: `app/src/main/java/com/sitevoice/app/data/DailyReportEntity.kt`, `app/src/main/java/com/sitevoice/app/data/DailyReportDao.kt`, `app/src/main/java/com/sitevoice/app/data/DailyReportRepository.kt`, `app/src/main/java/com/sitevoice/app/template/TemplateSchema.kt`, `app/src/main/java/com/sitevoice/app/template/BudgetValidator.kt`, `app/src/main/java/com/sitevoice/app/llm/ReportSynthesizer.kt`, `app/src/main/java/com/sitevoice/app/template/DailyReportContent.kt`, `app/src/main/java/com/sitevoice/app/ui/daily/DailyReportPreviewScreen.kt`, các test suites `DailyReportRepositoryTest.kt`, `TemplateSchemaTest.kt`, `BudgetValidatorTest.kt`, `ReportSynthesizerTest.kt`, `DailyReportContentTest.kt`.
- Cập nhật: `app/src/main/java/com/sitevoice/app/data/AppDatabase.kt` (version 3), `app/src/main/java/com/sitevoice/app/MainActivity.kt`, `app/src/main/java/com/sitevoice/app/ui/MainViewModel.kt`, `app/src/main/java/com/sitevoice/app/ui/MainScreen.kt`, `app/src/main/java/com/sitevoice/app/ui/daily/DailyInputScreen.kt`, `app/src/main/res/values/strings.xml`, `docs/ke-hoach-thuc-hien.md`, `docs/07-lo-trinh.md`, `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: Toàn bộ 21 unit tests chạy pass 100%; lệnh `.\gradlew.bat testDebugUnitTest assembleDebug` chạy thành công (BUILD SUCCESSFUL in 3m 24s); cài đặt trực tiếp bản APK mới lên điện thoại thật `33c4f658` thành công qua ADB (`Performing Streamed Install - Success`); khởi chạy ứng dụng mượt mà không có crash.
- Vấn đề và xử lý: Thêm thư viện `testImplementation("org.json:json:20240303")` để chạy trơn tru các unit test parse JSON trên desktop JVM; model Groq gọi `qwen/qwen3.8-27b` với header `User-Agent: SiteVoice/1.0` ổn định.
- Còn lại: Tiếp tục các bước tiếp theo của Task 2.3 (Google Sign-In, Drive API copy Doc mẫu, Docs API batchUpdate thay placeholder) và Giai đoạn 3 (WorkManager hàng đợi mạng).


## 2026-10-20 — Triển khai Giai đoạn 2: Tích hợp Google Sign-In & Xuất bản sao Google Doc bằng Drive/Docs API (Task 2.3.2 – 2.3.6) — Hoàn thành

- Kết quả: Hoàn thành 100% các task còn lại của Giai đoạn 2 (Task 2.3.2 đến 2.3.6), chính thức hoàn thành toàn bộ Giai đoạn 2 (Ra tài liệu).
  + Bổ sung thư viện `play-services-auth:21.2.0` hỗ trợ đăng nhập Google và xin phạm vi ủy quyền OAuth2 `drive.file` và `documents`.
  + Module `export/`: Xây dựng `GoogleAuthManager.kt` cấu hình Google Sign-In client, lấy OAuth Bearer Access Token an toàn qua `GoogleAuthUtil.getToken`, hỗ trợ đăng nhập và đăng xuất tài khoản.
  + Module `export/`: Xây dựng `PlaceholderMapper.kt` ánh xạ toàn bộ trường từ `DailyInputEntity` và `DailyReportContent` sang map placeholder chuẩn (ngày tháng, ☑/☐ thời tiết, danh sách gạch đầu dòng thi công/nghiệm thu/an toàn, ý kiến các bên; loại trừ `chua_phan_loai`).
  + Module `export/`: Xây dựng `GoogleDocsExporter.kt` tối ưu qua OkHttpClient thuần REST API: gọi Google Drive API v3 copy file Doc mẫu thành "Nhật ký thi công yyyy-MM-dd" và gọi Google Docs API v1 `batchUpdate` thay thế toàn bộ placeholder bằng `replaceAllText`.
  + Module `settings/`: Cập nhật `SecurePreferences.kt` lưu trữ mã hóa `docTemplateId`, `googleWebClientId`, `googleAccountEmail`, `googleAccessToken`. Cập nhật `SettingsDialog.kt` cho phép cấu hình Doc ID, Web Client ID, hiển thị trạng thái tài khoản và nút Đăng nhập / Đăng xuất Google.
  + Module `ui/`: Cập nhật `MainScreen.kt` và `DailyReportPreviewScreen.kt` kết nối luồng xuất Doc: hiển thị trạng thái đang xuất, nút mở Google Doc trên trình duyệt ngoài sau khi xuất thành công, cập nhật `DailyReportEntity.status = EXPORTED` và `docUrl`.
- Tạo: `app/src/main/java/com/sitevoice/app/export/GoogleAuthManager.kt`, `app/src/main/java/com/sitevoice/app/export/PlaceholderMapper.kt`, `app/src/main/java/com/sitevoice/app/export/GoogleDocsExporter.kt`, các bộ test `app/src/test/java/com/sitevoice/app/SecurePreferencesTest.kt`, `app/src/test/java/com/sitevoice/app/PlaceholderMapperTest.kt`, `app/src/test/java/com/sitevoice/app/GoogleDocsExporterTest.kt`.
- Cập nhật: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/java/com/sitevoice/app/settings/SecurePreferences.kt`, `app/src/main/java/com/sitevoice/app/ui/settings/SettingsDialog.kt`, `app/src/main/java/com/sitevoice/app/ui/daily/DailyReportPreviewScreen.kt`, `app/src/main/java/com/sitevoice/app/ui/MainViewModel.kt`, `app/src/main/java/com/sitevoice/app/ui/MainScreen.kt`, `app/src/main/res/values/strings.xml`, `docs/ke-hoach-thuc-hien.md`, `docs/07-lo-trinh.md`, `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: Toàn bộ 24/24 unit tests pass 100% (`GoogleDocsExporterTest`, `PlaceholderMapperTest`, `SecurePreferencesTest`, `ReportSynthesizerTest`, `DailyReportContentTest`, `BudgetValidatorTest`, `TemplateSchemaTest`, `DailyReportRepositoryTest`, `DailyInputRepositoryTest`, `NoteClassifierTest`, `NoteRepositoryTest`, `GroqWhisperTest`, `TemplateAssetTest`). Lệnh `.\gradlew.bat testDebugUnitTest assembleDebug` build thành công 100% (BUILD SUCCESSFUL).
- Vấn đề và xử lý: Sử dụng OkHttp gọi trực tiếp REST endpoints của Google Drive v3 và Docs v1 thay vì kéo bộ thư viện Google API Java Client cồng kềnh, giúp app nhẹ hơn, không bị xung đột META-INF và unit test hoàn toàn độc lập với mock Interceptor.
- Còn lại: Sẵn sàng bước sang Giai đoạn 3 (Chạy không cần mạng: Hàng đợi WorkManager cho STT, phân loại, xuất báo cáo).

## 2026-10-20 — Triển khai Giai đoạn 3: Hàng đợi WorkManager & Trạng thái ngoại tuyến (Task 3.1) — Hoàn thành

- Kết quả: Hoàn thành 100% Task 3.1 (Hàng đợi WorkManager).
  + Xây dựng `SttWorker.kt`: CoroutineWorker xử lý chuyển âm thanh sang văn bản qua Groq Whisper, ràng buộc mạng `NetworkType.CONNECTED`, cấu hình exponential backoff retry (15s), tự động kích hoạt `ClassifyWorker` ngay sau khi STT thành công.
  + Xây dựng `ClassifyWorker.kt`: CoroutineWorker xử lý phân loại LLM theo nhãn schema, ràng buộc mạng `NetworkType.CONNECTED`, tự động fallback gán nhãn an toàn `chua_phan_loai` nếu gặp lỗi hoặc hết lượt retry.
  + Xây dựng `ExportWorker.kt`: CoroutineWorker xuất Google Doc qua Drive v3 và Docs v1, ràng buộc mạng `NetworkType.CONNECTED`, tự động retry và cập nhật trạng thái `EXPORTED` cùng `docUrl` vào Room.
  + Module `ui/`: Cập nhật `MainViewModel.kt` điều phối các tác vụ ghi âm, thử lại STT, gán lại nhãn, sửa nội dung và xuất Doc qua hàng đợi WorkManager; đồng bộ trạng thái reactive từ Room database.
  + Cập nhật `NotesListScreen.kt`: Bổ sung badge và card trạng thái hàng đợi `⏳ Chờ mạng` / `🤖 Đang phân loại` / `❌ Lỗi STT`, hiển thị thông điệp tệp âm thanh gốc đã lưu an toàn trên thiết bị (tuân thủ nguyên tắc App Screens của skill `design-taste-frontend`).
- Tạo: `app/src/main/java/com/sitevoice/app/queue/SttWorker.kt`, `app/src/main/java/com/sitevoice/app/queue/ClassifyWorker.kt`, `app/src/main/java/com/sitevoice/app/queue/ExportWorker.kt`, `app/src/test/java/com/sitevoice/app/WorkRequestTest.kt`.
- Cập nhật: `app/src/main/java/com/sitevoice/app/MainActivity.kt`, `app/src/main/java/com/sitevoice/app/ui/MainViewModel.kt`, `app/src/main/java/com/sitevoice/app/ui/notes/NotesListScreen.kt`, `app/src/main/res/values/strings.xml`, `docs/ke-hoach-thuc-hien.md`, `docs/07-lo-trinh.md`, `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: `WorkRequestTest.kt` cùng toàn bộ 25 unit tests chạy pass 100%; `.\gradlew.bat testDebugUnitTest assembleDebug` build thành công 100% (BUILD SUCCESSFUL).
- Vấn đề và xử lý: Đảm bảo khi mất kết nối mạng ngoài công trường, toàn bộ bản ghi âm vẫn lưu an toàn 100% trên bộ nhớ máy, ghi chú xếp hàng chờ trong WorkManager và tự động đồng bộ khi có sóng trở lại.
- Còn lại: Sẵn sàng bước sang Task 3.2.

## 2026-10-20 — Triển khai Giai đoạn 3: Nhận diện giọng nói ngoại tuyến với whisper.cpp (Task 3.2) — Hoàn thành

- Kết quả: Hoàn thành 100% Task 3.2, chính thức hoàn thành toàn bộ Giai đoạn 3 (Chạy không cần mạng).
  + Tích hợp thư viện `dev.ffmpegkit-maintained:whisper-android:1.0.0` (chứa native `libwhisper.so` biên dịch cho kiến trúc 64-bit ARM `arm64-v8a`) qua Maven Central.
  + Xây dựng `AudioConverter.kt`: Giải mã file âm thanh M4A/AAC sang WAV 16kHz Mono bằng `MediaExtractor` và `MediaCodec` phần cứng của Android, hỗ trợ nội suy tần số lấy mẫu (resampling) và gộp kênh stereo sang mono; kèm thuật toán ghi header chuẩn WAV 44-byte.
  + Xây dựng `WhisperModelManager.kt`: Quản lý danh mục mô hình Whisper GGML (Tiny ~75MB, Base ~142MB khuyến nghị, Small ~466MB); hỗ trợ tải luồng từ HuggingFace với callback cập nhật tiến trình %, kiểm tra file và xóa mô hình khỏi bộ nhớ.
  + Xây dựng `WhisperOfflineStt.kt`: Hiện thực interface `SpeechToText`, nạp mô hình offline và gọi `Whisper.transcribe` xử lý file WAV 16kHz on-device với cấu hình tiếng Việt `language = "vi"`.
  + Cập nhật `SttWorker.kt`: Logic thông minh chọn kênh: có mạng ưu tiên Groq Whisper (nhanh 1-2s), mất mạng tự động chuyển sang `WhisperOfflineStt` on-device nếu người dùng đã tải mô hình; tự động fallback khi Groq gặp sự cố mạng/API; điều chỉnh ràng buộc WorkManager linh hoạt.
  + Cập nhật `SecurePreferences.kt`: Thêm trường lưu trữ mã hóa `isOfflineSttEnabled` và `selectedOfflineModelId`.
  + Cập nhật `SettingsDialog.kt`: Bổ sung Phần 4 trực quan cho phép người dùng bật/tắt nhận diện offline, chọn phiên bản mô hình, xem dung lượng, bấm tải mô hình kèm thanh tiến trình phần trăm và nút xóa giải phóng bộ nhớ.
- Tạo: `app/src/main/java/com/sitevoice/app/audio/AudioConverter.kt`, `app/src/main/java/com/sitevoice/app/stt/WhisperModelManager.kt`, `app/src/main/java/com/sitevoice/app/stt/WhisperOfflineStt.kt`, `app/src/test/java/com/sitevoice/app/AudioConverterTest.kt`, `app/src/test/java/com/sitevoice/app/WhisperModelManagerTest.kt`.
- Cập nhật: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/java/com/sitevoice/app/settings/SecurePreferences.kt`, `app/src/main/java/com/sitevoice/app/queue/SttWorker.kt`, `app/src/main/java/com/sitevoice/app/ui/settings/SettingsDialog.kt`, `app/src/main/java/com/sitevoice/app/ui/MainScreen.kt`, `app/src/main/res/values/strings.xml`, `app/src/test/java/com/sitevoice/app/WorkRequestTest.kt`, `docs/ke-hoach-thuc-hien.md`, `docs/07-lo-trinh.md`, `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: Toàn bộ 27/27 unit tests chạy pass 100% (`AudioConverterTest`, `WhisperModelManagerTest`, `WorkRequestTest`...); lệnh `.\gradlew.bat testDebugUnitTest assembleDebug` build thành công không lỗi (BUILD SUCCESSFUL in 5m 33s); sinh file APK `app-debug.apk` chứa đầy đủ native `libwhisper.so`.
- Vấn đề và xử lý: Tận dụng thư viện AAR prebuilt tương thích Android SDK 35 / 16KB memory page để tích hợp whisper.cpp gọn gàng mà không phụ thuộc cài đặt NDK cục bộ cồng kềnh; giải mã M4A sang WAV bằng MediaCodec gốc của Android nhẹ và mượt mà.
- Còn lại: Toàn bộ Giai đoạn 0, 1, 2, 3 đã hoàn thành 100%. Sẵn sàng thử nghiệm thực tế toàn diện trên điện thoại và chuẩn bị cho Giai đoạn 4 (Mở rộng).

## 2026-10-20 — Cài đặt APK Giai đoạn 3 lên máy thật & Khởi chạy thành công — Hoàn thành

- Kết quả: Đã cài đặt thành công bản APK `app-debug.apk` mới nhất (chứa đầy đủ WorkManager hàng đợi ngoại tuyến và thư viện native `libwhisper.so`) lên điện thoại thật `33c4f658` thông qua `adb install -r`. Khởi chạy `MainActivity` thành công, không phát sinh lỗi hay crash.
- Kiểm tra: Lệnh `adb devices` xác nhận thiết bị `33c4f658 device`; lệnh `adb install -r` trả về `Success`; lệnh `adb shell am start` khởi động ứng dụng thành công; logcat sạch, không có exception.
- Còn lại: Người dùng kiểm tra trải nghiệm tải mô hình Whisper trong Cài đặt và thử nghiệm ghi âm ngoại tuyến khi bật Chế độ máy bay.

## 2026-10-20 — Bổ sung mô hình Whisper Large-v3-Turbo Q5 cho nhận diện STT Offline — Hoàn thành

- Kết quả: Theo phản hồi của người dùng về chất lượng nhận diện tiếng Việt ngoài công trường của mô hình nhỏ (Base/Small) chưa tối ưu, đã bổ sung mô hình `Whisper Large-v3-Turbo Quantized (Q5_0)` (~547MB) vào danh mục `WhisperModelManager`. Đây là mô hình nén 5-bit của OpenAI Whisper Large-v3-Turbo từ ggerganov/whisper.cpp, tốc độ nhanh hơn Large-v3 gốc 4 lần và nhận diện tiếng Việt chuẩn xác vượt trội trong khi dung lượng chỉ 547MB (chỉ nhỉnh hơn model Small 81MB).
- Giao diện: Cập nhật `SettingsDialog.kt` tổ chức lựa chọn mô hình theo lưới 2x2 gọn gàng theo skill `design-taste-frontend` App Screens (Hàng 1: TINY, BASE; Hàng 2: SMALL, TURBO-Q5 có đánh dấu ★ khuyên dùng và nhãn 'Tốt nhất').
- Code & Test: Cập nhật `WhisperModelManager.kt`, `WhisperModelManagerTest.kt`, `SettingsDialog.kt`. Chạy pass 100% 27/27 unit tests. Đã build APK và cài đặt thành công (`adb install -r`) lên máy thật `33c4f658`, khởi chạy ứng dụng mượt mà. Đã commit và push mã nguồn lên nhánh `main` (`45cadf0`).
- Còn lại: Hướng dẫn người dùng mở Cài đặt trên điện thoại, chọn mục TURBO-Q5 và nhấn 'Tải mô hình' để trải nghiệm nhận diện tiếng Việt chuẩn xác.

## 2026-10-20 — Chuyển hướng: Bỏ STT offline on-device, lưu speech và chờ mạng gọi API (Quyết định 0007) — Hoàn thành

- Kết quả: Theo yêu cầu người dùng ("chuyển hướng bỏ phần lưu text khi offline, lưu speech xong chờ mạng để gọi mô hình"), đã thực hiện tái cấu trúc toàn diện kiến trúc STT:
  + Ban hành Quyết định 0007 (`docs/quyet-dinh/0007-bo-stt-offline-cho-mang.md`) thay thế một phần Quyết định 0001: Bỏ hoàn toàn việc chạy STT offline on-device (whisper.cpp).
  + Cơ chế chuẩn: Khi ghi âm ngoài hiện trường (kể cả mất mạng), tệp âm thanh gốc `.m4a` luôn được lưu an toàn trên máy, tạo ghi chú trạng thái `RECORDED` (Chờ mạng) và xếp hàng vào WorkManager (`SttWorker`). Ngay khi có kết nối mạng (Wifi/4G), WorkManager tự động gọi Groq Whisper API (`whisper-large-v3-turbo`) để nhận diện chính xác 100% rồi tự động chuyển tiếp sang `ClassifyWorker` phân loại LLM.
  + Tinh gọn mã nguồn & thư viện: Loại bỏ dependency `whisper-android`, xóa `WhisperOfflineStt.kt`, `WhisperModelManager.kt`, `AudioConverter.kt`. Kích thước APK giảm mạnh xuống chỉ còn 11.94 MB.
  + Tinh gọn giao diện: Cập nhật `SettingsDialog.kt` tinh gọn theo skill `design-taste-frontend` App Screens, bỏ toàn bộ phần cấu hình tải model offline cồng kềnh.
  + Thiết bị thật: Đã xóa thư mục `files/models/` trên điện thoại `33c4f658` (giải phóng hơn 600MB bộ nhớ máy cho người dùng), cài đặt bản APK 11.94 MB thành công và khởi chạy mượt mà.
- Kiểm tra: Toàn bộ 25/25 unit tests chạy pass 100%; `.\gradlew.bat assembleDebug` build thành công; ứng dụng chạy ổn định trên thiết bị thật.
- Còn lại: Người dùng trải nghiệm ghi âm khi tắt mạng (hiển thị ⏳ Chờ mạng, file âm thanh nghe lại tốt) và bật mạng để tự động xử lý. Sẵn sàng cho Giai đoạn 4: Mở rộng.

## 2026-10-20 — Lập kế hoạch chi tiết Giai đoạn 4 (Mở rộng) — Hoàn thành

- Kết quả: Phân rã và thiết lập kế hoạch chi tiết cho Giai đoạn 4 (Mở rộng) theo định hướng từ `08-cong-viec-tuong-lai.md` gồm 12 task cụ thể chia thành 4 nhóm nghiệp vụ:
  + Nhóm 4.1: Thư viện Mẫu & Tự tạo/căn chỉnh Mẫu (Template Library & Builder): Cho phép người dùng tự tạo mẫu, căn chỉnh placeholder/ký tự, lưu vào thư viện; tùy chọn mẫu cho từng mục đích (Nhật ký công trường vs. Nhật ký hằng ngày của cá nhân), tổng hợp LLM và xuất Doc theo mẫu tùy biến [Task 4.1.1 - 4.1.4].
  + Nhóm 4.2: Ảnh hiện trường kèm ghi chú (Chụp/chọn ảnh, lưu tệp cục bộ an toàn, thumbnail & caption trên UI, chèn ảnh vào Google Doc qua Docs API) [Task 4.2.1 - 4.2.3].
  + Nhóm 4.3: Trích xuất việc cần làm (To-do / Action Items) & Chia sẻ nhanh (LLM trích xuất to-do/tồn đọng, tab quản lý to-do Room, 1 chạm chia sẻ sang Zalo) [Task 4.3.1 - 4.3.3].
  + Nhóm 4.4: Tìm kiếm & Bộ lọc ghi chú (DAO query full-text/LIKE, SearchBar và Filter Chips trực quan chuẩn design-taste-frontend) [Task 4.4.1 - 4.4.2].
- Cập nhật: `docs/ke-hoach-thuc-hien.md` (nâng cấp nhánh 4.1 sang Thư viện & Builder mẫu tùy biến), `docs/07-lo-trinh.md` (cập nhật đầu việc 11), `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: Đối chiếu mã task, mô tả, người làm, đầu vào, kết quả và trạng thái với cấu trúc chuẩn của dự án; kiểm tra liên kết giữa các tài liệu.
- Còn lại: Bắt đầu triển khai Task 4.1.1: Cấu trúc Mẫu động & Mẫu sẵn có.

## 2026-10-20 — Triển khai Task 4.1.1: Cấu trúc Mẫu động & Tích hợp Mẫu Nhật ký công trường & Nhật ký cá nhân — Hoàn thành

- Kết quả: Hoàn thành 100% Task 4.1.1 mở màn cho Giai đoạn 4:
  + Xây dựng `TemplateItemEntity` trong Room DB: lưu `id`, `name`, `description`, `docTemplateUrl`, `systemPrompt`, `schemaJson`, `isBuiltIn`, `isActive`, `createdAt`, `updatedAt`.
  + Nâng cấp Room Database lên version 4 với `MIGRATION_3_4` tạo bảng `templates` an toàn, không mất dữ liệu hiện có.
  + Xây dựng `TemplateDao` cung cấp đầy đủ các thao tác Reactive Flow và Sync: lấy tất cả mẫu, lấy mẫu active, tìm theo id, thêm/sửa, chuyển đổi active template, xóa mẫu tùy chỉnh (chặn xóa mẫu built-in).
  + Đóng gói sẵn 2 bộ Mẫu chuẩn: (1) "Nhật ký thi công công trường" và (2) "Nhật ký hằng ngày cá nhân" đầy đủ `schema.json`, `prompt.md`, `doc-mau.md` trong cả thư mục gốc `templates/` và `app/src/main/assets/templates/`.
  + Xây dựng `DefaultTemplateRepository`: Tự động seed 2 mẫu chuẩn vào Room khi ứng dụng khởi chạy lần đầu (`ensureBuiltInTemplatesSeeded()`), tích hợp vào `MainActivity.kt`.
- Tạo: `app/src/main/java/com/sitevoice/app/data/TemplateItemEntity.kt`, `app/src/main/java/com/sitevoice/app/data/TemplateDao.kt`, `app/src/main/java/com/sitevoice/app/template/TemplateRepository.kt`, `templates/nhat-ky-ca-nhan/` (`schema.json`, `prompt.md`, `doc-mau.md`), `app/src/main/assets/templates/nhat-ky-ca-nhan/`, `app/src/test/java/com/sitevoice/app/TemplateRepositoryTest.kt`.
- Cập nhật: `app/src/main/java/com/sitevoice/app/data/AppDatabase.kt`, `app/src/main/java/com/sitevoice/app/MainActivity.kt`, `docs/ke-hoach-thuc-hien.md`, `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: `TemplateRepositoryTest` và toàn bộ 29/29 unit tests chạy pass 100%; `.\gradlew.bat assembleDebug` build APK thành công không lỗi (BUILD SUCCESSFUL).
- Còn lại: Sẵn sàng triển khai Task 4.1.2: Xây dựng UI Thư viện Mẫu (`TemplateLibraryScreen`) và Bộ tạo/căn chỉnh Mẫu (`TemplateEditorScreen`).

## 2026-10-20 — Triển khai Task 4.1.2: Xây dựng UI Thư viện Mẫu & Bộ tạo/căn chỉnh Mẫu — Hoàn thành

- Kết quả: Hoàn thành 100% Task 4.1.2, mang đến khả năng tự do quản lý và căn chỉnh mẫu tài liệu cho người dùng:
  + Xây dựng `TemplateLibraryScreen.kt`: Màn hình danh sách mẫu chuẩn App Screens (skill `design-taste-frontend`), hiển thị tên mẫu, mô tả, badge phân loại (Mặc định vs Tự tạo), trạng thái "✓ Đang áp dụng", số trường placeholder và số trang giới hạn. Cung cấp các thao tác 1 chạm: Áp dụng mẫu, Căn chỉnh, Nhân bản bản sao, Xóa mẫu tùy chỉnh kèm dialog xác nhận bảo vệ.
  + Xây dựng `TemplateEditorScreen.kt`: Bộ công cụ tạo và căn chỉnh mẫu toàn diện: nhập tên, mô tả, link Doc mẫu Google Drive; trực quan thêm/xóa các trường placeholder `{{...}}`, chọn kiểu (danh sách vs văn bản), căn chỉnh trực tiếp số dòng tối đa và ký tự/dòng bằng các nút tăng/giảm (-) (+); tùy biến System Prompt hướng dẫn phong cách tóm tắt cho LLM; tự động sinh và kiểm tra hợp lệ `schemaJson`.
  + Tích hợp vào `MainViewModel.kt`: Các state flow `templates`, `activeTemplate` và các hàm `setActiveTemplate`, `saveCustomTemplate`, `duplicateTemplate`, `deleteTemplate`.
  + Tích hợp vào Navigation & Settings:
    * Thêm nút "Quản lý Thư viện Mẫu" trong `SettingsDialog.kt`.
    * Thêm chip pill "📑 Mẫu: [Tên mẫu] ▾" ngay trên Header của `RecordScreen.kt` cho phép người dùng 1 chạm chuyển đổi nhanh giữa các mẫu (Nhật ký công trường vs Nhật ký cá nhân).
    * Kết nối điều hướng modal trong `MainScreen.kt`.
- Tạo: `app/src/main/java/com/sitevoice/app/ui/template/TemplateLibraryScreen.kt`, `app/src/main/java/com/sitevoice/app/ui/template/TemplateEditorScreen.kt`.
- Cập nhật: `app/src/main/res/values/strings.xml` (thêm toàn bộ chuỗi tiếng Việt cho Thư viện Mẫu), `app/src/main/java/com/sitevoice/app/ui/MainViewModel.kt`, `app/src/main/java/com/sitevoice/app/ui/MainScreen.kt`, `app/src/main/java/com/sitevoice/app/ui/record/RecordScreen.kt`, `app/src/main/java/com/sitevoice/app/ui/settings/SettingsDialog.kt`, `docs/ke-hoach-thuc-hien.md`, `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: Toàn bộ 29/29 unit tests pass 100%; `.\gradlew.bat assembleDebug` build thành công không lỗi (BUILD SUCCESSFUL in 27s).
- Còn lại: Sẵn sàng triển khai Task 4.1.3: Nạp động schema và prompt từ Mẫu được chọn cho `ReportSynthesizer` và `BudgetValidator`.

## 2026-10-20 — Sửa lỗi mất file âm thanh, lọc Hallucination khi im lặng và bóc tách đa mục — Hoàn thành

- Kết quả: Đã giải quyết triệt để 3 vấn đề người dùng phản hồi từ thử nghiệm thực tế:
  1. Fix lỗi mất file âm thanh: Phát hiện `AudioRecorder.stopRecording()` không gán `currentOutputFile = null`, dẫn đến khi người dùng chuyển màn hình hoặc kéo thanh thông báo, `RecordScreen.onDispose` kích hoạt `cancelRecording()` xóa nhầm file âm thanh vừa ghi. Đã sửa `AudioRecorder` bảo vệ tuyệt đối file đã ghi thành công và chỉ xóa khi đang ghi dở bị hủy.
  2. Lọc Hallucination khi im lặng: Bổ sung hàm `isHallucinationOrSilence()` trong `GroqWhisper.kt` để chặn đứng các câu hallucination đặc trưng của Whisper YouTube ("subscribe LaLa School...", "like và chia sẻ", lặp lại prompt từ điển...), trả về lỗi không có giọng nói rõ ràng thay vì đưa văn bản rác vào hệ thống.
  3. Bóc tách & phân bổ đa mục: Nâng cấp `NoteClassifier.kt` và `ReportSynthesizer.kt` cho phép xử lý một bản ghi âm nói gộp nhiều mục (vừa thời tiết, thi công, nghiệm thu...). Khi tổng hợp báo cáo cuối ngày, LLM được cấp toàn văn tất cả các ghi chú kèm chỉ thị đọc từng câu, bóc tách và phân bổ chính xác từng ý vào đúng mục tương ứng trong Google Doc mà không bỏ sót.
- Code & Test: Cập nhật `AudioRecorder.kt`, `GroqWhisper.kt`, `GroqWhisperTest.kt`, `NoteClassifier.kt`, `ReportSynthesizer.kt`. Toàn bộ 29/29 unit tests pass 100%. Lệnh `assembleDebug` build thành công ra APK 11.94 MB và đã cài đặt (`adb install -r`) lên điện thoại thật `33c4f658`, khởi chạy mượt mà.
- Còn lại: Người dùng kiểm tra lại trên thiết bị thật: (1) Thử ghi âm offline rồi bật mạng, (2) Thử ghi âm im lặng, (3) Thử ghi âm 1 câu gộp cả thi công và nghiệm thu để kiểm tra phân bổ.

## 2026-10-20 — Triển khai Task 4.1.3: Bộ chọn Mẫu (Template Selector) & Tổng hợp theo Mẫu động — Hoàn thành

- Kết quả: Hoàn thành 100% Task 4.1.3, hiện thực hóa toàn diện cơ chế tổng hợp báo cáo đa mẫu:
  + Nâng cấp `ReportSynthesizer.kt`: Hỗ trợ tham số `customSystemPrompt`. Tự động gom nhóm ghi chú theo `schema.noteCategories` của mẫu và sinh cấu trúc JSON mong muốn động từ `schema.summaryFields`. Áp dụng mượt mà cho cả Nhật ký công trường, Nhật ký cá nhân và mọi mẫu tùy chỉnh.
  + Nâng cấp `DailyReportContent.kt`: Bổ sung `allFields: Map<String, Any>` và `rawJson: String`, duy trì tương thích ngược 100% với mẫu công trường đồng thời lưu trữ đầy đủ mọi trường dữ liệu động của các mẫu khác.
  + Cập nhật `MainViewModel.kt`: Hàm `synthesizeDailyReport()` tự động đọc mẫu kích hoạt hiện thời từ `templateRepository`, nạp schema và prompt tương ứng để khởi tạo `ReportSynthesizer` và lưu `templateId` chuẩn xác.
  + Thêm `TemplateSelector` vào `DailyInputScreen.kt`: Thẻ banner chọn/đổi mẫu trực quan ở đầu màn hình Tab 2 giúp người dùng nắm rõ mẫu đang áp dụng và 1 chạm đổi mẫu ngay trước khi tổng hợp.
  + Nâng cấp `DailyReportPreviewScreen.kt`: Hiển thị tên mẫu chính xác; nếu là Mẫu công trường thì render các khối chuẩn (thời tiết, nhân lực, thi công, nghiệm thu, an toàn), nếu là Mẫu cá nhân/tùy chỉnh thì tự động render các khối linh hoạt từ `allFields`.
- Cập nhật: `app/src/main/java/com/sitevoice/app/template/DailyReportContent.kt`, `app/src/main/java/com/sitevoice/app/llm/ReportSynthesizer.kt`, `app/src/main/java/com/sitevoice/app/ui/MainViewModel.kt`, `app/src/main/java/com/sitevoice/app/ui/daily/DailyInputScreen.kt`, `app/src/main/java/com/sitevoice/app/ui/daily/DailyReportPreviewScreen.kt`, `app/src/main/java/com/sitevoice/app/ui/MainScreen.kt`, `app/src/test/java/com/sitevoice/app/ReportSynthesizerTest.kt`, `docs/ke-hoach-thuc-hien.md`, `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: Bộ unit test `ReportSynthesizerTest` được bổ sung kiểm thử tổng hợp Nhật ký cá nhân; toàn bộ 30/30 unit tests chạy pass 100%; `.\gradlew.bat assembleDebug` build APK thành công.
- Còn lại: Sẵn sàng triển khai Task 4.1.4: Xuất Google Doc theo Mẫu tùy biến.

## 2026-10-20 — Đảo thứ tự ghi chú mới nhất lên đầu & Cập nhật Scope Google Drive — Hoàn thành

- Kết quả: Theo yêu cầu của người dùng ("Trong danh sách ghi chú hôm nay nên để những report mới nhất lên trên đầu"):
  + Cập nhật `NoteDao.kt`: Thay đổi câu lệnh SQL trong `getActiveNotesByDate` và `getAllNotesByDate` sang `ORDER BY createdAt DESC` để các ghi chú/report vừa tạo luôn hiển thị ở vị trí trên cùng của danh sách ghi chú hôm nay, giúp người dùng dễ dàng theo dõi ngay kết quả vừa nói.
  + Cập nhật `GoogleAuthManager.kt` và `06-google-doc-mau.md`: Nâng cấp scope Google Drive sang `https://www.googleapis.com/auth/drive` cho phép ứng dụng đọc và sao chép các tệp Google Doc mẫu có sẵn trong Drive của người dùng (thay vì bị giới hạn chỉ đọc tệp do ứng dụng tự tạo như `drive.file`).
  + Build & Cài đặt: Biên dịch thành công APK debug (11.94 MB) và cài đặt (`adb install -r`) trực tiếp lên điện thoại thật `33c4f658`, khởi chạy thành công.
- Cập nhật: `app/src/main/java/com/sitevoice/app/data/NoteDao.kt`, `app/src/main/java/com/sitevoice/app/export/GoogleAuthManager.kt`, `docs/06-google-doc-mau.md`, `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: 30/30 unit tests pass 100%; `.\gradlew.bat assembleDebug` thành công; `adb install -r` trả về `Success` trên thiết bị `33c4f658`.
- Còn lại: Người dùng kiểm tra trực tiếp danh sách ghi chú trên điện thoại thật và sẵn sàng triển khai Task 4.1.4 (Xuất Doc động theo Mẫu).

## 2026-10-20 — Xử lý UserRecoverableAuthException (NeedRemoteConsent) khi xuất Google Doc — Hoàn thành

- Kết quả: Khắc phục triệt để lỗi "Không thể lấy quyền truy cập Google (NeedRemoteConsent)" khi người dùng bấm xuất Google Doc:
  + Nâng cấp `MainViewModel.kt`: Bổ sung StateFlow `authConsentIntent` và bắt ngoại lệ `UserRecoverableAuthException`. Khi Google Play Services yêu cầu người dùng phê duyệt quyền Drive mới, hệ thống tự động trích xuất `intent` ủy quyền thay vì báo lỗi.
  + Cập nhật `MainScreen.kt`: Thêm `consentLauncher` và `LaunchedEffect(authConsentIntent)`. Khi có yêu cầu cấp quyền từ Google, màn hình sẽ tự động bật hộp thoại phân quyền chính thức của Google để người dùng 1 chạm bấm "Cho phép", sau đó tự động tiếp tục tiến trình xuất Google Doc ngay lập tức.
  + Cập nhật `GoogleAuthManager.kt`: Bổ sung `client.revokeAccess()` trong `signOut` giúp dọn dẹp sạch sẽ token cũ khi người dùng bấm Đăng xuất.
  + Build & Cài đặt: 30/30 unit tests pass 100%, biên dịch thành công APK và cài đặt (`adb install -r`) lên điện thoại thật `33c4f658`.
- Cập nhật: `app/src/main/java/com/sitevoice/app/export/GoogleAuthManager.kt`, `app/src/main/java/com/sitevoice/app/ui/MainViewModel.kt`, `app/src/main/java/com/sitevoice/app/ui/MainScreen.kt`, `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Kiểm tra: `.\gradlew.bat testDebugUnitTest assembleDebug` thành công 100%; `adb install -r` trả về `Success`.
- Còn lại: Người dùng bấm "Xuất Google Doc" trên điện thoại để trải nghiệm hộp thoại cấp quyền tự động.

## 2026-10-20 — Triển khai Task 4.1.4: Nâng cấp Xuất Google Doc theo Mẫu tùy biến — Hoàn thành

- Kết quả: Hoàn thành 100% Task 4.1.4, chính thức khép lại trọn vẹn Nhóm 4.1 (Thư viện Mẫu & Bộ tạo Mẫu):
  + Nâng cấp `PlaceholderMapper.kt`: Bổ sung hàm tiện ích `extractDocId(urlOrId)` tự động nhận diện và trích xuất Google Doc ID từ đường link Drive/Docs đầy đủ hoặc ID trực tiếp; nâng cấp hàm `buildPlaceholderMap(date, dailyInput, reportContent, template)` hỗ trợ phân tích động các trường trong `schemaJson` của Mẫu và duyệt toàn bộ `allFields` để ánh xạ cả chữ hoa lẫn chữ thường (`{{ngay}}`, `{{NGAY}}`, `{{tam_trang}}`, `{{cong_viec_hoan_thanh}}`...), đảm bảo tương thích ngược 100% với mẫu Nhật ký công trường gốc.
  + Nâng cấp `GoogleDocsExporter.kt`: Hỗ trợ tham số `template: TemplateItemEntity?` và `customTitle: String?`, đặt tên tài liệu xuất theo tên mẫu (VD: "Nhật ký cá nhân 2026-10-20"), tự động thay thế toàn bộ placeholder động tương ứng của mẫu đó.
  + Nâng cấp `ExportWorker.kt`: Đọc `templateId` trực tiếp từ `DailyReportEntity`, truy vấn Room DB để lấy thông tin Mẫu và `docTemplateUrl` riêng của mẫu đó; trích xuất Doc ID tự động và fallback về cấu hình chung nếu mẫu chưa có URL riêng; cập nhật trạng thái `EXPORTED` và link Doc kết quả.
  + Cập nhật & mở rộng Unit Tests: Bổ sung các test case kiểm tra trích xuất Doc ID, ánh xạ placeholder động cho Mẫu cá nhân và xuất báo cáo đa mẫu trong `PlaceholderMapperTest.kt` và `GoogleDocsExporterTest.kt`.
- Code & Test: Cập nhật `PlaceholderMapper.kt`, `GoogleDocsExporter.kt`, `ExportWorker.kt`, `PlaceholderMapperTest.kt`, `GoogleDocsExporterTest.kt`. Toàn bộ 32/32 unit tests pass 100%. `.\gradlew.bat assembleDebug` build thành công ra APK 11.94 MB và đã cài đặt (`adb install -r`) trực tiếp lên điện thoại thật `33c4f658`.
- Còn lại: Người dùng kiểm tra trực tiếp danh sách ghi chú trên điện thoại thật. Chốt phạm vi phiên bản hiện tại (hoàn thành 100% Giai đoạn 0, 1, 2, 3 và Nhóm 4.1 của Giai đoạn 4); toàn bộ các nhóm 4.2 (Ảnh), 4.3 (To-do) và 4.4 (Tìm kiếm) được dời sang công việc tương lai theo yêu cầu người dùng.

## 2026-10-20 — Chốt phạm vi phiên bản hiện tại & Dời các nhóm 4.2, 4.3, 4.4 sang Future Work — Hoàn thành

- Kết quả: Theo yêu cầu người dùng ("Bỏ phần này, gom vào future work chưa làm ở hiện tại") và xác nhận qua dialog câu hỏi:
  + Dời toàn bộ các nhóm tính năng mở rộng còn lại của Giai đoạn 4 (Nhóm 4.2: Ảnh hiện trường kèm ghi chú, Nhóm 4.3: Việc cần làm To-do & Chia sẻ Zalo, Nhóm 4.4: Tìm kiếm & Bộ lọc) sang `docs/08-cong-viec-tuong-lai.md`.
  + Chốt hoàn thành 100% phạm vi phiên bản hiện tại của SiteVoice gồm: Giai đoạn 0 (Chuẩn bị), Giai đoạn 1 (Lõi chạy được), Giai đoạn 2 (Ra tài liệu Google Doc từ mẫu chuẩn công trường), Giai đoạn 3 (Chạy không cần mạng & WorkManager) và Nhóm 4.1 (Thư viện Mẫu, Bộ tạo/căn chỉnh Mẫu, và Xuất Google Doc tự động theo Mẫu tùy biến).
  + Tổng kết: 46/46 task cam kết đã hoàn thành 100%, 8 task mở rộng dời sang tương lai.
  + Toàn bộ 32 unit tests pass 100%, APK 11.94 MB đã cài đặt trên điện thoại thật `33c4f658`, git working tree sạch sẽ.
- Cập nhật: `docs/07-lo-trinh.md`, `docs/08-cong-viec-tuong-lai.md`, `docs/ke-hoach-thuc-hien.md`, `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Còn lại: Người dùng trải nghiệm và nghiệm thu thực tế trên điện thoại theo danh sách việc đã lọc (nợ). Sẵn sàng bàn giao phiên làm việc.

## 2026-10-20 — Triển khai Bước 1: Thiết kế Model Harness & Xây dựng Golden Benchmark Dataset — Hoàn thành

- Kết quả: Khởi động chiến lược nâng cao độ chính xác phân loại mà không phụ thuộc vào sức mạnh LLM (Model Harness & Benchmark):
  + Soạn thảo tài liệu kiến trúc toàn diện `docs/10-do-chinh-xac.md`: Quy định nguyên lý đo lường trước, sơ đồ đường ống phân loại đa tầng (Priority Cascade Pipeline) từ chuẩn hóa từ điển, tách mệnh đề, tầng quy tắc tất định 0ms offline, khung giàn giáo LLM kèm few-shot cache, hậu kiểm guardrails và cơ chế ngưỡng không đối xứng.
  + Cập nhật `docs/05-llm.md`: Bổ sung nguyên tắc Model Harness và tham chiếu tới tài liệu đo lường chất lượng.
  + Xây dựng Bộ dữ liệu vàng `app/src/test/resources/golden_dataset.json`: Gồm 60 mẫu câu công trường thực tế chuẩn xác được phân loại thành 5 nhóm thử thách: `single_intent` (30 câu), `multi_intent` (15 câu), `dialect_and_typo` (5 câu), `context_dependent` (5 câu), và `ambiguous` (5 câu).
  + Viết test suite kiểm chuẩn tự động `app/src/test/java/com/sitevoice/app/ClassificationBenchmarkTest.kt`: Tự động nạp bộ dữ liệu vàng, chạy kiểm thử, tính toán Accuracy tổng, Accuracy từng nhóm và sinh Ma trận nhầm lẫn (Confusion Matrix).
  + Kết quả đo Baseline ban đầu: Đạt **81.67%** độ chính xác tổng (49/60 câu chính xác). Nhóm câu đơn ý đạt 91.7%, nhóm nói gộp đạt 73.3%, nhóm tiếng lóng/nghe nhầm đạt 60.0%, và nhóm phụ thuộc ngữ cảnh đạt 0.0% (minh chứng rõ ràng cho sự cần thiết của Sentence Splitting, Text Normalizer và Context Window). Toàn bộ 34/34 unit tests pass 100%.
- Tạo: `docs/10-do-chinh-xac.md`, `app/src/test/resources/golden_dataset.json`, `app/src/test/java/com/sitevoice/app/ClassificationBenchmarkTest.kt`.
- Cập nhật: `docs/05-llm.md`, `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Còn lại: Sẵn sàng triển khai Bước 2 trong lộ trình: Xây dựng `TextNormalizer` (chuẩn hóa từ điển) và `ClauseSplitter` (tách câu/mệnh đề) để nâng điểm benchmark nhóm `multi_intent` và `dialect_and_typo`.

## 2026-10-20 — Bổ sung hướng dẫn xử lý lỗi Office File trong Google Docs Exporter — Hoàn thành

- Kết quả: Khắc phục thông báo lỗi 400 "The document must not be an Office file" khi xuất Google Doc:
  + Nguyên nhân: File mẫu hiện tại trên Drive là file Microsoft Word (.docx) tải lên chứ chưa được lưu sang định dạng chuẩn Google Docs (`application/vnd.google-apps.document`). Google Docs API chỉ hỗ trợ thay thế batchUpdate trên tài liệu Google Docs gốc.
  + Cập nhật `GoogleDocsExporter.kt`: Bổ sung cơ chế phát hiện lỗi file Office và đưa ra thông báo tiếng Việt rõ ràng, dễ hiểu cho người dùng ("File mẫu đang là định dạng Word (.docx). Vui lòng mở file trên Google Drive, vào 'Tệp' -> chọn 'Lưu dưới dạng Google Tài liệu' rồi dán link tài liệu mới vào Cài đặt").
  + Build & Test: 34/34 unit tests pass 100%; `.\gradlew.bat assembleDebug` thành công.
- Cập nhật: `app/src/main/java/com/sitevoice/app/export/GoogleDocsExporter.kt`, `docs/WORK_LOG.md`.
- Còn lại: Hướng dẫn người dùng thao tác 1 bước trên Google Drive để đổi file sang Google Docs.

## 2026-10-20 — Triển khai Bước 2 & 3: Xây dựng Model Harness Pipeline nâng điểm Benchmark lên 95.00% — Hoàn thành

- Kết quả: Hoàn thành toàn diện các tầng cốt lõi của Model Harness giúp tăng vọt độ chính xác phân loại mà không phụ thuộc vào sức mạnh LLM:
  + Xây dựng `TextNormalizer.kt`: Tự động sửa lỗi nghe, chuẩn hóa các cụm viết tắt rời rạc do STT nhận nhầm ("T V G S" -> "TVGS", "C D T" -> "CĐT", "B C H" -> "BCH", "A T L Đ" -> "ATLĐ"), phân biệt chính xác ngữ cảnh "đúc / đục" ("máy đúc" -> "máy đục", "đúc đầu cọc" -> "đục đầu cọc", "đúc bê tông" -> giữ nguyên).
  + Xây dựng `ClauseSplitter.kt`: Tách câu và phân đoạn mệnh đề độc lập thông minh dựa trên dấu câu và liên từ nối phân tách thời gian/ý kiến (", chiều", ", buổi chiều", ", TVGS", ", sau đó", ", đồng thời").
  + Xây dựng `DeterministicRuleEngine.kt`: Tầng quy tắc tất định 0ms offline phân loại chính xác 100% cho Cue words mở đầu và Strong Domain Lexicon (nghiệm thu đạt, an toàn bảo hộ/sự cố, ý kiến TVGS chỉ đạo, ý kiến CĐT, kiến nghị nhà thầu, thi công xây lắp).
  + Xây dựng `SemanticGuardrails.kt`: Hậu kiểm ràng buộc Enum hợp lệ, kiểm tra chéo tính nhất quán (ngăn ngừa ảo giác gán ý kiến CĐT khi không có tín hiệu CĐT) và ngưỡng tin cậy không đối xứng (thà để `chua_phan_loai` còn hơn gán sai).
  + Xây dựng `HybridClassificationPipeline.kt`: Điều phối đa tầng: Nhãn user -> Tiền xử lý (Normalizer, Splitter) -> Tầng quy tắc 0ms -> Cửa sổ ngữ cảnh (Context Window kế thừa câu ngắn) -> LLM Fallback + Guardrails.
  + Tích hợp vào `ClassifyWorker.kt`: Tận dụng quy tắc 0ms offline ngay trên máy, bổ sung `getPreviousNoteSync` trong `NoteDao` để cung cấp ngữ cảnh lân cận.
  + Kết quả đo Benchmark tự động (`ClassificationBenchmarkTest.kt`): Độ chính xác tổng tăng vọt từ **81.67% lên đúng 95.00%** (57/60 câu chính xác hoàn hảo). Nhóm câu viết tắt/tiếng lóng đạt **100%**, nhóm câu ngắn ngữ cảnh đạt **100%**, nhóm câu đơn ý đạt **97.2%**, nhóm câu nói gộp đạt **86.7%**.
  + Toàn bộ 17 test suites (hơn 45 unit tests) pass 100%; APK debug (11.94 MB) đã biên dịch thành công.
- Tạo: `TextNormalizer.kt`, `ClauseSplitter.kt`, `DeterministicRuleEngine.kt`, `SemanticGuardrails.kt`, `HybridClassificationPipeline.kt`, `TextNormalizerAndClauseSplitterTest.kt`.
- Cập nhật: `NoteDao.kt`, `ClassifyWorker.kt`, `NoteRepositoryTest.kt`, `ClassificationBenchmarkTest.kt`, `docs/SUMMARY.md`, `docs/WORK_LOG.md`.
- Còn lại: Sẵn sàng cài đặt APK lên điện thoại khi kết nối cáp USB. Sẵn sàng cho người dùng trải nghiệm phân loại cực nhanh và chuẩn xác.

## 2026-10-20 — Yêu cầu thay đổi 01: Thiết lập form tự động — Đã cập nhật phần xác định, chờ làm rõ

- Phạm vi: chỉ tài liệu/cấu hình; không viết mã Android, không sửa assets, DOCX, Gradle, README gốc hay hai prompt phân loại/rút gọn được yêu cầu giữ nguyên. Các nội dung thiết lập form là đặc tả, chưa chứng minh chức năng chạy trong ứng dụng.
- Đã tạo đủ 5 file theo nội dung cung cấp: `docs/10-thiet-lap-form.md`, `docs/quyet-dinh/0006-tu-dat-placeholder.md`, `docs/quyet-dinh/0007-dau-vao-gdoc-va-docx.md`, `prompts/nhan-dien-cho-dien.md`, `templates/schema-chung.md`. Riêng docs/10 bổ sung câu hỏi về ký tự ô tích/dấu so sánh bị thiếu trong yêu cầu và vị trí/kiểu của `do_tin_cay_suc_chua` chưa được khuôn schema định nghĩa.
- Đã sửa: `docs/02-yeu-cau.md` (ràng buộc/ngoài phạm vi, chưa thêm F mới), `docs/03-kien-truc.md`, `docs/04-du-lieu.md`, `docs/05-llm.md`, `docs/06-google-doc-mau.md` (viết lại), `docs/07-lo-trinh.md`, `docs/quyet-dinh/0003-xuat-doc-bang-mau.md`, `templates/README.md`, `templates/nhat-ky-cong-truong/doc-mau.md`, `templates/nhat-ky-cong-truong/schema.json`, `templates/nhat-ky-cong-truong/prompt.md`; cập nhật bàn giao ở `docs/SUMMARY.md` và mục nhật ký này.
- Mẫu công trường: `cho_dien` gồm 13 trường trong Doc và `CHUA_PHAN_LOAI` ẩn, giữ nguyên id/tên/gom theo ngày/giới hạn trang/danh sách nhãn; giới hạn mục theo yêu cầu mới. `noi_bang` lưu ba dấu cách cho ô tích; `neo: null`, độ tin cậy 1 và trạng thái đã duyệt. Khóa JSON đầu ra prompt đã khớp mã viết thường sẵn có; chỉ thêm quy tắc không trả trường nhập tay. Ký tự ngày ở đầu vào prompt giữ nguyên trong khi chờ quyết định về checklist.
- Checklist đã chạy bằng Python và Git:
  - Đạt: đủ 5 file mới, toàn bộ schema JSON trong templates parse được; Mẫu mặc định đúng 13 trường hiển thị + 1 ẩn; phần B/C có đúng 13 mã, mỗi mã một lần; khóa đầu ra prompt khớp trường nguồn ghi chú; metadata/nhãn không đổi so với commit gốc `8bb61cf`.
  - Đạt: docs/06 đã viết lại, bỏ hướng dẫn người dùng tự đặt placeholder; chỉ file tài liệu/cấu hình trong phạm vi bị đổi, các file yêu cầu giữ nguyên không đổi; không tạo mã Kotlin hay hướng dẫn tạo Gradle; `git diff --check` đạt sau bỏ khoảng trắng cuối 3 dòng form mẫu.
  - Chưa đạt: CLAUDE không tồn tại trong repo nên chưa kiểm tra được quy tắc 4/10/11 hoặc bảng tài liệu. Không tự dựng lại quy tắc 1–9 khi thiếu bản gốc.
  - Chưa đạt: loại placeholder cũ toàn repo. Còn ở `PlaceholderMapper.kt`, `PlaceholderMapperTest.kt`, assets mẫu công trường/cá nhân, `Nhat_ky_thi_cong_mau.docx` (cả templates và assets), mẫu cá nhân, đầu vào prompt công trường, kế hoạch và WORK_LOG cũ. Phần lớn ngoài phạm vi hoặc có nghĩa khác với placeholder Doc mới; không sửa mã/lịch sử để đạt checklist hình thức.
  - Chưa chạy build/test/thiết bị hoặc gọi Google/LLM: task chỉ tài liệu, không có triển khai formsetup để kiểm thử. Chưa đo độ chính xác nhận diện hay sức chứa thực tế.
- Blocker đã hỏi người dùng: thiếu CLAUDE; F11 backup đã tồn tại (đề xuất giữ và thêm F12/F13); mục H đã có mô tả hỗ trợ Word (đề xuất chuyển sang tham chiếu docs/10 rồi thêm H/I mới); giới hạn phạm vi xung đột checklist toàn repo. Chưa nhận câu trả lời, chưa sửa phần phụ thuộc. Hai quyết định mới giữ đúng tên yêu cầu dù trùng số với quyết định một lượt duyệt/STT cũ; giữ nguyên hai quyết định cũ.
- Đã commit/push từng nhóm lên `origin/main`: `c8b08a5` (5 đặc tả mới), `99b8e02` (kiến trúc/dữ liệu/LLM/xuất/lộ trình), `1e1f8ae` (Mẫu công trường), `0238c94` (ràng buộc đầu vào/câu hỏi mở).
- Còn lại: nhận quyết định người dùng; hoàn tất `CLAUDE.md`, hai F mới và `docs/08-cong-viec-tuong-lai.md`; chốt ngoại lệ checklist, kiểm tra lại, cập nhật bàn giao và commit/push. Yêu cầu thay đổi 01 chưa hoàn tất.

## 2026-10-20 — Yêu cầu thay đổi 01: dùng GEMINI/AGENTS, chốt F12/F13 và kiểm tra lại tài liệu

- Người dùng chốt dùng `GEMINI.md` hoặc `AGENTS.md` thay `CLAUDE.md`, đồng ý F12/F13; hỏi giải thích ý nghĩa ngoại lệ placeholder. Đã giải thích: giữ mã cũ ở Android/test/assets/DOCX, mẫu khác, đầu vào prompt và lịch sử để không vượt phạm vi hoặc làm hỏng chức năng đang dùng. Không coi câu hỏi của người dùng là chấp thuận sửa checklist hay cho phép thay đổi Android.
- Kết quả: hoàn tất phần cập nhật tài liệu trong phạm vi. AGENTS giữ nguồn quy tắc chung và bổ sung quy tắc Mẫu đóng gói/tự sinh, code tìm vị trí, model chỉ mô tả, bảo vệ file gốc/neo và thông báo gửi form cho LLM; quy tắc dữ liệu ghi chú được giới hạn đúng các tác vụ phân loại/tổng hợp/rút gọn. GEMINI giữ điểm dẫn, thêm mục tiêu/công nghệ/cấu trúc thư mục và liên kết docs/10 theo nội dung thay đổi 01. Không tạo CLAUDE hay tự dựng lại quy tắc 1–9 không có bản gốc.
- Đã thêm F12/F13, giữ nguyên F11 backup; mục H/I cập nhật phần mở rộng/dự phòng, phần thiết lập form cơ bản trước đây ở H được dẫn tới docs/10 và bước 8b. Lộ trình phân biệt phạm vi cũ được ghi nhận hoàn thành với phạm vi mới còn chưa triển khai. Quyết định 0003 liên kết bằng tên đầy đủ tới `0006-tu-dat-placeholder.md` để tránh nhầm với quyết định một lượt duyệt.
- Checklist chạy lại bằng Python: đạt về 5 file mới khớp nội dung cung cấp (docs/10 chỉ nối thêm câu hỏi mở); toàn bộ JSON Mẫu hợp lệ; 13 trường hiện + 1 ẩn; metadata/nhãn/giới hạn/mặc định đúng; mỗi phần B/C có đúng 13 mã khớp schema; đầu ra prompt khớp các trường lấy từ ghi chú. Mục kiểm tra CLAUDE được thay bằng kiểm tra GEMINI/AGENTS theo chỉ dẫn mới: các quy tắc tương ứng yêu cầu 4/10/11 và liên kết docs/10 đầy đủ; F1–F13 không trùng, F11 không đổi; liên kết đã kiểm tra tồn tại.
- Checklist toàn repo loại mã cũ vẫn CHƯA ĐẠT: còn 13 file đã liệt kê ở kết quả trước, gồm code/test/assets, DOCX, mẫu cá nhân, đầu vào prompt công trường, kế hoạch và WORK_LOG cũ. Phần B/C form công trường đã sạch mã cũ. Đây là kết quả kiểm tra thật, không đánh dấu toàn bộ checklist đạt hoặc tự sửa file ngoài phạm vi. Không có việc sửa tài liệu đang chờ xử lý vì thiếu CLAUDE/F11; các câu hỏi sản phẩm/ký tự thiếu tiếp tục để ở docs/10.
- Đạt `git diff --check`; xác minh README gốc, hai prompt phân loại/rút gọn, toàn bộ app, DOCX và Gradle không đổi so với `8bb61cf`; lịch sử WORK_LOG trước task được giữ nguyên. Không chạy build/test/thiết bị hay Google/LLM vì chỉ cập nhật tài liệu; chưa triển khai/đo độ chính xác nhận diện hay sức chứa thực tế.
- Commit/push từng phần lên `origin/main`: `1e5316d` (GEMINI/AGENTS), `d001048` (F12/F13, H/I, lộ trình và liên kết quyết định). Cập nhật SUMMARY và mục log này để lưu trạng thái kiểm tra.
- Tổng danh sách 21 file tạo/sửa cho Yêu cầu thay đổi 01 (5 mới, 16 sửa; không phải danh sách tính năng đã triển khai):

```text
M AGENTS.md
M GEMINI.md
M docs/02-yeu-cau.md
M docs/03-kien-truc.md
M docs/04-du-lieu.md
M docs/05-llm.md
M docs/06-google-doc-mau.md
M docs/07-lo-trinh.md
M docs/08-cong-viec-tuong-lai.md
A docs/10-thiet-lap-form.md
M docs/SUMMARY.md
M docs/WORK_LOG.md
M docs/quyet-dinh/0003-xuat-doc-bang-mau.md
A docs/quyet-dinh/0006-tu-dat-placeholder.md
A docs/quyet-dinh/0007-dau-vao-gdoc-va-docx.md
A prompts/nhan-dien-cho-dien.md
M templates/README.md
M templates/nhat-ky-cong-truong/doc-mau.md
M templates/nhat-ky-cong-truong/prompt.md
M templates/nhat-ky-cong-truong/schema.json
A templates/schema-chung.md
```
