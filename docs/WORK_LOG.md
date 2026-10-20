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
