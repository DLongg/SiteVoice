# 01. Tổng quan

## Vấn đề
Cán bộ công trường phải viết Nhật ký thi công hằng ngày theo form cố định, giới hạn 2 trang. Ngoài hiện trường tay bẩn, ồn, vội nên không gõ được; cuối ngày nhớ lại thì thiếu sót.

## Giải pháp
Một nút ghi âm duy nhất. Cuối ngày có thể sửa ghi chú và nhập thông tin bổ sung; AI tổng hợp vào form, người dùng duyệt bản xem trước một lần rồi xuất Google Doc.

## Người dùng
- Chính: cán bộ kỹ thuật / chỉ huy công trường (một người, một điện thoại Android).
- Sau này: nhiều người cùng ghi, một người duyệt (xem `08-cong-viec-tuong-lai.md`).

## Luồng sử dụng trong ngày
1. Ngoài công trường: nhấn giữ nút ghi, nói, thả ra. App lưu file âm thanh gốc ngay.
2. Có mạng: chuyển văn bản bằng Groq Whisper API. Không có mạng: lưu file âm thanh gốc và xếp hàng WorkManager chờ có mạng để tự động xử lý (Quyết định 0007).
3. App gán nhãn mục cho ghi chú (thẻ nhỏ), người dùng có thể đổi sau.
4. Cuối ngày: sửa chữ, sửa nhãn, bỏ ghi chú không dùng nếu cần → điền thời tiết, nhân lực. Đây là bước chỉnh dữ liệu, không phải lượt duyệt riêng.
5. Bấm "Tổng hợp": LLM điền form, app kiểm tra giới hạn ký tự, hiện bản xem trước.
6. Duyệt bản xem trước một lần → lưu nội dung đã duyệt trên điện thoại → xuất Google Doc từ mẫu qua hàng đợi mạng. Chưa duyệt thì giữ bản nháp.

## Phạm vi phiên bản đầu
Có: ghi âm, STT qua API, danh sách ghi chú, duyệt cuối ngày, tổng hợp LLM, xuất Google Doc.
Chưa có: offline Whisper, ảnh hiện trường, nhiều người dùng, báo cáo tuần.
