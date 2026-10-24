# 10. Thiết lập form tự động

Mục tiêu: người dùng nạp form của họ MỘT LẦN; app tự đặt placeholder, tự sinh Mẫu; người dùng chỉ duyệt.

## Đầu vào
- Google Doc: chọn bằng Google Picker (cấp quyền `drive.file` cho đúng file được chọn).
- DOCX từ máy: chọn bằng bộ chọn file hệ thống, app tải lên Drive và chuyển thành Google Doc (`mimeType = application/vnd.google-apps.document`), rồi xử lý như Google Doc.
- Không hỗ trợ: PDF, ảnh chụp, form có nội dung nằm trong hộp văn bản/hình vẽ. Phát hiện thì báo rõ lý do.
- Luôn làm việc trên BẢN SAO; không bao giờ sửa file gốc của người dùng.

## Luồng 7 bước
1. Nạp form -> tạo bản sao làm việc trên Google Drive (Drive API v3 `files.copy`), tuyệt đối không sửa file gốc của người dùng.
2. Đọc cấu trúc tài liệu bằng Docs API `documents.get`: các đoạn văn (`paragraph`), bảng (`table`), hàng (`tableRow`), ô (`tableCell`), và chỉ số vị trí (`startIndex`/`endIndex`).
3. Code quét tìm ỨNG VIÊN chỗ điền theo tập tín hiệu tất định (xem "Tín hiệu tìm ứng viên").
4. LLM hỗ trợ 2 pha (xem "Việc của model"):
   - Pha 4a: Đặt tên gợi ý, chọn loại dữ liệu, gán nguồn và độ tin cậy cho từng ứng viên.
   - Pha 4b: Tự sinh bản Prompt thống nhất cho Pha 2 dựa trên danh sách thẻ neo đã được người dùng chốt.
5. Đo sức chứa để tính toán giới hạn dòng/ký tự cho từng chỗ điền (xem "Đo sức chứa").
6. Quy trình Human-in-the-loop 2 bước (xem "Màn hình duyệt thiết lập"):
   - Bước 1: Người dùng duyệt, bật/tắt và chỉnh sửa danh sách thẻ neo `{{MA}}`.
   - Bước 2: Người dùng xem trước/chỉnh sửa Prompt AI thống nhất và điều chỉnh thanh trượt Nhiệt độ (`0.0 - 0.8`, mặc định `0.2`).
7. Ghi placeholder vào bản sao Google Doc bằng Docs API `batchUpdate`, lưu Mẫu vào CSDL Room (hỗ trợ schema song ngữ). Từ đây xuất file hằng ngày chạy theo `06-google-doc-mau.md`.

## Tín hiệu tìm ứng viên (do CODE thực hiện, không do model)
Bộ quét `FormCandidateScanner` áp dụng các biểu thức chính quy (Regex) và cấu trúc phân tích tài liệu sau:

| Tín hiệu | Biểu thức / Tiêu chí | Loại gợi ý | Xử lý chi tiết |
|---|---|---|---|
| **Đề mục mở rộng** | Regex số La Mã (`I.`, `II.`, `III.`), Chữ cái (`A.`, `B.`, `C.`), Mục con (`1.1`, `3.1`), Số thứ tự (`1.`, `2.`), hoặc dòng kết thúc bằng dấu `:` | Tiêu đề ngữ cảnh | Dùng làm `tieuDeGanNhat` gán ngữ cảnh cho các chỗ điền nằm bên dưới. |
| **Ô bảng trống / chấm** | Ô bên phải cùng hàng trong bảng (`tableCell`) có nội dung trống hoặc chỉ chứa dấu chấm | Theo ngữ cảnh nhãn | **Truyền nhãn bảng**: Tự động lấy nhãn văn bản từ ô bên trái làm `truoc` và nhãn gợi ý cho ô bên phải. |
| **Gộp dòng chấm liên tiếp** | Nhiều dòng chấm (`.....`) hoặc gạch dưới (`_____`) liên tiếp nằm dưới cùng một đề mục | `danh_sach` | **Gộp thành 1 chỗ điền duy nhất**: Đếm số dòng (`soDongLienTiep = count`), gộp phạm vi từ `startIndex` dòng đầu đến `endIndex` dòng cuối. |
| **Tách cụm chấm trên cùng dòng** | Nhiều cụm `...` xuất hiện trên cùng một đoạn (inline dots) | `ngay` / `so` / `van_ban` | **Bảo tồn & tách riêng**: Ví dụ `Ngày ... tháng ... năm ...` được tách thành 3 ứng viên riêng biệt (`NGAY`, `THANG`, `NAM`) với ngữ cảnh trước/sau tương ứng. |
| **Ký tự ô tích** | `☐`, `☑`, `[ ]`, `[x]` | `o_tich` | Nhận diện ô lựa chọn trạng thái thời tiết, an toàn... |
| **Đoạn trống dưới tiêu đề** | Đoạn văn không có chữ ngay sau một tiêu đề đề mục | `danh_sach` (nếu >= 2 dòng) hoặc `van_ban` | Gộp các đoạn trống liên tiếp thành 1 chỗ điền danh sách. |

