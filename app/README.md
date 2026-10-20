# Module ứng dụng Android: SiteVoice (`app/`)

Module chứa toàn bộ mã nguồn ứng dụng Android Native của SiteVoice.

- **Package**: `com.sitevoice.app`
- **Compile SDK**: 34 | **Min SDK**: 26 | **Target SDK**: 34
- **Framework**: Kotlin 2.0.21, Jetpack Compose, Material 3, Room, WorkManager, OkHttp
- **Kích thước APK Debug**: ~11.94 MB

## Cấu trúc Packages

- `audio/`: Quản lý ghi âm âm thanh gốc M4A/AAC (`AudioRecorder`) và phát lại âm thanh (`AudioPlayer`).
- `data/`: Thực thể Room Database (`NoteEntity`, `DailyInputEntity`, `DailyReportEntity`, `TemplateItemEntity`), DAOs và Repositories.
- `export/`: Xử lý OAuth Google Sign-In (`GoogleAuthManager`), trích xuất và ánh xạ placeholder (`PlaceholderMapper`), xuất tệp qua Drive & Docs API (`GoogleDocsExporter`).
- `llm/`: Khách kết nối Groq Chat API (`GroqLlmClient`), phân loại nhãn ghi chú (`NoteClassifier`), và tổng hợp báo cáo đa mẫu (`ReportSynthesizer`).
- `queue/`: Các tác vụ nền với WorkManager (`SttWorker`, `ClassifyWorker`, `ExportWorker`) hỗ trợ retry tự động khi có mạng.
- `settings/`: Lưu trữ mã hóa khóa bí mật và cấu hình người dùng bằng `EncryptedSharedPreferences` (`SecurePreferences`).
- `stt/`: Nhận diện giọng nói STT qua Groq Whisper API (`GroqWhisper`), tích hợp bộ lọc chống hallucination YouTube khi im lặng.
- `template/`: Đọc mẫu assets (`TemplateReader`), quản lý thư viện mẫu (`TemplateRepository`), thực thể dữ liệu báo cáo động (`DailyReportContent`).
- `ui/`: Các màn hình Jetpack Compose theo chuẩn `design-taste-frontend` App Screens:
  - `record/`: Màn hình ghi âm 1 chạm và danh sách ghi chú hôm nay (mới nhất lên trên đầu).
  - `daily/`: Màn hình chuẩn bị cuối ngày, chọn mẫu linh hoạt và xem trước báo cáo kèm nút duyệt 1 lần.
  - `template/`: Màn hình Thư viện Mẫu (`TemplateLibraryScreen`) và Bộ tạo/căn chỉnh Mẫu (`TemplateEditorScreen`).
  - `settings/`: Hộp thoại Cài đặt (`SettingsDialog`).

## Kiểm thử

Chạy kiểm thử cục bộ:
```powershell
.\gradlew.bat :app:testDebugUnitTest
```
Toàn bộ 32/32 unit tests kiểm thử logic lõi đều pass 100%.
