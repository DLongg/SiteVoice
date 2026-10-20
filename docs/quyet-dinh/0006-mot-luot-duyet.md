# 0006. Chỉ một lượt duyệt ở bản xem trước cuối cùng

Trạng thái: chấp nhận theo yêu cầu người dùng ngày 2026-10-20; mới cập nhật thiết kế, chưa triển khai.

## Bối cảnh
Luồng cũ yêu cầu duyệt ghi chú trước tổng hợp rồi duyệt bản xem trước trước xuất. Người dùng yêu cầu chỉ một lượt duyệt.

## Quyết định
- Sửa ghi chú, đổi nhãn, loại bỏ ghi chú và nhập dữ liệu cuối ngày là chỉnh dữ liệu, không phải một chốt duyệt.
- LLM dùng ghi chú không bị loại để tạo bản nháp theo Mẫu; hoàn tất kiểm tra và rút gọn trước khi người dùng duyệt.
- Người dùng chỉ duyệt bản xem trước cuối cùng. Lưu nội dung đã duyệt trên điện thoại trước khi đưa việc xuất Google Doc vào hàng đợi; không thêm lượt xác nhận duyệt khi xuất.
- Chưa duyệt thì giữ bản nháp. Lỗi xuất hoặc chờ mạng không yêu cầu duyệt lại cùng nội dung.
- Nếu sửa dữ liệu/tổng hợp lại làm thay đổi nội dung đã duyệt, kết quả mới là bản nháp cần được duyệt; không dùng lần duyệt cũ để xuất nội dung đã thay đổi.

## Hệ quả
- Bỏ Note.APPROVED và nút duyệt ghi chú riêng; DailyReport giữ DRAFT, APPROVED, EXPORTED.
- Cập nhật prompt và kế hoạch; mọi task ứng dụng vẫn chưa thực hiện.
- Quyết định này không chốt giờ thông báo, hành vi lúc 02:00, số ngày giữ dữ liệu chưa duyệt hoặc cơ chế backup máy tính. Phạm vi backup theo [yêu cầu](../02-yeu-cau.md#lưu-dữ-liệu-và-backup).
