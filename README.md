# SiteVoice 🎙️🏗️

> **Trợ lý hiện trường thông minh** — Ghi âm giọng nói ngoài công trường, tự động bóc tách đa mục, hoạt động ngoại tuyến an toàn và xuất Google Doc chuẩn mẫu (tối đa 2 trang A4) chỉ với 1 lần duyệt cuối ngày.

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg)](https://android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.0-blue.svg)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Tests](https://img.shields.io/badge/Unit%20Tests-32%2F32%20Pass%20(100%25)-success.svg)](app/src/test/)
[![APK Size](https://img.shields.io/badge/APK%20Size-11.94%20MB-brightgreen.svg)](app/build/outputs/apk/debug/)

---

## 🌟 Tính năng nổi bật

- **Nhấn giữ 1 chạm để ghi âm**: Ghi âm hiện trường tức thì bằng nút nhấn giữ lớn, trực quan, phù hợp môi trường công trường đeo găng tay hoặc đang bận việc.
- **Bảo toàn âm thanh gốc tuyệt đối**: Tệp âm thanh `.m4a` luôn được lưu trữ an toàn trong bộ nhớ riêng của máy, không bao giờ bị xóa tự động.
- **Nhận diện giọng nói STT tiếng Việt chính xác**: Tích hợp Groq Whisper Large-v3-Turbo cho kết quả nhận diện chính xác từng thuật ngữ chuyên ngành thi công (cốp pha, cốt thép, bê tông, mác, dầm, sàn...), kèm bộ lọc chặn đứng hiện tượng bịa từ khi im lặng.
- **Bóc tách & phân bổ đa mục**: Nói gộp nhiều ý trong một bản ghi âm (thời tiết, nhân lực, thi công, nghiệm thu, an toàn) được LLM tự động phân tích và đưa vào đúng từng mục tương ứng trong báo cáo.
- **Chạy ngoại tuyến (Offline-First)**: Ghi âm thoải mái khi mất sóng/ngoài vùng phủ mạng; WorkManager tự động xếp hàng và xử lý ngay khi có kết nối mạng (Wifi/4G).
- **Thư viện Mẫu & Bộ tạo Mẫu (Template Builder)**:
  - Tích hợp sẵn 2 mẫu chuẩn: **Nhật ký thi công công trường** & **Nhật ký hằng ngày cá nhân**.
  - Cho phép người dùng tự tạo mẫu, căn chỉnh placeholder `{{...}}`, ngân sách dòng và prompt LLM.
- **Một lượt duyệt duy nhất (Single Approval)**: Không làm phiền duyệt từng mẩu ghi chú nhỏ; người dùng kiểm tra bản xem trước tổng hợp cuối ngày và duyệt 1 lần trước khi xuất.
- **Xuất Google Doc tự động**: Kết nối Google Drive & Docs API, tự động sao chép Google Doc mẫu và thay thế chính xác các placeholder `{{...}}` theo ngân sách ký tự đã chốt (đảm bảo không bao giờ tràn sang trang 3).

---

## 🏗️ Kiến trúc & Công nghệ

Ứng dụng được xây dựng theo kiến trúc phân tầng rõ ràng (Clean Architecture), tuân thủ nguyên tắc thiết kế **App Screens** (chống "AI-slop", tinh gọn, độ tương phản cao ngoài nắng, phản hồi trạng thái rõ ràng):

- **Ngôn ngữ & Nền tảng**: Kotlin 2.0.21, Android SDK 34 (Min SDK 26).
- **Giao diện**: Jetpack Compose + Material 3, Navigation Compose.
- **Cơ sở dữ liệu cục bộ**: Room Database (SQLite) quản lý ghi chú (`notes`), dữ liệu cuối ngày (`daily_inputs`), báo cáo tổng hợp (`daily_reports`) và thư viện mẫu (`templates`).
- **Hàng đợi tác vụ nền**: AndroidX WorkManager với Exponential Backoff (`SttWorker`, `ClassifyWorker`, `ExportWorker`).
- **Bảo mật**: `EncryptedSharedPreferences` mã hóa API Key và thông tin nhạy cảm.
- **Mạng & AI**: OkHttp 4.12, Groq Whisper API (`whisper-large-v3-turbo`), Groq Chat API (`qwen/qwen3.8-27b`).
- **Tích hợp Google**: `play-services-auth` (Google Sign-In), Google Drive API v3, Google Docs API v1 (`batchUpdate`).

---

## 📁 Cấu trúc thư mục

```text
SiteVoice/
├── app/                                 # Mã nguồn ứng dụng Android
│   ├── src/main/
│   │   ├── java/com/sitevoice/app/
│   │   │   ├── audio/                  # AudioRecorder, AudioPlayer
│   │   │   ├── data/                   # Room Entities, DAOs, Repositories
│   │   │   ├── export/                 # GoogleAuthManager, GoogleDocsExporter, PlaceholderMapper
│   │   │   ├── llm/                    # GroqLlmClient, NoteClassifier, ReportSynthesizer
│   │   │   ├── queue/                  # SttWorker, ClassifyWorker, ExportWorker
│   │   │   ├── settings/               # SecurePreferences
│   │   │   ├── stt/                    # GroqWhisper, SpeechToText
│   │   │   ├── template/               # TemplateReader, TemplateRepository, DailyReportContent
│   │   │   └── ui/                     # Compose Screens (Record, Daily, Templates, Settings...)
│   │   ├── assets/templates/           # Các bộ mẫu đóng gói kèm APK
│   │   └── res/                        # Strings tiếng Việt, Themes, Layouts
│   └── src/test/                       # 32 Unit Tests kiểm thử toàn diện
├── docs/                               # Tài liệu thiết kế, lộ trình, dữ liệu, quyết định kỹ thuật
│   ├── quyet-dinh/                     # Các quyết định kỹ thuật chấp nhận (0001 - 0007)
│   ├── SUMMARY.md                      # Trạng thái hiện tại & bản đồ bàn giao
│   └── WORK_LOG.md                     # Nhật ký phát triển chi tiết theo ngày
├── templates/                          # Mẫu tài liệu (schema.json, prompt.md, doc-mau.md, .docx)
│   ├── nhat-ky-cong-truong/            # Mẫu chuẩn Nhật ký thi công công trường
│   └── nhat-ky-ca-nhan/                # Mẫu chuẩn Nhật ký cá nhân
├── AGENTS.md                           # Quy tắc chỉ đạo cho AI Agent / Pair Programming
└── README.md                           # Tài liệu giới thiệu & hướng dẫn dự án
```

---

## 🚀 Hướng dẫn cài đặt & Chạy ứng dụng

### 1. Yêu cầu môi trường
- Máy tính cài đặt **JDK 17** (khuyến nghị Adoptium Temurin 17 LTS) và **Android Studio Ladybug (2024.2+)** hoặc Android SDK Command-line Tools.
- Điện thoại thật chạy **Android 8.0 trở lên**, đã bật **Tùy chọn cho nhà phát triển** và **Gỡ lỗi qua USB (USB Debugging)**.

### 2. Biên dịch & Cài đặt APK
Tại thư mục gốc dự án:
```powershell
# Chạy toàn bộ Unit Tests
.\gradlew.bat testDebugUnitTest

# Biên dịch APK Debug (sinh file app/build/outputs/apk/debug/app-debug.apk)
.\gradlew.bat assembleDebug

# Cài đặt trực tiếp lên điện thoại kết nối qua cáp USB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Cấu hình ban đầu trong ứng dụng
1. Mở ứng dụng **SiteVoice** trên điện thoại.
2. Cấp quyền **Microphone (Ghi âm)** khi được yêu cầu.
3. Bấm biểu tượng ⚙️ **Cài đặt** ở góc trên bên phải:
   - Nhập **Groq API Key** (lấy miễn phí tại [console.groq.com](https://console.groq.com)).
   - Bấm **"Đăng nhập Google"** để liên kết tài khoản Drive/Docs xuất báo cáo.
   - Kiểm tra **Google Doc Mẫu** đã được cấu hình sẵn cho mẫu Nhật ký thi công.

---

## 📖 Hướng dẫn sử dụng

### 1. Ghi âm ngoài công trường (Tab 1 — Ghi âm)
- **Nhấn và giữ** nút mic tròn màu xanh để nói.
- Nói tự nhiên, có thể gộp nhiều nội dung: *"Hôm nay trời nắng ráo, buổi sáng đội thợ 20 người hoàn thành đổ bê tông sàn tầng 2, buổi chiều nghiệm thu cốt thép dầm..."*
- **Thả tay**: Bản ghi âm được lưu an toàn; hệ thống tự động gọi Whisper và AI phân loại nhãn. Danh sách ghi chú mới nhất luôn nằm trên đầu.
- **Nghe lại hoặc chỉnh sửa**: Bấm vào nút Play để nghe lại âm thanh gốc hoặc sửa trực tiếp văn bản nếu muốn.

### 2. Chuẩn bị cuối ngày & Duyệt báo cáo (Tab 2 — Báo cáo)
- Kiểm tra các thông số cố định: Thời tiết (Sáng / Chiều / Tối), nhân lực chỉ huy, công nhân, máy móc (tự động kế thừa từ ngày hôm trước).
- Có thể chọn mẫu khác (Nhật ký công trường vs. Nhật ký cá nhân) tại thanh chọn mẫu.
- Bấm **"Tổng hợp báo cáo"**: AI sẽ phân tích toàn bộ ghi chú trong ngày, kiểm soát ngân sách dòng và hiển thị màn hình xem trước.
- **Kiểm tra & Duyệt**: Chỉnh sửa trực tiếp nội dung nếu cần, sau đó bấm **"Duyệt & Xuất Google Doc"**.
- Ứng dụng tự động sao chép Doc mẫu trên Drive, điền dữ liệu và cung cấp nút mở trực tiếp file Google Doc hoàn chỉnh.

---

## 🧪 Kiểm thử & Chất lượng

Dự án duy trì bộ kiểm thử tự động toàn diện:
- `BudgetValidatorTest`: Kiểm tra ngân sách ký tự và kiểm soát tràn dòng.
- `GoogleDocsExporterTest` & `PlaceholderMapperTest`: Kiểm tra trích xuất Doc ID, ánh xạ placeholder động đa mẫu và gọi API Google Docs.
- `GroqWhisperTest`: Kiểm tra bộ lọc hallucination và xử lý âm thanh im lặng.
- `ReportSynthesizerTest`: Kiểm tra tổng hợp LLM đa mẫu (Công trường & Cá nhân).
- `NoteRepositoryTest`, `TemplateRepositoryTest`: Kiểm tra tương tác Room Database và migration.

---

## 🤖 Hướng dẫn dành cho AI Agent

Mọi AI Agent khi làm việc với repository này **bắt buộc tuân thủ quy tắc trong [AGENTS.md](AGENTS.md)**:
1. Đọc [AGENTS.md](AGENTS.md) trước, sau đó đọc [docs/SUMMARY.md](docs/SUMMARY.md).
2. Không quét lại toàn bộ repository khi bắt đầu phiên; chỉ tra cứu file tài liệu liên quan.
3. Luôn commit và push liên tục mọi thay đổi dù là nhỏ nhất lên Git remote `origin/main`.
4. Duy trì các quyết định kỹ thuật cốt lõi trong `docs/quyet-dinh/`.
5. Cập nhật [docs/WORK_LOG.md](docs/WORK_LOG.md) và [docs/SUMMARY.md](docs/SUMMARY.md) khi hoàn thành công việc.

---

## 📄 Bản quyền & Tác giả

Phát triển bởi đội ngũ **SiteVoice** — Giải pháp năng suất hiện trường ứng dụng AI thông minh.
Mã nguồn lưu trữ tại: [GitHub Repository](https://github.com/DLongg/SiteVoice).
