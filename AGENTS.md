# Quy tắc làm việc với SiteVoice

## Bắt đầu phiên và đọc context

1. Đọc file này, sau đó đọc [docs/SUMMARY.md](docs/SUMMARY.md), kể cả khi tiếp tục sau reset/compact.
2. Đối chiếu yêu cầu hiện tại với trạng thái và mục bàn giao trong SUMMARY; kiểm tra các file liên quan trước khi sửa. Nếu có Git, xem diff/status để giữ thay đổi của người dùng; nếu không có Git, đối chiếu trực tiếp file.
3. Chỉ đọc thêm tài liệu/module liên quan trong bảng dưới. Không quét lại toàn bộ repository hoặc tự động tải toàn bộ WORK_LOG mỗi phiên. Khi cần lịch sử, tìm theo task ID, ngày hoặc module rồi đọc đoạn phù hợp.
4. Source code và kết quả kiểm tra là bằng chứng về triển khai. Tài liệu thiết kế mô tả mục tiêu, không chứng minh chức năng đã chạy. Ghi rõ điều chưa xác minh; không suy diễn tiến độ từ ví dụ hoặc kế hoạch.

| Cần biết | Nguồn đọc khi cần |
|---|---|
| Mục tiêu, phạm vi, yêu cầu | `docs/01-tong-quan.md`, `docs/02-yeu-cau.md` |
| Kiến trúc, dữ liệu | `docs/03-kien-truc.md`, `docs/04-du-lieu.md` |
| LLM, thuật ngữ | `docs/05-llm.md`, `docs/09-thuat-ngu.md`, prompt liên quan trong `prompts/` |
| Mẫu, giới hạn, placeholder | `templates/<ten-mau>/schema.json`, `prompt.md`, `doc-mau.md`; xuất Doc: `docs/06-google-doc-mau.md` |
| Thiết lập form tự động (Google Doc/DOCX) | `docs/10-thiet-lap-form.md`, `templates/schema-chung.md`, `prompts/nhan-dien-cho-dien.md` |
| Lộ trình, task cụ thể | `docs/07-lo-trinh.md`; tìm task ID trong `docs/ke-hoach-thuc-hien.md` |
| Lý do quyết định, mở rộng | File liên quan trong `docs/quyet-dinh/`; `docs/08-cong-viec-tuong-lai.md` |
| Thiết kế UI, nguyên tắc giao diện | Skill `design-taste-frontend` (`.agents/skills/design-taste-frontend/SKILL.md`), `docs/03-kien-truc.md` |
| Lịch sử thực hiện | Đoạn liên quan trong `docs/WORK_LOG.md`; nhật ký cũ chỉ khi cần đối chiếu |

## Coding, phạm vi và kiểm tra

- Khi có bất kỳ bước nào chưa rõ, bắt buộc hỏi người dùng để làm rõ trước khi thực hiện bước đó. Không tự suy đoán yêu cầu hoặc tự chọn phương án cho phần chưa rõ; phải chờ người dùng trả lời rồi mới tiếp tục bước đó.
- Khi gặp bước cần người dùng thao tác, phải nêu rõ việc cần làm và đưa ra gợi ý hoặc hướng dẫn cụ thể. Nếu người dùng chốt phương án cho phép agent làm tiếp, tiếp tục theo phương án đã chốt. Nếu chưa chốt hoặc phương án vẫn cần người dùng thao tác, phải chờ người dùng xác nhận đã làm xong rồi mới thực hiện bước phụ thuộc; không coi việc đồng ý với gợi ý là đã hoàn tất thao tác.
- Làm từng bước nhỏ trong phạm vi người dùng giao và lộ trình hiện hành. Không tự đổi kiến trúc, thêm chức năng hoặc dependency ngoài yêu cầu. Khi phát hiện xung đột thiết kế ảnh hưởng task, ghi lại và xin làm rõ trước khi tự chọn phương án.
- Giữ các quyết định đã chấp nhận trong `docs/quyet-dinh/`. Khi thay đổi thiết kế đã được cho phép, sửa tài liệu gốc liên quan; quyết định lớn cần file quyết định mới.
- Quy tắc sản phẩm kế thừa từ GEMINI.md: lưu âm thanh gốc trước mọi xử lý, không tự xóa; một nút nhấn giữ, không bắt chọn mục lúc ghi; thời tiết/nhân lực nhập cuối ngày, mặc định từ hôm trước.
- KHÔNG viết cứng nội dung nhật ký công trường trong code. Mọi loại tài liệu đi qua Mẫu (đóng gói sẵn trong templates/ hoặc do app sinh từ form của người dùng).
- Chỗ điền trong form do CODE tìm; model chỉ đặt tên/phân loại/ghép nguồn, không tạo hoặc dời vị trí.
- Không bao giờ sửa file gốc của người dùng; luôn làm việc trên bản sao. Không âm thầm điền khi không tìm thấy neo: phải báo người dùng.
- Thiết kế giao diện (UI): Khi tạo mới hoặc chỉnh sửa màn hình Compose / giao diện ứng dụng, bắt buộc áp dụng skill `design-taste-frontend` (nguyên tắc App Screens trong `.agents/skills/design-taste-frontend/SKILL.md`: tinh gọn, chống "AI-slop", không gradient/glow/hiệu ứng thừa, kiểu chữ rõ ràng, phân cấp thị giác chuẩn, phản hồi trạng thái đầy đủ).
- Khi phân loại/tổng hợp/rút gọn, LLM chỉ dùng dữ liệu ghi chú; không rõ mục thì để chưa phân loại. Khi thiết lập form, LLM chỉ mô tả ứng viên do code cung cấp từ nội dung form; phải thông báo trước khi gửi nội dung form cho nhà cung cấp LLM. Người dùng phải duyệt bản xem trước trước khi xuất Google Doc. Tác vụ mạng theo thiết kế phải qua WorkManager, chờ mạng và có retry.
- Chuỗi UI tiếng Việt trong `strings.xml`; tên biến/hàm/class tiếng Anh. API key lưu mã hóa, không lưu trong Room; không ghi key, token, credentials hoặc dữ liệu nhạy cảm vào log/context/commit.
- Chỉ chạy lệnh build/test khi cấu hình thực sự tồn tại. Sau khi có Gradle wrapper, dùng wrapper của dự án (`.\gradlew.bat assembleDebug`, `.\gradlew.bat test`, `.\gradlew.bat lint` trên Windows; `./gradlew` tương ứng trên hệ khác), tại thư mục chứa wrapper.
- Kiểm tra đúng phần thay đổi: logic thuần (đếm ký tự, phân loại từ khóa, ghép placeholder) cần test phù hợp khi triển khai; bước ứng dụng cần xác minh trên điện thoại thật theo lộ trình. Task tài liệu kiểm tra nội dung, liên kết, tính nhất quán; không cần tạo test ứng dụng.
- Báo rõ kiểm tra đã chạy, kết quả và phần chưa chạy cùng lý do. Không ghi build/test/thiết bị là đạt nếu chưa thực hiện.
- Luôn commit và push liên tục: Mọi thay đổi dù là nhỏ nhất (tạo file, sửa hàm, sửa tài liệu, fix bug), bắt buộc phải commit thật chi tiết từng phần với thông điệp rõ ràng và push ngay lên Git remote (origin/main). Tuyệt đối không gom nhiều thay đổi rời rạc vào một commit lớn; lịch sử Git phải phản ánh minh bạch từng bước phát triển.