### Quy tắc xử lý đặc thù của Code:
1. **Gộp dòng chấm/gạch liên tiếp**: Khi phát hiện chuỗi các đoạn chấm hoặc gạch dưới nối tiếp nhau dưới cùng một tiêu đề, code không tạo rời rạc từng dòng mà gom thành đúng một thẻ duy nhất với `loaiGoiY = "danh_sach"`, số dòng ghi nhận bằng tổng số dòng chấm để tính ngân sách sức chứa.
2. **Bảo tồn cụm chấm nội dòng**: Không được gộp các cụm `...` nằm xen kẽ chữ trên cùng một dòng (như ngày/tháng/năm, giờ/phút, khoảng cách km).
3. **Khử trùng lặp mã thẻ (`ma`)**: Sau khi phát hiện và gán nhãn, bộ lọc kiểm tra tính duy nhất của mã thẻ. Nếu phát hiện trùng lặp (ví dụ hai chỗ đều có mã `CONG_VIEC`), hệ thống tự động thêm hậu tố phân biệt (`CONG_VIEC_1`, `CONG_VIEC_2`) để tránh xung đột khi thay thế trên Google Doc.

## Việc của model (bước 4)
Model tham gia vào 2 pha chuyên biệt:

### Pha 4a: Gán nhãn & mô tả ứng viên ban đầu
- **Đầu vào**: Danh sách ứng viên do code phát hiện (chỉ số, ngữ cảnh trước `truoc`, ngữ cảnh sau `sau`, tín hiệu, tiêu đề gần nhất).
- **Đầu ra**: JSON gán nhãn cho từng ứng viên gồm: `ten` (tên hiển thị), `loai` (`danh_sach`, `van_ban`, `so`, `ngay`, `o_tich`), `nguon` (nhãn ghi chú công trường hoặc `nhap_tay`), `bo_qua` (true nếu là khối ký tên/tiêu ngữ không cần điền), `do_tin_cay`.
- **Ràng buộc**: Model KHÔNG được tạo thêm ứng viên mới và KHÔNG được làm lệch vị trí `startIndex`/`endIndex`.

### Pha 4b: Tự sinh Prompt thống nhất cho Pha 2 (`generateUnifiedPrompt`)
Sau khi người dùng đã duyệt và chốt danh sách thẻ neo ở Bước 1, AI tự động sinh một bản System Prompt hoàn chỉnh gồm 3 phần:
1. **Quy tắc biên tập văn phong**: Hướng dẫn LLM tổng hợp viết lại các câu khẩu ngữ rời rạc ngoài hiện trường thành văn phong hành chính kỹ thuật gãy gọn, mạch lạc; tuyệt đối bám sát sự thật, bảo tồn 100% số liệu thực tế (khối lượng, kích thước, tầng, trục, máy móc), không tự bịa đặt.
2. **Mô tả chi tiết từng thẻ neo `{{MA}}`**: Định nghĩa rõ ngữ nghĩa, nguồn bóc tách thông tin và yêu cầu định dạng cho từng thẻ neo được kích hoạt.
3. **Cấu trúc JSON đích khớp 1-1**: Chỉ định rõ schema JSON đầu ra chỉ chứa đúng các trường tương ứng với danh sách thẻ neo của Mẫu, đảm bảo `ReportSynthesizer` bóc tách dữ liệu chính xác.

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

## Màn hình duyệt thiết lập (Quy trình Human-in-the-loop 2 bước)
Giao diện hộp thoại thiết lập biểu mẫu (`FormSetupDialog`) được thiết kế tinh gọn theo chuẩn App Screens (`design-taste-frontend`), chia thành 2 bước rõ ràng với Stepper trực quan:

### Bước 1: Duyệt & tinh chỉnh thẻ neo chỗ điền (`STEP_1_TAGS_REVIEW`)
- **Tên mẫu tài liệu**: Ô nhập tên Mẫu mới (mặc định lấy theo tên tài liệu gốc).
- **Thẻ bản sao an toàn trên Google Drive**:
  - Nút `📄 Mở & gõ sửa trên Docs ↗`: Mở bản sao trực tiếp trên trình duyệt để người dùng kiểm tra hoặc chèn thêm dấu chấm.
  - Nút `🔄 Quét lại`: Đọc lại cấu trúc tài liệu mới nhất sau khi người dùng sửa trên Google Docs mà không phải nạp lại từ đầu.
