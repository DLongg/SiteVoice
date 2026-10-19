# 0002. Chỉ một nút ghi âm

Trạng thái: chấp nhận

## Bối cảnh
Ngoài công trường tay bẩn, ồn, vội; chọn mục trước mỗi lần ghi làm chậm và dễ bỏ.

## Quyết định
Một nút nhấn giữ duy nhất. Việc phân loại chuyển sang LLM.

## Rủi ro và cách chặn
- Phân loại sai mục → gán nhãn ngay sau khi ghi, hiển thị thẻ nhãn, cuối ngày duyệt và đổi nhãn.
- Thiếu/bịa → prompt ràng buộc, mục không rõ vào "chưa phân loại".
- Số liệu cố định lẫn vào lời nói → thời tiết và nhân lực nhập ở màn hình duyệt, không qua giọng nói.
- Nghe nhầm thuật ngữ → từ điển thuật ngữ, cho sửa chữ nhanh.
- Khuyến khích (không bắt buộc) nói từ khóa mở đầu: "Thi công: ...", "Nghiệm thu: ...", "An toàn: ...", "Chủ đầu tư: ...", "Giám sát: ...".
