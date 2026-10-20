# 06. Google Doc mẫu và xuất file

## Hai nguồn của Doc mẫu
- Mẫu mặc định: file mẫu có sẵn placeholder (`templates/nhat-ky-cong-truong/doc-mau.md`).
- Mẫu tự sinh: bản sao làm việc đã được app ghi placeholder lúc thiết lập (`docs/10-thiet-lap-form.md`).
Từ lúc có Doc mẫu, cách xuất hằng ngày GIỐNG NHAU.

## Xuất hằng ngày
1. Drive API copy Doc mẫu sang tên "<Tên Mẫu> yyyy-MM-dd".
2. Docs API `batchUpdate` với `replaceAllText` cho từng placeholder (hoặc chèn theo neo nếu `replaceAllText` không phù hợp).
3. Kiểm tra: còn sót `{{` trong Doc thì báo lỗi.
4. Lưu link Doc vào `DailyReport.docUrl`. Tùy chọn: xuất DOCX/PDF bằng Drive `files.export` để tải về.

## Quy tắc
- Placeholder viết HOA, không dấu, dạng `{{TEN}}`. Danh sách theo `cho_dien[]` của Mẫu.
- Định dạng giá trị theo `loai` (xem `templates/schema-chung.md`): danh sách mỗi dòng `- `, ô tích ``/``, ngày theo `tuy_chon`.
- Placeholder không có dữ liệu: thay bằng chuỗi rỗng.
- Không bao giờ sửa Doc mẫu gốc hay file gốc của người dùng, chỉ sửa bản sao.
- Quyền Google: tối thiểu cần thiết (`drive.file`, `documents`); file có sẵn của người dùng được cấp quyền qua Google Picker.
- Ô tích là ký tự ``/``, không dùng checkbox thật của Google Docs.

## Kiểm tra giới hạn trang
Xuất PDF, đếm trang. Vượt giới hạn: giảm giới hạn mục chiếm nhiều chỗ nhất rồi làm lại; nếu vẫn vượt, báo người dùng chọn bỏ bớt ý.
