# 10. Thiết lập form tự động

Mục tiêu: người dùng nạp form của họ MỘT LẦN; app tự đặt placeholder, tự sinh Mẫu; người dùng chỉ duyệt.

## Đầu vào
- Google Doc: chọn bằng Google Picker (cấp quyền `drive.file` cho đúng file được chọn).
- DOCX từ máy: chọn bằng bộ chọn file hệ thống, app tải lên Drive và chuyển thành Google Doc (`mimeType = application/vnd.google-apps.document`), rồi xử lý như Google Doc.
- Không hỗ trợ: PDF, ảnh chụp, form có nội dung nằm trong hộp văn bản/hình vẽ. Phát hiện thì báo rõ lý do.
- Luôn làm việc trên BẢN SAO; không bao giờ sửa file gốc của người dùng.

## Luồng 7 bước
1. Nạp form  tạo bản sao làm việc (Drive API).
2. Đọc cấu trúc bằng Docs API `documents.get`: đoạn văn, bảng, ô, chỉ số vị trí (`startIndex`/`endIndex`).
3. Code tìm ỨNG VIÊN chỗ điền (xem "Tín hiệu").
4. Gọi LLM (`prompts/nhan-dien-cho-dien.md`): đặt tên, chọn loại, ghép nguồn dữ liệu, loại chỗ không phải điền.
5. Đo sức chứa để ra giới hạn dòng/ký tự từng chỗ (xem "Đo sức chứa").
6. Người dùng duyệt (xem "Màn hình duyệt thiết lập").
7. Ghi placeholder vào bản sao, lưu Mẫu. Từ đây xuất file hằng ngày chạy như `06-google-doc-mau.md`.

## Tín hiệu tìm ứng viên (do code, không do model)
| Tín hiệu | Ví dụ | Loại gợi ý |
|---|---|---|
| Dãy chấm hoặc gạch dưới | `......`, `_____` | `van_ban` hoặc `so` |
| Ký tự ô tích | ``, ``, ``, `[ ]` | `o_tich` |
| Ô bảng trống | ô phải của hàng "Nhân lực" | `van_ban` hoặc `so` |
| Đoạn trống ngay dưới tiêu đề | dưới "3. Công việc thi công" | `danh_sach` hoặc `van_ban` |
| Dòng kết thúc bằng dấu hai chấm | "Chủ đầu tư:" | `van_ban` |
| Nhiều đoạn trống/dòng chấm liên tiếp | 10 dòng chấm | `danh_sach` (gộp thành MỘT chỗ điền) |
| Ngữ cảnh ngày | "Hà Nội, ngày ... tháng ... năm ..." | `ngay` |
Quy tắc gộp: nhiều dòng chấm/đoạn trống liên tiếp thuộc cùng một tiêu đề là MỘT chỗ điền; khi điền, xóa phần thừa.

## Việc của model (bước 4)
Nhận: danh sách ứng viên (chỉ số, chữ trước, chữ sau, tín hiệu) và danh sách tiêu đề. Trả về cho mỗi ứng viên: `ten`, `loai`, `nguon` (nhãn ghi chú hoặc nhập tay), `bo_qua` (true nếu không phải chỗ điền, ví dụ khối chữ ký), `do_tin_cay`. Model KHÔNG được tạo ứng viên mới và KHÔNG được đổi vị trí.

## Ghi placeholder vào Doc (bước 7)
- Với mỗi chỗ điền đã duyệt: `deleteContentRange` (xóa dấu chấm/gạch/ô trống) rồi `insertText` `{{MA}}` tại cùng chỉ số.
- Áp dụng các sửa đổi theo thứ tự chỉ số GIẢM DẦN (từ cuối Doc về đầu) để không lệch vị trí. Gom thành một `batchUpdate`.
- Ô tích: thay ký tự ô tích bằng `{{MA_...}}` (xem schema, mỗi lựa chọn một placeholder hoặc một placeholder gộp).
- Với ô tích thật (checklist) của Google Docs: chuyển thành ký tự `` trước (xóa định dạng danh sách), vì API không điền được trạng thái.
- Giữ nguyên định dạng chữ của đoạn (font, cỡ); chỉ thay nội dung.

