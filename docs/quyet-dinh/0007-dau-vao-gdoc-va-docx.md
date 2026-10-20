# 0007. Đầu vào form: Google Doc và DOCX từ máy

Trạng thái: chấp nhận

## Quyết định
- Google Doc: chọn bằng Google Picker, quyền `drive.file`.
- DOCX từ máy: tải lên Drive, chuyển thành Google Doc, rồi xử lý như Google Doc.
- Luôn làm việc trên bản sao.

## Lý do
Chỉ cần một đường xử lý (Docs API) cho cả hai loại đầu vào.

## Hệ quả
- DOCX có thể mất một số định dạng nâng cao khi chuyển; Doc thử cho người dùng thấy trước.
- Không hỗ trợ PDF/ảnh ở giai đoạn này.
- Có thể xuất kết quả thành DOCX/PDF bằng Drive `files.export` (xem câu hỏi mở trong `10-thiet-lap-form.md`).
