# 0001. Chuyển giọng nói: kết hợp online và offline

Trạng thái: chấp nhận

## Bối cảnh
Cần chuyển giọng nói tiếng Việt thành văn bản, ngân sách thấp, công trường có lúc mất mạng, môi trường ồn.

## Quyết định
- Luôn lưu file âm thanh gốc trước.
- Có mạng: Groq Whisper (miễn phí/rẻ) hoặc OpenAI Whisper (~0,006 USD/phút).
- Không có mạng: whisper.cpp trên máy (model base ~140 MB hoặc small ~460 MB) hoặc chờ có mạng.
- Android SpeechRecognizer KHÔNG dùng làm chính vì bị ngắt khi im lặng, không hợp ghi dài.

## Hệ quả
Cần interface `SpeechToText` để đổi nhà cung cấp. Whisper offline làm ở giai đoạn sau (bước 10).
Mẹo chất lượng: tai nghe/mic cài áo, nói câu ngắn, dùng `initial_prompt` với từ điển thuật ngữ.
