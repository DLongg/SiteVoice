# 08. Công việc tương lai

Chỉ làm sau khi phiên bản hiện tại (Lõi ứng dụng + Đa mẫu + Hàng đợi ngoại tuyến + Xuất Google Doc hoàn chỉnh) đã được người dùng nghiệm thu thực tế ngoài hiện trường.

- A. Nhiều loại tài liệu nâng cao: báo cáo tuần/tháng cho sếp, bảng chấm công, biên bản họp/hiện trường. Một chuỗi ghi chú sinh nhiều tài liệu cùng lúc.
- B. Tổng hợp theo thời gian: nhật ký ngày → báo cáo tuần → tháng/quý, dựa trên nhật ký đã duyệt. Thống kê ngày mưa, tổng công nhân, hạng mục kéo dài.
- C. Từ ghi chú sang hành động (To-do / Action Items): rút to-do và nhắc việc, gom vấn đề tồn đọng bằng LLM, soạn email/Zalo gửi chủ đầu tư và sếp qua Android Share Sheet (dời từ Nhóm 4.3).
- D. Làm giàu dữ liệu bằng hình ảnh: chụp/chọn ảnh hiện trường, lưu cục bộ an toàn, hiển thị thumbnail/phóng to trên ứng dụng, tải lên Google Drive và chèn ảnh vào Google Doc qua Docs API `insertInlineImage` (dời từ Nhóm 4.2).
- E. Tìm kiếm và bộ lọc nâng cao (Search & Filter): tìm kiếm toàn văn theo từ khóa, thanh SearchBar và Filter Chips lọc theo dải ngày, nhãn danh mục (dời từ Nhóm 4.4).
- F. Dùng chung nhiều người: nhiều người ghi, một người duyệt và xuất.
- G. Mở rộng ngành khác: y tế, bán hàng thị trường, kiểm tra chất lượng, nông nghiệp, bảo trì thiết bị.
- H. Hỗ trợ tạo Doc mẫu dễ dàng: người dùng chỉ cần cung cấp file Word (`.doc`/`.docx`) đang dùng; hệ thống phân tích bố cục, nhận diện phần cố định/phần cần điền và tự tạo bản Doc mẫu có placeholder.
  - Mục tiêu: giữ bố cục, bảng biểu và khu vực chữ ký của tài liệu gốc; cho xem trước, chỉnh lại các trường nhận diện và xác nhận trước khi dùng mẫu.
  - Kết quả mong muốn: Google Doc mẫu có placeholder và cấu hình ánh xạ trường tương ứng với bộ Mẫu (`schema.json`, `prompt.md`, `doc-mau.md`), dùng được với luồng xuất bằng bản sao hiện có trong thiết kế. Giữ nguyên file người dùng cung cấp.

Thứ tự ưu tiên khi triển khai tiếp: Ảnh hiện trường → To-do/tồn đọng & chia sẻ Zalo → Tìm kiếm/Bộ lọc → Báo cáo tuần/tháng → Nhiều người dùng.
