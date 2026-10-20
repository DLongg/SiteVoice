# 0006. App tự đặt placeholder vào form của người dùng

Trạng thái: chấp nhận (thay thế một phần 0003)

## Bối cảnh
Bắt người dùng tự gõ `{{...}}` vào Doc làm họ phải thao tác nhiều và dễ sai.

## Quyết định
Người dùng chỉ nạp form một lần và duyệt. App tự tìm chỗ điền (code), đặt tên/ghép nguồn (model), tự ghi placeholder, tự sinh schema và giới hạn qua đo sức chứa.

## Hệ quả
- Xuất hằng ngày vẫn dùng copy + `replaceAllText` (0003 còn hiệu lực cho phần xuất).
- Mẫu có thể sinh ở thời điểm chạy, không chỉ đóng gói sẵn.
- Cần bước duyệt thiết lập, Doc thử và bài kiểm tra chuẩn.
- Gửi nội dung form cho LLM nên phải thông báo cho người dùng.

## Phương án đã loại
- Người dùng tự đặt placeholder bằng tay: quá nhiều thao tác.
- App tự dựng tài liệu từ đầu bằng code: không giữ được form có sẵn của công ty (để dành làm phương án thay thế nếu việc sửa Doc gây lỗi).