## Kết thúc task và duy trì bộ nhớ

1. Kiểm tra thay đổi thực tế và kết quả. Không đánh dấu task dở dang là hoàn thành.
2. Bổ sung một mục có ngày vào cuối [docs/WORK_LOG.md](docs/WORK_LOG.md) cho mỗi task có kết quả đáng kể, lỗi/blocker quan trọng, hoặc quyết định hoãn/hủy. Ghi task ID nếu có, kết quả, file/module, kiểm tra, lỗi và xử lý, việc còn lại. Không log từng lần đọc/tìm/sửa nhỏ; không viết lại lịch sử cũ.
3. Cập nhật [docs/SUMMARY.md](docs/SUMMARY.md) khi trạng thái, quyết định, blocker hoặc bước tiếp theo thay đổi. Giữ khoảng tối đa 80 dòng; chỉ lưu trạng thái đang hữu ích và liên kết tới nguồn, không chép nhật ký hay đặc tả. Cập nhật checkbox/trạng thái trong kế hoạch gốc nếu task đó thực sự thay đổi.
4. Mỗi loại thông tin có một nguồn chính: quy tắc ở AGENTS; trạng thái hiện tại ở SUMMARY; lịch sử ở WORK_LOG; đặc tả/quyết định ở tài liệu gốc; giới hạn Mẫu ở schema.json. GEMINI.md chỉ là điểm dẫn; không ghi log song song vào nhật ký cũ.
5. Khi tóm tắt, giữ quyết định/ràng buộc quan trọng hoặc liên kết tới nguồn lưu chúng. Không xóa lịch sử hoặc thay thế bằng thông tin chưa có bằng chứng.

## Bàn giao trước khi ngắt phiên/compact

- Khi được báo sắp compact, người dùng yêu cầu bàn giao, hoặc trước khi chủ động dừng task chưa xong: cập nhật mục bàn giao trong SUMMARY với mục tiêu/phạm vi, việc đã làm và file đã sửa, việc chưa xong, lỗi/blocker, kiểm tra đã/chưa chạy và bước cụ thể tiếp theo. Chỉ dẫn tới WORK_LOG/tài liệu khi cần chi tiết.
- Phiên mới dùng mục bàn giao rồi kiểm tra lại file liên quan trước khi tiếp tục; không chạy lại toàn bộ khảo sát.
- Đây là workflow bằng instruction. Repository chưa có hook tự cập nhật context hoặc cơ chế phát hiện dung lượng context. Việc đọc AGENTS phụ thuộc agent hỗ trợ; nếu không, người dùng cần yêu cầu đọc file này. Reset/compact đột ngột có thể làm mất thay đổi chưa được ghi lại; không cam kết tự động lưu trước mọi lần compact.
