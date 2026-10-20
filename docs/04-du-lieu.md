# 04. Dữ liệu (Room)

## Note (ghi chú)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | Long (PK) | |
| createdAt | Long (epoch ms) | giờ ghi |
| date | String (yyyy-MM-dd) | ngày làm việc để gom |
| audioPath | String | file gốc, không bao giờ xóa tự động |
| durationMs | Long | |
| text | String? | văn bản sau STT, người dùng có thể sửa |
| sttSource | String? | groq / openai / whisper_cpp / manual |
| label | String? | khóa mục theo schema, hoặc `unclassified` |
| labelSource | String? | auto / user |
| status | String | RECORDED, TRANSCRIBED, LABELED, DISCARDED; không có bước APPROVED riêng cho Note |
| error | String? | lỗi gần nhất |

## DailyInput (dữ liệu cố định cuối ngày)
date (PK), weatherMorning, weatherAfternoon, weatherEvening, commandStaffCount, workerCount, equipmentList (JSON). Mặc định lấy từ ngày trước.

## DailyReport (bản tổng hợp)
| Cột | Ghi chú |
|---|---|
| date (PK) | |
| templateId | ví dụ `nhat-ky-cong-truong` |
| contentJson | kết quả LLM theo schema của Mẫu; khi duyệt phải lưu đầy đủ nội dung đã duyệt, gồm cả dữ liệu nhập tay trong tài liệu, không chỉ link Google Doc |
| status | DRAFT, APPROVED, EXPORTED |
| docUrl | link Google Doc sau khi xuất |
| updatedAt | |

## Quy ước
- Chỉ DailyReport có trạng thái duyệt; ghi chú không bị loại có thể dùng để tạo bản nháp mà không cần duyệt trước. Quyết định [0006](quyet-dinh/0006-mot-luot-duyet.md).
- Nội dung đã duyệt lưu trên điện thoại trước khi xuất; backup dài hạn chỉ cần nội dung này cùng ngày và thông tin Mẫu cần thiết để phục hồi. Định dạng gói backup chưa chốt, không coi bảng Room này là thiết kế backup đã hoàn chỉnh.
- Dữ liệu ngày chưa duyệt giữ đầy đủ trên điện thoại đến khi duyệt xong, không giới hạn số ngày. Backup chỉ giữ văn bản và thông tin liên quan của các ngày này, không kèm âm thanh. Phạm vi chi tiết ở [yêu cầu lưu và backup](02-yeu-cau.md#lưu-dữ-liệu-và-backup).
- Điện thoại vẫn giữ âm thanh gốc để nghe lại; không tự xóa khi duyệt xong. Backup văn bản không khôi phục được file âm thanh; không coi audioPath của máy cũ là âm thanh có thể nghe trên máy phục hồi.
- Mọi thay đổi schema Room phải có migration.
- Không lưu API key trong Room.

## FormTemplate (Mẫu do app sinh)
| Cột | Ghi chú |
|---|---|
| id (PK) | trùng id Mẫu |
| name | tên hiển thị |
| sourceType | gdoc / docx |
| workingDocId | id Google Doc bản sao làm việc |
| sourceHash | mã băm nội dung form gốc, để phát hiện form đổi |
| schemaJson | schema đầy đủ theo templates/schema-chung.md (gồm cho_dien[]) |
| status | SETUP_DRAFT, READY, NEEDS_RESETUP |
| createdAt | |
DailyReport.templateId trỏ tới FormTemplate.id hoặc Mẫu đóng gói sẵn.
