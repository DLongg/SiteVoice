# SiteVoice — Điểm vào cho Gemini

Đọc [AGENTS.md](AGENTS.md) trước, sau đó [docs/SUMMARY.md](docs/SUMMARY.md) khi bắt đầu hoặc tiếp tục phiên sau reset/compact.

AGENTS.md là nguồn quy tắc chung (bao gồm các ràng buộc sản phẩm trước đây ở file này); SUMMARY là trạng thái hiện tại và bản đồ bàn giao. Chỉ đọc tài liệu liên quan theo bảng trong AGENTS; chỉ tra đoạn WORK_LOG cần thiết.

Tuân thủ workflow kết thúc task và bàn giao trong AGENTS. Ghi lịch sử mới duy nhất vào [docs/WORK_LOG.md](docs/WORK_LOG.md). Đây là instruction, không phải hook tự động; không giả định agent phát hiện được context window.

## Mục tiêu
Người dùng nạp form (Google Doc hoặc DOCX) MỘT lần; app tự đặt placeholder và sinh Mẫu; người dùng chỉ duyệt.

## Công nghệ (thiết kế)
- Xuất: Google Docs API + Drive API (copy bản Doc đã có placeholder, replaceAllText). Thiết lập form: Docs API (documents.get/batchUpdate), Drive API (copy, import DOCX, export PDF để đo trang), Google Picker.

## Cấu trúc thư mục
- templates/<ten-mau>/  mỗi Mẫu gồm schema.json, prompt.md, doc-mau.md; templates/schema-chung.md mô tả khuôn chung (Mẫu app tự sinh lưu ở bộ nhớ app, cùng khuôn)

## Tài liệu chi tiết
| File | Nội dung |
|---|---|
| [AGENTS.md](AGENTS.md) | Quy tắc chung, gồm quy tắc về Mẫu, code tìm chỗ điền và bảo vệ file gốc |
| [docs/10-thiet-lap-form.md](docs/10-thiet-lap-form.md) | Thiết lập form tự động (Google Doc/DOCX) |
