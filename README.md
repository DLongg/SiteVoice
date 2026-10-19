# SiteVoice – Nhật ký công trường

Ghi âm giọng nói ngoài công trường, cuối ngày duyệt, AI tổng hợp thành Nhật ký thi công (tối đa 2 trang) trong Google Doc.

## Bắt đầu
1. Cài Android Studio, bật Gỡ lỗi USB trên điện thoại.
2. Mở thư mục này bằng Android Studio (hoặc Claude Code).
3. Với AI agent: đọc [AGENTS.md](AGENTS.md), sau đó [docs/SUMMARY.md](docs/SUMMARY.md); chỉ mở tài liệu liên quan tới task. Gemini có điểm dẫn tại [GEMINI.md](GEMINI.md).
4. Làm theo `docs/07-lo-trinh.md` từng bước; ghi kết quả có ý nghĩa vào [docs/WORK_LOG.md](docs/WORK_LOG.md) và cập nhật SUMMARY khi trạng thái thay đổi.

## Cấu trúc
`app/`: nơi đặt app Android; `docs/`: thiết kế, quyết định và context; `prompts/`: prompt dùng chung; `templates/`: cấu hình từng loại tài liệu. Xem trạng thái hiện tại trong [SUMMARY](docs/SUMMARY.md) và bảng tài liệu đọc theo nhu cầu trong [AGENTS](AGENTS.md).

## Chưa có mã nguồn
Thư mục `app/` hiện chỉ có README, chưa có source hoặc cấu hình build. Bước 1 trong lộ trình là tạo dự án Android rỗng.
