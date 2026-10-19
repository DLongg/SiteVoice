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
| status | String | RECORDED, TRANSCRIBED, LABELED, APPROVED, DISCARDED |
| error | String? | lỗi gần nhất |

## DailyInput (dữ liệu cố định cuối ngày)
date (PK), weatherMorning, weatherAfternoon, weatherEvening, commandStaffCount, workerCount, equipmentList (JSON). Mặc định lấy từ ngày trước.

## DailyReport (bản tổng hợp)
| Cột | Ghi chú |
|---|---|
| date (PK) | |
| templateId | ví dụ `nhat-ky-cong-truong` |
| contentJson | kết quả LLM theo schema của Mẫu |
| status | DRAFT, APPROVED, EXPORTED |
| docUrl | link Google Doc sau khi xuất |
| updatedAt | |

## Quy ước
- Mọi thay đổi schema Room phải có migration.
- Không lưu API key trong Room.
