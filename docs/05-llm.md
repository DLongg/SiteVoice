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
4. (Tùy chọn) Sau khi xuất, xuất PDF đếm trang; > 2 trang thì giảm ngân sách và làm lại.

## Chi phí
Ghi chú cả ngày chỉ vài nghìn token; dùng model rẻ cho phân loại, model tốt hơn cho tổng hợp nếu cần.
