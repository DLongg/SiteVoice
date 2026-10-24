# 05. LLM

## Bốn tác vụ
1. **Phân loại ghi chú** (`prompts/phan-loai-ghi-chu.md`): chạy ngay sau STT, model nhẹ/rẻ, phân loại câu/đoạn vào các danh mục công trường.
2. **Tổng hợp theo Mẫu** (`ReportSynthesizer`): chạy cuối ngày hoặc khi người dùng yêu cầu tổng hợp báo cáo. Cấu trúc JSON đích được sinh động 1-1 theo các trường tóm tắt (`summary_fields`) của Mẫu đang kích hoạt, sử dụng `llmTemperature` cấu hình trong `SecurePreferences`.
3. **Rút gọn một mục** (`prompts/rut-gon-muc.md`): khi một trường vượt ngân sách ký tự hoặc số dòng, gọi riêng biệt để thu gọn nội dung.
4. **Thiết lập biểu mẫu tự động** (`FormLlmDescriber`):
   - **Pha 4a**: Nhận diện & gán nhãn chỗ điền (`prompts/nhan-dien-cho-dien.md`), model chất lượng cao, `temperature = 0.0`.
   - **Pha 4b**: Tự sinh bản Prompt thống nhất (`generateUnifiedPrompt`), chốt quy tắc biên tập và cấu trúc JSON 1-1 từ các thẻ neo đã duyệt ở Bước 1.

