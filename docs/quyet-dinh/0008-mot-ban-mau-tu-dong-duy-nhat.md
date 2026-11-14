# 0008. Một Bản Mẫu Tự Động Duy Nhất Từ Google Doc

Trạng thái: chấp nhận (2026-11-03)

## Bối cảnh
Trước đây ứng dụng đóng gói sẵn 2 mẫu mặc định (`nhat-ky-cong-truong` và `nhat-ky-ca-nhan`) trong màn hình Thư viện Mẫu và tách rời hộp thoại thiết lập 2 bước với màn hình chỉnh sửa mẫu. Người dùng mong muốn bỏ 2 mẫu mặc định có sẵn đó, đi thẳng vào luồng **Nhật ký tự động**: dán link Google Doc để hệ thống tự động tạo bản mẫu một lần trước, sau đó hiển thị duy nhất một bản mẫu để người dùng trực tiếp chỉnh sửa mọi thành phần (System Prompt, Form Google Doc, các thẻ `{{MA}}`, vị trí neo, nhiệt độ AI).

## Quyết định
1. Loại bỏ việc hiển thị và khởi tạo sẵn 2 mẫu mặc định (`Nhật ký thi công công trường`, `Nhật ký hằng ngày cá nhân`) trong danh sách mẫu của người dùng; dọn sạch các bản mẫu `isBuiltIn = true` cũ trong cơ sở dữ liệu Room khi khởi chạy.
2. Chuyển giao diện quản lý mẫu sang mô hình **Một Bản Mẫu Tự Động Duy Nhất** kết hợp **Duyệt 2 Bước (`FormSetupDialog`)**:
   - Lần đầu (khi chưa có mẫu): Người dùng dán link/ID Google Doc (kèm thông báo minh bạch trước khi gửi cấu trúc chỗ điền cho LLM) -> hệ thống tự động tạo bản sao làm việc trên Google Drive, quét cấu trúc, gọi AI đề xuất các thẻ `{{MA}}` và System Prompt -> mở hộp thoại **Thiết lập Biểu mẫu 2 Bước** (Bước 1: duyệt/sửa các thẻ `{{MA}}` & vị trí neo; Bước 2: duyệt/sửa System Prompt & nhiệt độ AI) trước khi ghi placeholder vào bản sao Google Doc và lưu thành bản mẫu duy nhất đang kích hoạt (`auto_single_template`).
   - Sau khi tạo (và các lần mở sau): Mở trực tiếp màn hình chỉnh sửa (`TemplateEditorScreen`) của bản mẫu duy nhất đó để người dùng tùy chỉnh tên, form Google Doc bản sao, danh sách các mục/thẻ `{{MA}}`, tọa độ vị trí neo, System Prompt (kèm nút Render lại từ các thẻ) và thanh trượt Nhiệt độ AI (`0.0 - 0.8`), hoặc nạp link Google Doc mới (qua quy trình duyệt 2 bước) nếu muốn thay thế bản mẫu.
3. Khi người dùng chưa nạp Google Doc lần đầu mà bấm ghi âm trước, bộ phân loại ghi chú vẫn có danh mục dự phòng an toàn trong code để không gây lỗi ứng dụng.

## Hệ quả
- Giảm tối đa thao tác thừa: không còn màn hình danh sách nhiều mẫu gây rối, người dùng chỉ làm việc tập trung trên đúng một bản mẫu tự động sinh ra từ file Google Doc của họ.
- Vẫn tuân thủ trọn vẹn nguyên tắc bảo vệ file gốc (luôn thao tác trên bản sao Google Drive) và code tìm vị trí chỗ điền (`FormCandidateScanner`).
