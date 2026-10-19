# 08. Công việc tương lai

Chỉ làm sau khi nhật ký công trường chạy tốt và Mẫu thứ hai chứng minh lõi đủ tổng quát.

- A. Nhiều loại tài liệu: báo cáo ngày/tuần/tháng cho sếp, nhật ký cá nhân, bảng chấm công, biên bản họp/hiện trường. Một chuỗi ghi chú sinh nhiều tài liệu cùng lúc.
- B. Tổng hợp theo thời gian: nhật ký ngày → báo cáo tuần → tháng/quý, dựa trên nhật ký đã duyệt. Thống kê ngày mưa, tổng công nhân, hạng mục kéo dài.
- C. Từ ghi chú sang hành động: rút to-do và nhắc việc, gom vấn đề tồn đọng, soạn email/Zalo gửi chủ đầu tư và sếp.
- D. Làm giàu dữ liệu: ảnh hiện trường kèm chú thích vào Doc, tự gắn vị trí/giờ/thời tiết, LLM hỏi lại khi thiếu thông tin.
- E. Tìm kiếm và truy vấn: hỏi đáp trên dữ liệu đã lưu.
- F. Dùng chung nhiều người: nhiều người ghi, một người duyệt và xuất.
- G. Ngành khác: y tế, bán hàng thị trường, kiểm tra chất lượng, nông nghiệp, bảo trì thiết bị.
- H. Hỗ trợ tạo Doc mẫu dễ dàng: người dùng chỉ cần cung cấp file Word (`.doc`/`.docx`) đang dùng; hệ thống phân tích bố cục, nhận diện phần cố định/phần cần điền và tự tạo bản Doc mẫu có placeholder. Phương án khác là công cụ hướng dẫn người dùng chọn vùng cần điền, gán trường dữ liệu và tạo mẫu mà không phải tự gõ placeholder.
  - Mục tiêu: giữ bố cục, bảng biểu và khu vực chữ ký của tài liệu gốc; cho xem trước, chỉnh lại các trường nhận diện và xác nhận trước khi dùng mẫu.
  - Kết quả mong muốn: Google Doc mẫu có placeholder và cấu hình ánh xạ trường tương ứng với bộ Mẫu (`schema.json`, `prompt.md`, `doc-mau.md`), dùng được với luồng xuất bằng bản sao hiện có trong thiết kế. Giữ nguyên file người dùng cung cấp.
  - Chưa triển khai/chưa chọn công cụ. Cần khảo sát khả năng chuyển đổi định dạng, giữ bố cục và kiểm tra số trang bằng tài liệu thực tế; chọn phương án tự động hoặc công cụ hỗ trợ khi lên kế hoạch chi tiết.

Thứ tự đề xuất: Mẫu báo cáo tuần → ảnh hiện trường → to-do/tồn đọng → tìm kiếm → nhiều người.