## Neo và phát hiện form thay đổi
- Mỗi chỗ điền lưu `neo.truoc` (tối đa 40 ký tự trước) và `neo.sau` (tối đa 40 ký tự sau) để tìm lại khi form bị sửa nhẹ.
- Lưu mã băm nội dung form gốc. Khác mã băm: đề nghị chạy lại thiết lập, chỉ hỏi phần khác biệt.
- Không tìm thấy neo: báo người dùng thiết lập lại phần đó; KHÔNG âm thầm điền sai.

## Đo sức chứa (giải bài toán giới hạn trang)
1. Điền vào bản sao dữ liệu thử dài tối đa (mỗi mục có số dòng/ký tự ở mức đề xuất ban đầu).
2. Xuất PDF (Drive `files.export`, `application/pdf`), đếm số trang (Android `PdfRenderer.pageCount`).
3. Vượt giới hạn trang của Mẫu: giảm giới hạn của mục chiếm nhiều chỗ nhất ~10% rồi lặp (tối đa 6 vòng).
4. Dưới giới hạn: thử tăng nhẹ để tận dụng chỗ trống (tối đa 3 vòng), giữ lại cấu hình vừa nhất.
5. Lưu kết quả vào `gioi_han` của từng chỗ điền; lưu `do_tin_cay_suc_chua`.
6. Dữ liệu thử chỉ có trong bản làm việc, bị xóa sau khi đo.

## Màn hình duyệt thiết lập
- Thẻ "chắc chắn" (độ tin cậy cao): duyệt hàng loạt bằng một nút.
- Thẻ "nghi ngờ": đoạn form được tô sáng, kèm mô tả: `Mục 3  danh sách  từ ghi chú Thi công  tối đa 8 dòng`. Chạm Đúng/Sai/Sửa (đổi tên, loại, nguồn, bỏ qua).
- Cuối cùng: tạo Doc thử điền dữ liệu mẫu để người dùng xem một lần, rồi bấm Xác nhận.
- Nhắc rõ trước khi gửi LLM: nội dung form (không có ghi chú giọng nói) sẽ được gửi tới nhà cung cấp LLM đã chọn.

## Mẫu sinh ra
Lưu trong bộ nhớ app (`filesDir/templates/<id>/`) theo cùng khuôn với `templates/` (xem `templates/schema-chung.md`), kèm: `form_nguon` (id Doc làm việc, mã băm), `cho_dien[]`. Mẫu `nhat-ky-cong-truong` trong repo là Mẫu mặc định và bài kiểm tra chuẩn.

## Bài kiểm tra chuẩn
Chạy bộ nhận diện trên form nhật ký công trường (xem `templates/nhat-ky-cong-truong/doc-mau.md`, nội dung form gốc có chấm/ô tích). Kỳ vọng: nhận đúng  90% trong 13 chỗ điền, không bỏ sót chỗ nào, không tạo chỗ giả ở khối chữ ký. Ghi kết quả vào bảng đo độ chính xác (khi có `10-do-chinh-xac.md`).

## Rủi ro đã biết
- Bảng lồng nhau, cột trộn ô.
- Vùng điền không có dấu hiệu nào (khoảng trắng thuần).
- Form có hộp văn bản/hình vẽ chứa chỗ điền (API khó sửa).
- DOCX có định dạng nâng cao có thể sai lệch khi chuyển sang Google Doc.
Chốt chặn: bước duyệt và Doc thử.

## Câu hỏi còn mở
- Có cho người dùng giữ nhiều Mẫu sinh ra cùng lúc không?
- Có cần xuất kết quả ngược lại thành DOCX tải về không (Drive `files.export`)?
- Ngưỡng `do_tin_cay` để chuyển từ "chắc chắn" sang "nghi ngờ" là bao nhiêu (đề xuất 0.85, chỉnh sau khi đo)?
- Nội dung yêu cầu được cung cấp bị trống ký tự ô tích và dấu so sánh ở mức chính xác 90%/giới hạn 2 trang. Cần xác nhận bộ ký tự và dấu trước khi triển khai; hiện giữ nguyên nội dung được cung cấp trong các tài liệu liên quan.
- `do_tin_cay_suc_chua` được lưu ở cấp Mẫu hay từng chỗ điền, và kiểu dữ liệu/ý nghĩa cụ thể là gì? Khuôn schema được cung cấp chưa định nghĩa trường này.
