# 05. LLM

## Ba tác vụ
1. Phân loại ghi chú (`prompts/phan-loai-ghi-chu.md`): chạy ngay sau STT, model nhẹ/rẻ.
2. Tổng hợp theo Mẫu (`templates/<mau>/prompt.md`): chạy cuối ngày.
3. Rút gọn một mục (`prompts/rut-gon-muc.md`): khi vượt ngân sách ký tự.

## Quy tắc gọi
- Yêu cầu LLM trả về CHỈ JSON theo schema; app bỏ fence ```json trước khi parse.
- Parse lỗi → thử lại tối đa 2 lần, rồi báo người dùng.
- temperature thấp (0 đến 0.3).
- Prompt luôn kèm từ điển thuật ngữ (`docs/09-thuat-ngu.md`).
- Gửi kèm nhãn do người dùng đã sửa (nhãn `user` ưu tiên hơn `auto`).
- Tổng hợp bản nháp từ ghi chú trong ngày không bị loại; không yêu cầu duyệt ghi chú trước. Người dùng chỉ duyệt bản xem trước cuối cùng. Mọi tổng hợp/rút gọn làm thay đổi nội dung phải hoàn tất trước lượt duyệt đó.

## Chống bịa
- Chỉ dùng thông tin có trong ghi chú.
- Giữ nguyên số liệu, vị trí, tên hạng mục.
- Không đủ thông tin → để trống/mảng rỗng, không suy đoán.
- Ghi chú không rõ mục → `chua_phan_loai` để người dùng xử lý.

## Ngân sách ký tự (nhật ký công trường, chỉnh sau khi in thử)
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
4. (Tùy chọn) Sau khi xuất, xuất PDF đếm trang; > 2 trang thì báo cần điều chỉnh ngân sách. Nếu làm lại khiến nội dung thay đổi, kết quả mới là bản nháp cần duyệt; không tự sửa bản đã duyệt.

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
