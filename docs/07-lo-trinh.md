# 07. Lộ trình

Đánh dấu 👉 giai đoạn đang làm. Mỗi bước phải chạy được trên điện thoại thật rồi mới sang bước sau.

## Giai đoạn 0: Chuẩn bị (không code)
- [x] Thử luồng thủ công vài ngày: ghi âm → dán vào chatbot với prompt → chép vào Doc mẫu
- [x] Chốt prompt, ngân sách ký tự, Google Doc mẫu
- [x] Cài Android Studio, bật gỡ lỗi USB, kết nối thiết bị thật
- [ ] Chuẩn bị bài kiểm tra chuẩn: form nhật ký gốc (có chấm/ô tích) dạng Google Doc và DOCX

## Giai đoạn 1: Lõi chạy được
- [x] 1. Dự án Android rỗng chạy trên điện thoại
- [x] 2. Nút nhấn giữ ghi âm, lưu file, phát lại
- [x] 3. STT qua API (Groq), hiển thị văn bản
- [x] 4. Danh sách ghi chú trong ngày, sửa/xóa (Room)
- [x] 5. Gán nhãn mục bằng LLM, đổi nhãn bằng tay

## Giai đoạn 2: Ra tài liệu
- [x] 6. Chuẩn bị cuối ngày: sửa ghi chú + nhập thời tiết, nhân lực (kế thừa ngày trước); không có lượt duyệt riêng
- [x] 7. Tổng hợp LLM theo Mẫu + kiểm tra ký tự + rút gọn
- [x] 8. Xem trước & duyệt 1 lần duy nhất + xuất Google Doc từ mẫu (quyết định 0006)
- [ ] 8b. Thiết lập form tự động (Google Doc + DOCX): nạp form, nhận diện, duyệt, đo sức chứa, sinh Mẫu (docs/10)

## Giai đoạn 3: Chạy không cần mạng
- [x] 9. Hàng đợi WorkManager cho STT, phân loại, xuất (Task 3.1)
- [x] 10. whisper.cpp offline / Hàng đợi lưu âm thanh ngoại tuyến và chờ mạng gọi API Groq Whisper (Task 3.2, Quyết định 0007)

## Giai đoạn 4: Mở rộng
- [x] 11. Thư viện Mẫu & Tự tạo/căn chỉnh Mẫu: Cho phép user tự tạo mẫu, căn chỉnh placeholder/ký tự, lưu vào thư viện; tùy chọn mẫu cho từng loại công việc (Nhật ký công trường vs. Nhật ký hằng ngày cá nhân); xuất Google Doc tương ứng từng mẫu (Task 4.1.1 - 4.1.4)
- [—] 12. Ảnh hiện trường (Task 4.2): Dời sang [08-cong-viec-tuong-lai.md](08-cong-viec-tuong-lai.md) theo yêu cầu người dùng
- [—] 13. Từ ghi chú sang hành động: To-do & chia sẻ Zalo (Task 4.3): Dời sang [08-cong-viec-tuong-lai.md](08-cong-viec-tuong-lai.md) theo yêu cầu người dùng
- [—] 14. Tìm kiếm & Lọc (Task 4.4): Dời sang [08-cong-viec-tuong-lai.md](08-cong-viec-tuong-lai.md) theo yêu cầu người dùng

## Trạng thái hiện tại
✅ Phạm vi trước Yêu cầu thay đổi 01 đã được ghi nhận hoàn thành (Giai đoạn 0, 1, 2, 3 và Nhóm 4.1 của Giai đoạn 4), chờ người dùng kiểm thử và nghiệm thu thực tế ngoài công trường. Phạm vi mới gồm bài kiểm tra chuẩn và bước 8b còn chưa triển khai; cập nhật tài liệu không đồng nghĩa chức năng thiết lập form đã chạy.

Làm 8 (đường cố định, Mẫu mặc định) TRƯỚC 8b; 8b dùng Mẫu mặc định làm bài kiểm tra chuẩn.