## Quy tắc gọi & Cấu hình Nhiệt độ (llmTemperature)
- **Định dạng JSON nghiêm ngặt**: Yêu cầu LLM trả về CHỈ JSON theo schema; app tự động lọc bỏ fence ```json trước khi phân tích cú pháp.
- **Xử lý lỗi**: Parse lỗi -> thử lại tối đa 2 lần với lời nhắc điều chỉnh lỗi cú pháp, sau đó báo cho người dùng.
- **Thanh trượt Nhiệt độ (`llmTemperature`)**:
  - Lưu trữ an toàn trong `SecurePreferences` (mã hóa AES-256), cho phép người dùng điều chỉnh từ màn hình Cài đặt (`SettingsDialog`) hoặc trực tiếp ở Bước 2 khi thiết lập Form (`FormSetupDialog`).
  - Dải giá trị: `0.0f` đến `0.8f` (bước nhảy `0.05f`), giá trị mặc định là `0.20f`.
  - Phân mức phong cách tổng hợp:
    * `0.0 - 0.1` (Bám sát nguyên văn): Tôn trọng tối đa lời nói gốc, tối thiểu hóa việc biên tập lại câu chữ, chỉ lọc bỏ từ đệm.
    * `0.2 - 0.3` (Mặc định khuyên dùng - Viết lại mượt mà): Tự động liên kết các câu khẩu ngữ ngắn, rời rạc ngoài hiện trường thành các câu văn kỹ thuật trôi chảy, mạch lạc, dễ đọc; tuyệt đối bảo tồn 100% dữ liệu gốc (số lượng công nhân, chủng loại máy móc, tầng, trục, khối lượng hoàn thành, lỗi nghiệm thu).
    * `0.4 - 0.8` (Trau chuốt linh hoạt): Cho phép model diễn đạt mềm mại hơn, thích hợp cho các báo cáo phong cách tự do hoặc biên bản mô tả chi tiết.
- **Ngữ cảnh & Thuật ngữ**: Prompt luôn kèm từ điển thuật ngữ công trường (`docs/09-thuat-ngu.md`) và phân nhóm ghi chú sơ bộ.
- **Ưu tiên nhãn người dùng**: Nhãn `user` (người dùng tự sửa tay) luôn được ưu tiên hơn nhãn `auto` do AI phân loại.
- **Tổng hợp một lượt duyệt**: Tổng hợp bản nháp từ toàn bộ ghi chú hợp lệ trong ngày; người dùng chỉ duyệt bản xem trước cuối cùng trước khi xuất Google Doc.

## Tổng hợp động theo Mẫu (Dynamic Report Synthesis)
Trong `ReportSynthesizer`, cấu trúc dữ liệu JSON đầu ra không bị viết cứng cố định 7 trường, mà được **xây dựng động khớp 1-1** với danh sách trường tóm tắt (`summary_fields` / `truong_tong_hop`) của `TemplateSchema` đang kích hoạt:
1. **Khởi tạo Prompt người dùng**: Duyệt qua từng trường trong `schema.summaryFields`. Nếu trường có kiểu `danh_sach`, prompt yêu cầu mảng các dòng (`[]`); nếu kiểu `van_ban`, yêu cầu chuỗi văn bản (`""`).
2. **Kế thừa mẫu mặc định**: Đối với Mẫu chuẩn "Nhật ký công trường", cấu trúc gồm: `cong_viec_thi_cong` (danh sách có phân nhóm `3.1`, `3.2`), `nghiem_thu`, `an_toan`, `y_kien_cdt`, `y_kien_tvgs`, `y_kien_nha_thau`, `chua_phan_loai`.
3. **Mẫu tùy chỉnh tự động**: Đối với các Mẫu do người dùng thiết lập, cấu trúc JSON sinh ra bám sát chính xác các mã thẻ neo người dùng đã kích hoạt ở Bước 1 (ví dụ `{"tien_do": [], "vat_tu_ve": [], "nhan_luc_chi_tiet": ""}`).
4. **Truyền nhiệt độ cấu hình**: Giá trị `llmTemperature` từ `SecurePreferences` được truyền trực tiếp vào tham số `temperature` của `LlmClient.chat(..., temperature, responseJson = true)`.

## Cấu trúc Prompt thống nhất cho Mẫu tùy chỉnh (Phase 4b)
Khi người dùng hoàn tất Bước 1 của Form Setup, hàm `FormLlmDescriber.generateUnifiedPrompt` tự động tạo ra bản System Prompt hoàn chỉnh lưu vào `TemplateItemEntity.systemPrompt`:
- **Định danh vai trò**: "Bạn là trợ lý thư ký công trường chuyên nghiệp, chịu trách nhiệm tổng hợp báo cáo hằng ngày theo biểu mẫu..."
- **Nguyên tắc biên tập & Chống bịa đặt (Anti-hallucination)**:
  * Phân bổ đa mục: Đọc kỹ từng câu, bóc tách từng ý trong ghi chú để phân bổ vào đúng trường.
  * Chỉ dùng thông tin có trong bản ghi âm/ghi chú; tuyệt đối không tự suy diễn số liệu, khối lượng hay vị trí.
  * Trường không có thông tin: Trả về mảng rỗng `[]` hoặc chuỗi rỗng `""`.
- **Mô tả chi tiết từng thẻ neo**: Danh sách từng thẻ `{{MA}}` kèm nhãn hiển thị, loại dữ liệu (`danh_sach`/`van_ban`), nguồn ghi chú và giới hạn số dòng.
- **Định dạng JSON 1-1**: Mẫu JSON mẫu chứa đầy đủ các khóa cần xuất, đảm bảo LLM phản hồi đúng cấu trúc.

## Chống bịa
- Chỉ dùng thông tin có trong ghi chú.
- Giữ nguyên số liệu, vị trí, tên hạng mục.
- Không đủ thông tin → để trống/mảng rỗng, không suy đoán.
- Ghi chú không rõ mục → `chua_phan_loai` để người dùng xử lý.

## Ngân sách ký tự (nhật ký công trường, chỉnh sau khi in thử)
Với Mẫu tự sinh, giới hạn do đo sức chứa quyết định (docs/10); bảng dưới chỉ áp dụng cho Mẫu mặc định.

| Mục | Giới hạn |
|---|---|
| 3. Công việc thi công | 8 dòng, mỗi dòng ≤ 110 ký tự (≈ 900 tổng) |
| 4. Nghiệm thu | 4 dòng (≈ 500) |
| 5. An toàn, vệ sinh | 3 dòng (≈ 300) |
| 6. Mỗi ý kiến đơn vị | ≤ 200 ký tự |

Nguồn sự thật là `templates/nhat-ky-cong-truong/schema.json`; bảng này chỉ để đọc.

## Vòng kiểm tra
1. Nhận JSON → đếm ký tự từng trường.
2. Trường vượt → gọi `rut-gon-muc` cho riêng trường đó (tối đa 2 vòng).
3. Còn vượt → báo người dùng chọn bỏ bớt ý.
4. Xuất PDF, đếm trang; vượt giới hạn trang thì giảm giới hạn mục chiếm nhiều chỗ nhất và làm lại (dùng chung cơ chế đo sức chứa ở docs/10).

## Chi phí
Ghi chú cả ngày chỉ vài nghìn token; dùng model rẻ cho phân loại, model tốt hơn cho tổng hợp nếu cần.

## Khung bao bọc phân loại & Kiểm chuẩn độ chính xác (Model Harness)
- Không dựa dẫm hoàn toàn vào LLM cho việc phân loại; bao bọc model bằng đường ống đa tầng:
  1. Chuẩn hóa từ điển & tách mệnh đề trước khi gửi vào LLM.
  2. Tầng quy tắc tất định (nhãn user, cue words, từ vựng mạnh) chạy 0ms offline, giải quyết các ca rõ ràng mà không cần gọi API.
  3. Cửa sổ ngữ cảnh (context window ±2 câu) và dynamic few-shot từ các lần người dùng sửa nhãn.
  4. Hậu kiểm Guardrails, ràng buộc schema Enum và kiểm tra chéo ngữ nghĩa; không chắc chắn thì hạ về `chua_phan_loai`.
- Mọi tối ưu hóa prompt, quy tắc hoặc model đều phải đo lường bằng Bộ dữ liệu vàng (Golden Dataset) và script kiểm chuẩn tự động.
- Chi tiết xem tại [docs/10-do-chinh-xac.md](10-do-chinh-xac.md).
