# 02. Yêu cầu

## Chức năng
- F1. Nút nhấn giữ để ghi âm; thả ra là lưu.
- F2. Lưu file âm thanh gốc, phát lại được.
- F3. Chuyển giọng nói thành văn bản (API khi có mạng; offline ở giai đoạn sau).
- F4. Gán nhãn mục tự động: Thi công, Nghiệm thu, An toàn, Ý kiến CĐT, Ý kiến TVGS, Ý kiến nhà thầu, Chưa phân loại.
- F5. Danh sách ghi chú trong ngày: xem, sửa chữ, đổi nhãn, xóa, nghe lại.
- F6. Chuẩn bị cuối ngày: sửa ghi chú nếu cần, nhập thời tiết (3 buổi), số ban chỉ huy, số công nhân, danh sách máy móc; không yêu cầu duyệt ghi chú riêng.
- F7. Tổng hợp bằng LLM theo Mẫu, có kiểm tra ký tự và vòng rút gọn.
- F8. Chỉ một lượt duyệt ở bản xem trước cuối cùng; duyệt xong lưu nội dung đã duyệt rồi xuất, không yêu cầu duyệt lần hai.
- F9. Xuất Google Doc từ Doc mẫu (copy + thay placeholder).
- F10. Cài đặt: nhà cung cấp LLM, API key, tài khoản Google, máy móc mặc định, từ điển thuật ngữ.
- F11. Backup theo trạng thái: ngày đã duyệt giữ nội dung hoàn chỉnh đã duyệt; ngày chưa duyệt giữ văn bản chuyển từ giọng nói và thông tin liên quan để tiếp tục xử lý, đến khi duyệt xong, không giới hạn số ngày. Backup không kèm âm thanh; điện thoại vẫn giữ âm thanh gốc để nghe lại. Cách đưa backup sang máy tính và định dạng backup chưa chốt.
- F12. Thiết lập form một lần: nạp Google Doc hoặc DOCX từ máy, app tự nhận diện chỗ điền, tự đặt placeholder, tự sinh Mẫu và giới hạn; người dùng duyệt (xem docs/10).
- F13. Đo sức chứa tự động để vừa số trang của form (xuất PDF, đếm trang).

## Lưu dữ liệu và backup
- Nội dung đã duyệt phải có bản trên điện thoại; yêu cầu có thêm bản trên máy tính và Google Docs đã được ghi nhận, cơ chế chuyển bản sang máy tính chưa chốt.
- Ngày đã duyệt: backup nội dung hoàn chỉnh đã duyệt cùng ngày và thông tin Mẫu cần thiết để lấy lại nội dung khi Google Docs gặp vấn đề; không chỉ lưu link Doc. Không cần kèm ghi chú trung gian hay âm thanh.
- Ngày chưa duyệt: backup văn bản chuyển từ giọng nói (gồm chỉnh sửa hiện có), ngày ghi, nhãn, dữ liệu cuối ngày, bản nháp và trạng thái xử lý hiện có để tiếp tục tổng hợp/duyệt. Giữ đến khi duyệt xong, kể cả ngày tồn lâu; không áp dụng giới hạn 7/30 ngày.
- Backup không chứa file âm thanh. Trên điện thoại vẫn giữ đầy đủ dữ liệu ngày chưa duyệt, gồm âm thanh gốc, đến khi duyệt xong; việc duyệt xong không kích hoạt tự xóa âm thanh. Chỉ thu gọn nội dung backup, không tự xóa dữ liệu gốc trên điện thoại.
- Phục hồi từ backup chỉ khôi phục văn bản và thông tin được backup, không tái tạo âm thanh để nghe lại. Ghi âm chưa chuyển thành văn bản vẫn chỉ có bản gốc trên điện thoại, không được coi là đã có backup văn bản.
- Dọn dữ liệu theo khoảng ngày là yêu cầu đã ghi nhận; phạm vi dữ liệu/nơi xóa và điều kiện bảo vệ ngày chưa duyệt cần làm rõ trước triển khai. Không tự đặt thời hạn xóa âm thanh gốc.

## Ràng buộc
- Tài liệu cuối tối đa 2 trang, kiểm soát bằng ngân sách ký tự từng mục (xem `05-llm.md`).
- Ghi âm dùng được khi không có mạng; các khâu cần mạng đi qua hàng đợi.
- Ngân sách thấp: ưu tiên dịch vụ rẻ/miễn phí, người dùng tự nhập API key.
- Không bịa dữ liệu: LLM chỉ dùng thông tin trong ghi chú.
- API key lưu mã hóa.
- Form nguồn: Google Doc hoặc DOCX; không hỗ trợ PDF/ảnh. Luôn làm việc trên bản sao.

## Ngoài phạm vi (tạm thời)
iOS, web, đồng bộ nhiều thiết bị, đăng lên Google Play. PDF/ảnh làm form nguồn.

## Tiêu chí hoàn thành phiên bản đầu
Một ngày ghi thật ≥ 15 ghi chú ra được Google Doc đúng form, ≤ 2 trang, người dùng chỉ phải sửa nhẹ.