- **Danh sách thẻ neo đã phát hiện** (`LazyColumn`):
  - Hiển thị từng thẻ dưới dạng Card phẳng 2 trạng thái (Thu gọn và Mở rộng chỉnh sửa).
  - Checkbox bật/tắt: Cho phép loại bỏ ngay các chỗ không cần điền (như khối chữ ký) bằng cách đánh dấu `boQua = true`.
  - Chỉnh sửa trực tiếp: Mã thẻ `ma` (tự động viết hoa, VD: `CONG_VIEC_THI_CONG`), tên trường `ten`, loại dữ liệu (`danh_sach`, `van_ban`, `so`, `ngay`, `o_tich`), và danh mục nguồn dữ liệu bóc tách.
- **Thanh điều hướng Bước 1**:
  - Nút "Hủy": Đóng hộp thoại và hủy bỏ phiên làm việc.
  - Nút "Tiếp tục: Sinh Prompt AI →": Lọc danh sách thẻ neo hợp lệ đã kích hoạt (`!boQua && ma.isNotBlank()`), gọi AI sinh Prompt thống nhất và tự động chuyển sang Bước 2.

### Bước 2: Duyệt Prompt thống nhất & Cài đặt Nhiệt độ AI (`STEP_2_PROMPT_REVIEW`)
- **Thanh trượt Nhiệt độ biên tập (Temperature Slider)**:
  - Cho phép người dùng trực tiếp điều chỉnh nhiệt độ cho Mẫu này trong dải `0.0f` đến `0.8f` (bước nhảy `0.05f`, mặc định `0.20f`).
  - Badge hiển thị giá trị số trực quan kèm gợi ý phân mức phong cách:
    * `0.0 - 0.1`: Bám sát nguyên văn từng chữ, tối thiểu viết lại.
    * `0.2 - 0.3` (Mặc định khuyên dùng): Viết lại mượt mà, xâu chuỗi các câu khẩu ngữ rời rạc thành văn bản trôi chảy, tuyệt đối bảo tồn sự thật và số liệu.
    * `0.4 - 0.8`: Văn phong mượt mà, linh hoạt hơn.
- **Hộp văn bản Prompt thống nhất do AI tự sinh**:
  - Hiển thị toàn văn bản System Prompt do Pha 4b sinh ra (gồm quy tắc biên tập, mô tả thẻ neo và schema JSON đích).
  - Cho phép người dùng gõ sửa trực tiếp từng câu chữ theo ý muốn nghiệp vụ của công trường.
  - Nút `🔄 Sinh lại Prompt`: Cho phép gọi lại LLM nếu muốn tái tạo lại prompt từ đầu.
- **Thanh điều hướng Bước 2**:
  - Nút "← Quay lại Bước 1": Quay lại điều chỉnh danh sách thẻ mà không làm mất trạng thái hay tên Mẫu.
  - Nút "✓ Xác nhận & Tạo Mẫu": Ghi các thẻ placeholder `{{MA}}` vào bản sao Google Doc bằng Docs API `batchUpdate`, đồng thời lưu Mẫu hoàn chỉnh vào Room database.

## Mẫu sinh ra & Tương thích Schema song ngữ
Mẫu được lưu trong CSDL Room (`template_items`) với `schemaJson` tương thích linh hoạt cả 2 chuẩn ngôn ngữ:

```json
{
  "id": "custom_auto_1729780000000",
  "name": "Nhật ký thi công Biểu mẫu A",
  "ten": "Nhật ký thi công Biểu mẫu A",
  "version": "1.0",
  "max_pages": 2,
  "gioi_han_trang": 2,
  "summary_fields": [
    {
      "key": "cong_viec_thi_cong",
      "label": "Công việc thi công",
      "type": "danh_sach",
      "kieu": "danh_sach",
      "placeholder": "{{CONG_VIEC_THI_CONG}}",
      "source": "thi_cong",
      "max_lines": 8,
      "toi_da_dong": 8,
      "max_chars_per_line": 80
    }
  ],
  "truong_tong_hop": {
    "cong_viec_thi_cong": {
      "kieu": "danh_sach",
      "toi_da_dong": 8,
      "ky_tu_moi_dong": 80,
      "toi_da_ky_tu": 640
    }
  }
}
```

### Nguyên tắc tương thích:
- `TemplateSchema.fromJson`: Kiểm tra đồng thời cả khóa tiếng Việt (`ten`, `truong_tong_hop`, `kieu`, `toi_da_dong`...) và khóa tiếng Anh (`name`, `summary_fields` / `summaryFields`, `type`, `max_lines`...). Hỗ trợ cả định dạng `JSONArray` lẫn `JSONObject` map.
- Tuyệt đối không ném ngoại lệ `JSONException` làm sập ngầm ứng dụng hoặc rơi về Mẫu mặc định khi gặp schema sinh từ các phiên bản khác nhau.

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
