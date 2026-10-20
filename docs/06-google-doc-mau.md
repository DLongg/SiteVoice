# 06. Google Doc mẫu và xuất file

## Cách hoạt động
Điều kiện xuất: người dùng đã duyệt bản xem trước một lần và ứng dụng đã lưu nội dung đã duyệt trên điện thoại. Không yêu cầu lượt duyệt ghi chú riêng trước tổng hợp; lỗi mạng khi xuất chỉ retry nội dung đã duyệt, không bắt duyệt lại. Xem [0006](quyet-dinh/0006-mot-luot-duyet.md).

1. Người dùng tạo sẵn MỘT Google Doc mẫu (định dạng, bảng, chữ ký nằm sẵn), điền placeholder dạng `{{TEN}}`.
2. App dùng Drive API copy bản mẫu sang tên "Nhật ký thi công yyyy-MM-dd".
3. App dùng Docs API `batchUpdate` với `replaceAllText` để thay từng placeholder.
4. Lưu link Doc vào `DailyReport.docUrl`.

## Quy tắc
- Placeholder viết HOA, không dấu, dạng `{{TEN}}`. Danh sách đầy đủ ở `templates/<mau>/doc-mau.md`.
- Ô tích thời tiết: thay bằng ký tự `☑` hoặc `☐` (hai placeholder cho mỗi lựa chọn).
- Danh sách nhiều dòng: nối bằng ký tự xuống dòng, mỗi dòng bắt đầu bằng `- `.
- Placeholder không có dữ liệu: thay bằng chuỗi rỗng, không để lại `{{...}}` trong Doc.
- Không bao giờ sửa Doc mẫu gốc, chỉ sửa bản sao.
- Quyền Google: xin `drive` và `documents` (cho phép đọc/sao chép Doc mẫu có sẵn và ghi nội dung).

## Kiểm tra sau khi xuất
- Còn sót `{{` trong Doc → báo lỗi.
- (Tùy chọn) Xuất PDF, đếm trang, > 2 trang → báo cần điều chỉnh ngân sách ký tự; nếu sửa/rút gọn nội dung thì phải tạo bản nháp để duyệt nội dung mới, không âm thầm đổi bản đã duyệt.
