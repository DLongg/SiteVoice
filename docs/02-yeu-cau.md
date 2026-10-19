# 02. Yêu cầu

## Chức năng
- F1. Nút nhấn giữ để ghi âm; thả ra là lưu.
- F2. Lưu file âm thanh gốc, phát lại được.
- F3. Chuyển giọng nói thành văn bản (API khi có mạng; offline ở giai đoạn sau).
- F4. Gán nhãn mục tự động: Thi công, Nghiệm thu, An toàn, Ý kiến CĐT, Ý kiến TVGS, Ý kiến nhà thầu, Chưa phân loại.
- F5. Danh sách ghi chú trong ngày: xem, sửa chữ, đổi nhãn, xóa, nghe lại.
- F6. Màn hình duyệt cuối ngày: thời tiết (3 buổi), số ban chỉ huy, số công nhân, danh sách máy móc.
- F7. Tổng hợp bằng LLM theo Mẫu, có kiểm tra ký tự và vòng rút gọn.
- F8. Bản xem trước trước khi xuất.
- F9. Xuất Google Doc từ Doc mẫu (copy + thay placeholder).
- F10. Cài đặt: nhà cung cấp LLM, API key, tài khoản Google, máy móc mặc định, từ điển thuật ngữ.

## Ràng buộc
- Tài liệu cuối tối đa 2 trang, kiểm soát bằng ngân sách ký tự từng mục (xem `05-llm.md`).
- Ghi âm dùng được khi không có mạng; các khâu cần mạng đi qua hàng đợi.
- Ngân sách thấp: ưu tiên dịch vụ rẻ/miễn phí, người dùng tự nhập API key.
- Không bịa dữ liệu: LLM chỉ dùng thông tin trong ghi chú.
- API key lưu mã hóa.

## Ngoài phạm vi (tạm thời)
iOS, web, đồng bộ nhiều thiết bị, đăng lên Google Play.

## Tiêu chí hoàn thành phiên bản đầu
Một ngày ghi thật ≥ 15 ghi chú ra được Google Doc đúng form, ≤ 2 trang, người dùng chỉ phải sửa nhẹ.
