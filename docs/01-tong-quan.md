# 01. Tổng quan

## Vấn đề
Cán bộ công trường phải viết Nhật ký thi công hằng ngày theo form cố định, giới hạn 2 trang. Ngoài hiện trường tay bẩn, ồn, vội nên không gõ được; cuối ngày nhớ lại thì thiếu sót.

## Giải pháp
Một nút ghi âm duy nhất. Cuối ngày duyệt các ghi chú, AI tổng hợp vào form, xuất ra Google Doc.

## Người dùng
- Chính: cán bộ kỹ thuật / chỉ huy công trường (một người, một điện thoại Android).
- Sau này: nhiều người cùng ghi, một người duyệt (xem `08-cong-viec-tuong-lai.md`).

## Luồng sử dụng trong ngày
1. Ngoài công trường: nhấn giữ nút ghi, nói, thả ra. App lưu file âm thanh gốc ngay.
2. Có mạng: chuyển văn bản bằng API. Không có mạng: whisper.cpp trên máy hoặc chờ có mạng.
3. App gán nhãn mục cho ghi chú (thẻ nhỏ), người dùng có thể đổi sau.
4. Cuối ngày: màn hình duyệt → sửa chữ, sửa nhãn, bỏ ghi chú không dùng → điền thời tiết, nhân lực.
5. Bấm "Tổng hợp": LLM điền form, app kiểm tra giới hạn ký tự, hiện bản xem trước.
6. Duyệt xong → xuất Google Doc từ mẫu.

## Phạm vi phiên bản đầu
Có: ghi âm, STT qua API, danh sách ghi chú, duyệt cuối ngày, tổng hợp LLM, xuất Google Doc.
Chưa có: offline Whisper, ảnh hiện trường, nhiều người dùng, báo cáo tuần.
