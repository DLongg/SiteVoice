# 06. Google Doc mẫu và xuất file

## Hai nguồn của Doc mẫu
- Mẫu mặc định: file mẫu có sẵn placeholder (`templates/nhat-ky-cong-truong/doc-mau.md`).
- Mẫu tự sinh: bản sao làm việc đã được app ghi placeholder lúc thiết lập (`docs/10-thiet-lap-form.md`).
Từ lúc có Doc mẫu, cách xuất hằng ngày GIỐNG NHAU.

## Xuất hằng ngày — Cơ chế Quét sạch 2 lớp (Two-Pass Clean Export)
Để đảm bảo tài liệu xuất ra đạt độ hoàn thiện cao nhất, không bao giờ bị lộ placeholder thô hoặc thẻ chưa điền dữ liệu, `GoogleDocsExporter` áp dụng quy trình xuất quét sạch 2 lớp:

1. **Tạo bản sao làm việc**: Drive API v3 sao chép Doc mẫu sang tên mới: `"<Tên Mẫu> yyyy-MM-dd"`.
2. **Lớp 1 (Pass 1 — Map-based Replacement)**:
   - `PlaceholderMapper` nạp dữ liệu ngày tháng, thời tiết, nhân lực, thiết bị và các trường báo cáo đã tổng hợp.
   - Nạp toàn bộ các trường định nghĩa trong `schemaJson` của Mẫu (hỗ trợ cả tiếng Việt `truong_tong_hop` và tiếng Anh `summary_fields` / `summaryFields`) với giá trị mặc định là chuỗi rỗng `""` nếu không có dữ liệu.
   - Tự động sinh các biến thể khoảng trắng (`{{MA}}`, `{{ MA }}`, `{{ MA}}`, `{{MA }}`) và không phân biệt chữ hoa/thường (`matchCase = false`).
   - Gửi lệnh `batchUpdate` với danh sách các thao tác `replaceAllText` để điền dữ liệu vào bản sao Doc.
3. **Lớp 2 (Pass 2 — Sweep & Clean Remaining Tags)**:
   - Gọi Docs API `documents.get` để đọc lại toàn bộ cấu trúc nội dung văn bản của tài liệu vừa điền ở Pass 1.
   - Sử dụng biểu thức chính quy `\{\{[^{}]+\}\}` để phát hiện mọi thẻ `{{...}}` còn sót lại trên tài liệu (do người dùng thêm trong Doc mẫu nhưng không có trong schema, hoặc lỗi cú pháp).
   - Nếu phát hiện thẻ sót, gửi tiếp lệnh `batchUpdate` thứ 2 với `replaceAllText` -> `""` để xóa sạch 100% các tag rỗng.
4. **Hoàn tất & Lưu trữ**:
   - Lưu liên kết tài liệu vào `DailyReport.docUrl`.
   - Tùy chọn: xuất định dạng PDF/DOCX qua Drive API `files.export` để chia sẻ hoặc in ấn.

## Quy tắc
- Placeholder viết HOA, không dấu, dạng `{{TEN}}`. Danh sách theo `cho_dien[]` hoặc `summary_fields` của Mẫu.
- Định dạng giá trị theo `loai` (xem `templates/schema-chung.md`): danh sách mỗi dòng `- `, ô tích `☑`/`☐`, ngày theo định dạng dd/MM/yyyy.
- Giá trị mặc định của mọi placeholder trong schema: chuỗi rỗng `""`.
- Không bao giờ sửa Doc mẫu gốc hay file gốc của người dùng, chỉ thực hiện trên bản sao.
- Quyền Google: tối thiểu cần thiết (`drive.file`, `documents`); file của người dùng được cấp quyền qua Google Picker.
- Ô tích là ký tự `☑`/`☐`, không dùng checkbox tương tác của Google Docs vì API không hỗ trợ cập nhật trạng thái.

## Kiểm tra giới hạn trang
Xuất PDF, đếm trang. Vượt giới hạn: giảm giới hạn mục chiếm nhiều chỗ nhất rồi làm lại; nếu vẫn vượt, báo người dùng chọn bỏ bớt ý.
