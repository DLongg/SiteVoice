# 0003. Xuất Google Doc bằng Doc mẫu và placeholder

Trạng thái: chấp nhận

## Quyết định
Copy một Google Doc mẫu bằng Drive API rồi `replaceAllText` bằng Docs API. Định dạng, bảng, chữ ký nằm sẵn trong mẫu.

## Phương án đã loại
Tạo .docx trên máy (Apache POI) rồi upload: định dạng phức tạp hơn, khó giữ đúng form.

## Hệ quả
Cần Google Sign-In và hàng đợi khi mất mạng. Danh sách placeholder là hợp đồng giữa Doc mẫu và Mẫu (`templates/<mau>/doc-mau.md`).

Cập nhật: placeholder không còn do người dùng tự đặt, xem 0006. Phần xuất (copy + replaceAllText) vẫn giữ nguyên.
