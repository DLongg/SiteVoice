# 07. Lộ trình

Đánh dấu 👉 giai đoạn đang làm. Mỗi bước phải chạy được trên điện thoại thật rồi mới sang bước sau.

## Giai đoạn 0: Chuẩn bị (không code)
- [x] Thử luồng thủ công vài ngày: ghi âm → dán vào chatbot với prompt → chép vào Doc mẫu
- [x] Chốt prompt, ngân sách ký tự, Google Doc mẫu
- [x] Cài Android Studio, bật gỡ lỗi USB, kết nối thiết bị thật

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

## Giai đoạn 3: Chạy không cần mạng
- [x] 9. Hàng đợi WorkManager cho STT, phân loại, xuất (Task 3.1)
- [x] 10. whisper.cpp offline (model tiny/base/small) (Task 3.2)

## Giai đoạn 4: Mở rộng (đang lập kế hoạch)
- [ ] 11. Mẫu thứ hai & Đa mẫu: Báo cáo tuần công trường (`templates/bao-cao-tuan/`, tổng hợp nhiều ngày, xuất Doc tuần) (Task 4.1)
- [ ] 12. Ảnh hiện trường: Chụp/đính kèm ảnh vào ghi chú, hiển thị thumbnail, chèn ảnh vào Google Doc (Task 4.2)
- [ ] 13. Từ ghi chú sang hành động: LLM trích xuất việc cần làm (To-do), quản lý việc, chia sẻ nhanh qua Zalo (Task 4.3)
- [ ] 14. Tìm kiếm & Lọc: Truy vấn từ khóa full-text, lọc theo ngày và danh mục ghi chú (Task 4.4)
