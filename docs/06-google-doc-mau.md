# 06. Google Doc mẫu và xuất file

## Cách hoạt động
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
- Quyền Google: chỉ xin `drive.file` và `documents` (tối thiểu cần thiết).

## Kiểm tra sau khi xuất
- Còn sót `{{` trong Doc → báo lỗi.
- (Tùy chọn) Xuất PDF, đếm trang, > 2 trang → giảm ngân sách ký tự.
