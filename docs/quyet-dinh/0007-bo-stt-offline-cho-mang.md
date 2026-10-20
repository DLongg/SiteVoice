# 0007. Bỏ STT Offline on-device, lưu âm thanh ngoại tuyến và chờ mạng gọi API

Trạng thái: chấp nhận (thay thế một phần Quyết định 0001)
Ngày: 2026-10-20

## Bối cảnh
- Quyết định 0001 từng dự kiến hai phương án STT khi mất mạng: chạy whisper.cpp on-device hoặc chờ có mạng.
- Quá trình thử nghiệm thực tế cho thấy mô hình whisper on-device trên điện thoại có nhiều nhược điểm:
  1. Mô hình nhỏ (Base ~142MB, Small ~466MB) nhận diện tiếng Việt chuyên ngành công trường còn sai sót, thiếu dấu và dễ nhầm từ vựng.
  2. Mô hình lớn hơn (Turbo Q5 ~547MB, Medium ~1.4GB) tốn nhiều bộ nhớ lưu trữ của điện thoại, gây nóng máy và tốn pin khi giải mã on-device.
  3. Cần chuyển đổi M4A sang WAV 16kHz mono tốn thêm tài nguyên CPU/RAM.
- Trong khi đó, mô hình online `whisper-large-v3-turbo` trên Groq LPU có tốc độ cực nhanh (1–2 giây), nhận diện tiếng Việt chính xác gần như tuyệt đối, hỗ trợ `initial_prompt` thuật ngữ công trường và nhận trực tiếp file M4A gốc.

## Quyết định
- **Bỏ hoàn toàn việc chạy STT Offline on-device (whisper.cpp)**: Không nạp mô hình vào điện thoại, không cần tải model hàng trăm MB, không convert M4A sang WAV on-device.
- **Lưu âm thanh ngoại tuyến và chờ mạng**: Khi kỹ sư ghi âm ngoài công trường (kể cả khi không có mạng 4G/Wifi):
  1. Tệp âm thanh gốc `.m4a` luôn được lưu an toàn tuyệt đối trên bộ nhớ thiết bị.
  2. Tạo ghi chú trong Room Database với trạng thái `RECORDED` (Chờ mạng).
  3. Xếp hàng tác vụ vào WorkManager (`SttWorker`) với ràng buộc có mạng (`NetworkType.CONNECTED`).
  4. Ngay khi điện thoại có kết nối mạng trở lại, WorkManager tự động chạy ngầm gọi Groq Whisper STT và Groq LLM phân loại nội dung.
- **Giao diện người dùng**:
  - Tinh gọn màn hình Cài đặt (`SettingsDialog`), bỏ toàn bộ phần tải/xóa mô hình offline cồng kềnh.
  - Trên màn hình danh sách ghi chú (`NotesListScreen`), bản ghi âm chưa có mạng hiển thị trạng thái `⏳ Chờ mạng` kèm ghi chú *"Âm thanh gốc đã lưu an toàn trên máy. Tự động xử lý khi có mạng"*. Nút nghe lại âm thanh gốc vẫn hoạt động bình thường.

## Hệ quả
- Kích thước ứng dụng (APK) giảm hơn 50% (từ 22MB xuống dưới 10MB) do loại bỏ native library `libwhisper.so`.
- Giải phóng hàng trăm MB bộ nhớ điện thoại cho người dùng.
- Độ chính xác văn bản tiếng Việt luôn đạt mức cao nhất (nhờ luôn dùng `whisper-large-v3-turbo`).
- Ứng dụng chạy mượt mà, không nóng máy, tiết kiệm pin tối đa ngoài công trường.
