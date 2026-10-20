# Kế hoạch thực hiện công việc – SiteVoice

> [!IMPORTANT]
> Từ 2026-10-19, quy tắc context nằm tại [AGENTS.md](../AGENTS.md). Ghi kết quả task có ý nghĩa, lỗi/blocker quan trọng và quyết định hoãn/hủy vào [WORK_LOG.md](WORK_LOG.md); không log từng thao tác nhỏ.
> [Nhật ký cũ](nhat-ky-thuc-hien.md) được giữ để tra cứu, không ghi song song. Trạng thái hiện tại: [SUMMARY.md](SUMMARY.md).

---

## Mục lục

- [Quy tắc chung](#quy-tắc-chung)
- [Giai đoạn 0 – Chuẩn bị](#giai-đoạn-0--chuẩn-bị-không-code)
- [Giai đoạn 1 – Lõi chạy được](#giai-đoạn-1--lõi-chạy-được)
- [Giai đoạn 2 – Ra tài liệu](#giai-đoạn-2--ra-tài-liệu)
- [Giai đoạn 3 – Chạy không cần mạng](#giai-đoạn-3--chạy-không-cần-mạng)
- [Mẫu ghi log](#mẫu-ghi-log)

---

## Quy tắc chung

| # | Quy tắc |
|---|---------|
| 1 | Làm **tuần tự** từ trên xuống. Không nhảy bước. |
| 2 | Mỗi task xong phải **chạy được trên điện thoại thật** (trừ task tài liệu/chuẩn bị). |
| 3 | Ghi kết quả task có ý nghĩa vào `docs/WORK_LOG.md` theo AGENTS; cập nhật trạng thái task thực tế và SUMMARY khi cần. Không log từng thao tác nhỏ. |
| 4 | Nếu thay đổi thiết kế → cập nhật file docs liên quan + thêm file `docs/quyet-dinh/` nếu là quyết định lớn. |
| 5 | Không làm thêm chức năng ngoài task đang làm. |
| 6 | Chuỗi hiển thị tiếng Việt (strings.xml); tên biến/hàm/class tiếng Anh. |
| 7 | API key không bao giờ ghi log, không commit vào git. |
| 8 | **Thiết kế UI:** Bắt buộc áp dụng skill `design-taste-frontend` (`.agents/skills/design-taste-frontend/SKILL.md`) cho mọi màn hình Compose / App Screens (tinh gọn, chống AI-slop, không gradient/shadow thừa, kiểu chữ rõ ràng, độ tương phản cao, phản hồi trạng thái chuẩn). |

---

## Giai đoạn 0 – Chuẩn bị (không code)

### 0.1 Thử luồng thủ công
| | |
|---|---|
| **Mô tả** | Dùng dữ liệu ghi chú công trường thực tế → chạy thử prompt từ `templates/nhat-ky-cong-truong/prompt.md` → đánh giá độ dài và ngân sách dòng. |
| **Người làm** | Agent |
| **Đầu vào** | Prompt có sẵn trong `templates/nhat-ky-cong-truong/prompt.md` |
| **Kết quả** | Chạy thử 2 kịch bản (ngày tải cao & ngày mưa bão), đo đạc số dòng trung bình 10–12 dòng nội dung |
| **Trạng thái** | ✅ Hoàn thành |

### 0.2 Chốt prompt & ngân sách ký tự
| | |
|---|---|
| **Mô tả** | Dựa trên kết quả thử và phân tích file gốc `Nhat_ky_thi_cong.docx`, chỉnh `schema.json` (số dòng, ký tự/dòng). |
| **Người làm** | Agent |
| **Đầu vào** | Kết quả từ 0.1 và cấu trúc `Nhat_ky_thi_cong.docx` |
| **Kết quả** | Chốt ngân sách `cong_viec_thi_cong` tối đa 15 dòng (100 kt/dòng), `nghiem_thu` 6 dòng, `an_toan` 3 dòng, các ý kiến 200 ký tự |
| **Trạng thái** | ✅ Hoàn thành |

### 0.3 Chuẩn bị Google Doc mẫu
| | |
|---|---|
| **Mô tả** | Tạo Google Doc mẫu với đầy đủ placeholder `{{...}}` theo `templates/nhat-ky-cong-truong/doc-mau.md`. Dán link vào `doc-mau.md`. In thử kiểm tra ≤ 2 trang. |
| **Người làm** | Agent + Người dùng |
| **Đầu vào** | `templates/nhat-ky-cong-truong/Nhat_ky_thi_cong.docx` |
| **Kết quả** | Đã tạo `Nhat_ky_thi_cong_mau.docx` và lưu link Google Doc mẫu vào `doc-mau.md` |
| **Trạng thái** | ✅ Hoàn thành |

### 0.4 Cài đặt môi trường phát triển
| | |
|---|---|
| **Mô tả** | Cài Android Studio, bật gỡ lỗi USB trên điện thoại, kiểm tra kết nối `adb devices`. |
| **Người làm** | Agent + Người dùng |
| **Đầu vào** | Không |
| **Kết quả** | Android SDK, Adoptium JDK 17 LTS, kết nối ADB thành công với điện thoại thật |
| **Trạng thái** | ✅ Hoàn thành |

---

## Giai đoạn 1 – Lõi chạy được

### 1.1 Tạo dự án Android rỗng

#### 1.1.1 Khởi tạo project Kotlin + Compose
| | |
|---|---|
| **Mô tả** | Tạo project Android mới tại `app/`, gói `com.sitevoice.app`, Kotlin + Jetpack Compose, `minSdk = 26`, `targetSdk = 34`. |
| **Người làm** | Agent |
| **Đầu vào** | Không |
| **Kết quả** | Gradle 8.10.2 + AGP 8.7.0 + JDK 17, `.\gradlew.bat assembleDebug` build thành công ra APK |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.1.2 Chạy trên điện thoại thật
| | |
|---|---|
| **Mô tả** | Cài app lên điện thoại, hiển thị màn hình "Hello SiteVoice". |
| **Người làm** | Agent + Người dùng |
| **Đầu vào** | Project từ 1.1.1 |
| **Kết quả** | Cài đặt `app-debug.apk` thành công qua ADB và khởi chạy trực tiếp trên điện thoại |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.1.3 Thiết lập cấu trúc thư mục source
| | |
|---|---|
| **Mô tả** | Tạo các package theo kiến trúc: `ui/`, `audio/`, `stt/`, `llm/`, `template/`, `export/`, `data/`, `queue/`, `settings/`. Thêm dependency cơ bản: Compose, Room, WorkManager, EncryptedSharedPreferences. |
| **Người làm** | Agent |
| **Đầu vào** | `docs/03-kien-truc.md` |
| **Kết quả** | Tạo đầy đủ 9 package, cấu hình Room, WorkManager, EncryptedSharedPreferences, compile thành công |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.1.4 Copy templates vào assets
| | |
|---|---|
| **Mô tả** | Copy thư mục `templates/nhat-ky-cong-truong/` vào `app/src/main/assets/templates/`. Viết hàm đọc file từ assets (chưa cần parse). |
| **Người làm** | Agent |
| **Đầu vào** | `templates/nhat-ky-cong-truong/` |
| **Kết quả** | TemplateReader và Unit Test `TemplateAssetTest` đọc thành công file assets |
| **Trạng thái** | ✅ Hoàn thành |

---

### 1.2 Nút nhấn giữ ghi âm

#### 1.2.1 Xin quyền RECORD_AUDIO
| | |
|---|---|
| **Mô tả** | Thêm quyền `RECORD_AUDIO` vào manifest, viết logic xin runtime permission khi mở app lần đầu. |
| **Người làm** | Agent |
| **Đầu vào** | Không |
| **Kết quả** | Khai báo Manifest và Compose permission launcher xử lý cấp quyền micro |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.2.2 UI nút ghi âm (nhấn giữ)
| | |
|---|---|
| **Mô tả** | Compose: một nút tròn lớn giữa màn hình. Nhấn giữ → ghi, thả ra → dừng. Hiển thị trạng thái (đang ghi / sẵn sàng). |
| **Người làm** | Agent |
| **Đầu vào** | Quyết định `docs/quyet-dinh/0002-mot-nut-ghi-am.md` |
| **Kết quả** | Nút trung tâm 108dp, scale animation, haptic feedback, tuân thủ skill design-taste-frontend |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.2.3 Ghi âm bằng MediaRecorder/AudioRecord
| | |
|---|---|
| **Mô tả** | Module `audio/`: ghi âm thành file (M4A/AAC), lưu vào thư mục app riêng. **Lưu file TRƯỚC khi làm bất cứ gì khác** (quy tắc #1). |
| **Người làm** | Agent |
| **Đầu vào** | Không |
| **Kết quả** | Class `AudioRecorder` ghi âm file M4A chất lượng cao lưu vào `filesDir/audio/` an toàn |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.2.4 Phát lại file âm thanh
| | |
|---|---|
| **Mô tả** | Module `audio/`: phát lại file đã ghi bằng MediaPlayer. UI: nút play/stop nhỏ bên cạnh mỗi ghi chú. |
| **Người làm** | Agent |
| **Đầu vào** | File âm thanh từ 1.2.3 |
| **Kết quả** | Class `AudioPlayer` và UI thẻ bản ghi cho phép nghe lại/dừng bản thu |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.2.5 Hiển thị thời lượng ghi âm
| | |
|---|---|
| **Mô tả** | Khi đang ghi: hiển thị bộ đếm thời gian. Sau khi ghi: hiển thị thời lượng bản ghi. |
| **Người làm** | Agent |
| **Đầu vào** | Không |
| **Kết quả** | Bộ đếm thời gian thực khi nhấn giữ và hiển thị thời lượng giây trên thẻ kết quả |
| **Trạng thái** | ✅ Hoàn thành |

---

### 1.3 STT qua API (Groq Whisper)

#### 1.3.1 Interface SpeechToText
| | |
|---|---|
| **Mô tả** | Module `stt/`: tạo interface `SpeechToText` với hàm `suspend fun transcribe(audioFile: File): Result<String>`. |
| **Người làm** | Agent |
| **Đầu vào** | `docs/quyet-dinh/0001-stt-lai-online-offline.md` |
| **Kết quả** | Interface `SpeechToText` chuẩn hóa trong `com.sitevoice.app.stt` |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.3.2 Cài đặt GroqWhisper
| | |
|---|---|
| **Mô tả** | Class `GroqWhisper` implements `SpeechToText`. Gọi Groq Whisper API (multipart upload file âm thanh). Dùng `initial_prompt` kèm từ điển thuật ngữ. |
| **Người làm** | Agent |
| **Đầu vào** | API docs của Groq, `docs/09-thuat-ngu.md` |
| **Kết quả** | Class `GroqWhisper` kết nối model `whisper-large-v3-turbo`, truyền từ điển thuật ngữ, unit test pass |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.3.3 Màn hình cài đặt API key (cơ bản)
| | |
|---|---|
| **Mô tả** | Module `settings/`: màn hình nhập API key Groq, lưu bằng EncryptedSharedPreferences. Không ghi log key. |
| **Người làm** | Agent |
| **Đầu vào** | Quy tắc #7 |
| **Kết quả** | `SettingsDialog` và `SecurePreferences` lưu trữ AES256 mã hóa an toàn trên máy |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.3.4 Gọi STT sau khi ghi âm
| | |
|---|---|
| **Mô tả** | Sau khi ghi xong → tự động gọi STT → hiển thị văn bản dưới nút ghi âm. Trạng thái Note: RECORDED → TRANSCRIBED. Xử lý lỗi (hiện thông báo, cho thử lại). |
| **Người làm** | Agent |
| **Đầu vào** | 1.2.3 + 1.3.2 |
| **Kết quả** | `RecordScreen` tự động gọi STT, hiển thị loading xoay, thẻ kết quả văn bản nhận diện và nút thử lại |
| **Trạng thái** | ✅ Hoàn thành |

---

### 1.4 Danh sách ghi chú trong ngày (Room)

#### 1.4.1 Entity & DAO cho Note
| | |
|---|---|
| **Mô tả** | Module `data/`: tạo `NoteEntity`, `NoteDao` theo `docs/04-du-lieu.md`. Database class với Room. |
| **Người làm** | Agent |
| **Đầu vào** | `docs/04-du-lieu.md` |
| **Kết quả** | `NoteEntity`, `NoteDao`, `AppDatabase` cấu hình KSP Room SQLite |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.4.2 Repository cho Note
| | |
|---|---|
| **Mô tả** | `NoteRepository`: lấy ghi chú theo ngày, thêm, sửa text, sửa label, xóa (soft delete = DISCARDED). |
| **Người làm** | Agent |
| **Đầu vào** | 1.4.1 |
| **Kết quả** | `NoteRepository` & `DefaultNoteRepository`, unit test pass 100% tại `NoteRepositoryTest.kt` |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.4.3 Tích hợp ghi âm với Room
| | |
|---|---|
| **Mô tả** | Khi ghi âm xong → tạo NoteEntity (status = RECORDED). STT xong → cập nhật text (status = TRANSCRIBED). |
| **Người làm** | Agent |
| **Đầu vào** | 1.2.3 + 1.3.4 + 1.4.2 |
| **Kết quả** | Ghi âm xong lưu ngay vào DB qua `MainViewModel`, cập nhật text sau STT, lưu trữ bền vững |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.4.4 Màn hình danh sách ghi chú
| | |
|---|---|
| **Mô tả** | UI Compose: danh sách ghi chú trong ngày, mỗi item hiển thị: thời gian, văn bản (hoặc "đang chuyển..."), nhãn, nút phát lại. LazyColumn, kéo xuống xem thêm. |
| **Người làm** | Agent |
| **Đầu vào** | 1.4.2 |
| **Kết quả** | Màn hình `NotesListScreen` hiển thị danh sách cuộn mượt, thẻ từng ghi chú, badge phân loại |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.4.5 Sửa văn bản ghi chú
| | |
|---|---|
| **Mô tả** | Bấm vào ghi chú → dialog/bottom sheet cho sửa text. Lưu lại Room. |
| **Người làm** | Agent |
| **Đầu vào** | 1.4.4 |
| **Kết quả** | `EditNoteDialog` cho phép nhập và lưu văn bản chỉnh sửa vào Room qua repository |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.4.6 Xóa ghi chú
| | |
|---|---|
| **Mô tả** | Vuốt hoặc nút xóa → xác nhận → đổi status = DISCARDED (không xóa file âm thanh). |
| **Người làm** | Agent |
| **Đầu vào** | 1.4.4 |
| **Kết quả** | `DeleteConfirmDialog`, cập nhật status DISCARDED, ghi chú ẩn khỏi danh sách, file âm thanh gốc vẫn nguyên |
| **Trạng thái** | ✅ Hoàn thành |

---

### 1.5 Gán nhãn mục bằng LLM

#### 1.5.1 Interface LlmClient
| | |
|---|---|
| **Mô tả** | Module `llm/`: interface `LlmClient` với `suspend fun chat(messages: List<LlmMessage>): Result<String>`. |
| **Người làm** | Agent |
| **Đầu vào** | `docs/05-llm.md` |
| **Kết quả** | Interface `LlmClient` và data class `LlmMessage` hoàn chỉnh |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.5.2 Cài đặt 1 LLM provider (Groq Llama 3.1 8B Instant)
| | |
|---|---|
| **Mô tả** | Class implement `LlmClient` cho provider Groq (llama-3.1-8b-instant). Dùng chung API key Groq an toàn. |
| **Người làm** | Agent |
| **Đầu vào** | API docs Groq |
| **Kết quả** | `GroqLlmClient` gọi API Chat Completions với JSON object format |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.5.3 Logic phân loại ghi chú
| | |
|---|---|
| **Mô tả** | Hàm `classify(id, text)`: xây prompt từ `prompts/phan-loai-ghi-chu.md`, gọi LLM, parse JSON kết quả, chuẩn hóa nhãn mục. |
| **Người làm** | Agent |
| **Đầu vào** | `prompts/phan-loai-ghi-chu.md`, 1.5.2 |
| **Kết quả** | `NoteClassifier`, unit test pass 100% tại `NoteClassifierTest.kt` |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.5.4 Tự động phân loại sau STT
| | |
|---|---|
| **Mô tả** | Sau STT xong → gọi phân loại → Note: TRANSCRIBED → LABELED. Hiển thị chip nhãn trên mỗi ghi chú. |
| **Người làm** | Agent |
| **Đầu vào** | 1.3.4 + 1.5.3 |
| **Kết quả** | Tự động kích hoạt trong `MainViewModel`, cập nhật label và status LABELED trong Room |
| **Trạng thái** | ✅ Hoàn thành |

#### 1.5.5 Đổi nhãn bằng tay
| | |
|---|---|
| **Mô tả** | Bấm vào chip nhãn → dropdown chọn mục mới → cập nhật `label`, `labelSource = user`. |
| **Người làm** | Agent |
| **Đầu vào** | 1.5.4, danh sách mục từ `schema.json` |
| **Kết quả** | DropdownMenu 7 mục trên `NoteItemCard`, lưu `labelSource = user` vào Room |
| **Trạng thái** | ✅ Hoàn thành |

---

## Giai đoạn 2 – Ra tài liệu

### 2.1 Màn hình duyệt cuối ngày

#### 2.1.1 Entity & DAO cho DailyInput
| | |
|---|---|
| **Mô tả** | Tạo `DailyInputEntity`, `DailyInputDao` theo `docs/04-du-lieu.md`. |
| **Người làm** | Agent |
| **Đầu vào** | `docs/04-du-lieu.md` |
| **Kết quả** | `DailyInputEntity`, `DailyInputDao`, `DailyInputRepository` với unit test `DailyInputRepositoryTest` pass 100% |
| **Trạng thái** | ✅ Hoàn thành |

#### 2.1.2 UI nhập thời tiết & nhân lực
| | |
|---|---|
| **Mô tả** | Màn hình duyệt: phần trên nhập thời tiết (3 buổi, nắng/mưa), số ban chỉ huy, số công nhân, danh sách máy móc. Mặc định lấy từ ngày hôm qua. |
| **Người làm** | Agent |
| **Đầu vào** | `templates/nhat-ky-cong-truong/schema.json` phần `nhap_tay` |
| **Kết quả** | `DailyInputScreen` nhập thời tiết chip 3 buổi, nhân lực, máy móc, tự động kế thừa hôm trước |
| **Trạng thái** | ✅ Hoàn thành |

#### 2.1.3 Danh sách ghi chú để chuẩn bị tổng hợp
| | |
|---|---|
| **Mô tả** | Danh sách ghi chú (LABELED), gom theo nhãn. Cho sửa text, đổi nhãn, loại bỏ trước tổng hợp; không có nút duyệt ghi chú riêng và không chuyển Note sang APPROVED. Chỉ duyệt bản xem trước cuối cùng theo quyết định 0006. |
| **Người làm** | Agent |
| **Đầu vào** | 1.4.4 + 1.5.4 |
| **Kết quả** | `NotesListScreen` hiển thị badge số lượng, cho sửa text, đổi nhãn, soft-delete loại bỏ, sẵn sàng cho tổng hợp |
| **Trạng thái** | ✅ Hoàn thành |

#### 2.1.4 Navigation giữa các màn hình
| | |
|---|---|
| **Mô tả** | Thiết lập Navigation: Ghi âm (home) ↔ Danh sách ↔ Duyệt cuối ngày ↔ Cài đặt. Bottom nav hoặc drawer. |
| **Người làm** | Agent |
| **Đầu vào** | Các màn hình từ giai đoạn 1 + 2.1 |
| **Kết quả** | NavigationBar Material 3 tích hợp 3 tab mượt mà kèm Dialog cài đặt API key |
| **Trạng thái** | ✅ Hoàn thành |

---

### 2.2 Tổng hợp LLM theo Mẫu

#### 2.2.1 Module template – parse schema.json
| | |
|---|---|
| **Mô tả** | Module `template/`: đọc `schema.json` từ assets, parse thành data class `TemplateSchema`. Kiểm tra trường bắt buộc. |
| **Người làm** | Agent |
| **Đầu vào** | `templates/nhat-ky-cong-truong/schema.json` |
| **Kết quả** | Data class `TemplateSchema`, unit test pass 100% tại `TemplateSchemaTest.kt` |
| **Trạng thái** | ✅ Hoàn thành |

#### 2.2.2 Xây prompt tổng hợp
| | |
|---|---|
| **Mô tả** | Đọc `prompt.md` từ assets, thay placeholder `{{DANH_SACH_GHI_CHU_THEO_MUC}}`, `{{GIOI_HAN_TU_SCHEMA}}`, `{{TU_DIEN_THUAT_NGU}}`, `{{NGAY}}`. |
| **Người làm** | Agent |
| **Đầu vào** | `templates/nhat-ky-cong-truong/prompt.md` |
| **Kết quả** | `ReportSynthesizer` nhóm các ghi chú theo mục, ghép prompt theo format chuẩn JSON |
| **Trạng thái** | ✅ Hoàn thành |

#### 2.2.3 Gọi LLM tổng hợp
| | |
|---|---|
| **Mô tả** | Gửi prompt → LLM → parse JSON kết quả theo schema. Lỗi parse → thử lại tối đa 2 lần. Tạo `DailyReportEntity` (status = DRAFT). |
| **Người làm** | Agent |
| **Đầu vào** | 2.2.2 + 1.5.2 |
| **Kết quả** | Gọi `LlmClient` trả về JSON, lưu vào `DailyReportEntity` qua `DailyReportRepository` |
| **Trạng thái** | ✅ Hoàn thành |

#### 2.2.4 Kiểm tra ngân sách ký tự
| | |
|---|---|
| **Mô tả** | Đếm ký tự/số dòng mỗi trường so với giới hạn trong schema. Trả danh sách trường vượt. |
| **Người làm** | Agent |
| **Đầu vào** | `docs/05-llm.md` phần ngân sách |
| **Kết quả** | Class `BudgetValidator`, unit test pass 100% tại `BudgetValidatorTest.kt` |
| **Trạng thái** | ✅ Hoàn thành |

#### 2.2.5 Vòng rút gọn
| | |
|---|---|
| **Mô tả** | Trường vượt → gọi `prompts/rut-gon-muc.md` cho từng trường (tối đa 2 vòng). Còn vượt → báo người dùng. |
| **Người làm** | Agent |
| **Đầu vào** | `prompts/rut-gon-muc.md`, 2.2.4 |
| **Kết quả** | Cơ chế tự động rút gọn vòng lặp trong `ReportSynthesizer`, unit test pass 100% |
| **Trạng thái** | ✅ Hoàn thành |

---

### 2.3 Xem trước & Xuất Google Doc

#### 2.3.1 Màn hình xem trước
| | |
|---|---|
| **Mô tả** | Hiển thị nội dung DailyReport: thời tiết, nhân lực, từng mục tổng hợp. Ghi chú "chưa phân loại" hiện riêng (không đưa vào Doc). Cho quay lại sửa dữ liệu và tạo lại bản nháp. Một hành động duyệt cuối cùng lưu nội dung DailyReport.APPROVED trên điện thoại rồi xếp hàng xuất Google Doc; không yêu cầu duyệt ghi chú riêng hay duyệt lại khi retry cùng nội dung. |
| **Người làm** | Agent |
| **Đầu vào** | 2.2.3 |
| **Kết quả** | Màn hình `DailyReportPreviewScreen`, `DailyReportContent`, nút duyệt chuyển trạng thái APPROVED (quyết định 0006), hiển thị riêng mục chưa phân loại |
| **Trạng thái** | ✅ Hoàn thành |

#### 2.3.2 Google Sign-In
| | |
|---|---|
| **Mô tả** | Tích hợp Google Sign-In, xin quyền `drive.file` + `documents`. Lưu tài khoản vào settings. |
| **Người làm** | Agent |
| **Đầu vào** | Google Cloud Console project, `docs/06-google-doc-mau.md` |
| **Kết quả** | `GoogleAuthManager.kt` và nút đăng nhập/đăng xuất trong `SettingsDialog`, lưu tài khoản trong `SecurePreferences` |
| **Trạng thái** | ✅ Hoàn thành |

#### 2.3.3 Cài đặt link Doc mẫu
| | |
|---|---|
| **Mô tả** | Trong Settings: trường nhập link/ID Google Doc mẫu. Lưu vào EncryptedSharedPreferences. |
| **Người làm** | Agent |
| **Đầu vào** | 0.3 |
| **Kết quả** | Trường nhập link Doc mẫu trong Cài đặt, tự trích xuất Doc ID, unit test `SecurePreferencesTest` pass 100% |
| **Trạng thái** | ✅ Hoàn thành |

#### 2.3.4 Copy Doc mẫu bằng Drive API
| | |
|---|---|
| **Mô tả** | Module `export/`: dùng Drive API copy Doc mẫu → tên "Nhật ký thi công yyyy-MM-dd". Trả về ID Doc mới. |
| **Người làm** | Agent |
| **Đầu vào** | 2.3.2 + 2.3.3 |
| **Kết quả** | `GoogleDocsExporter.kt` gọi Drive API v3 copy file, unit test `GoogleDocsExporterTest` pass 100% |
| **Trạng thái** | ✅ Hoàn thành |

#### 2.3.5 Thay placeholder bằng Docs API
| | |
|---|---|
| **Mô tả** | `batchUpdate` với `replaceAllText` cho từng placeholder. Ô tích thời tiết dùng ☑/☐. Danh sách nối bằng xuống dòng + "- ". Placeholder không có dữ liệu → chuỗi rỗng. |
| **Người làm** | Agent |
| **Đầu vào** | `templates/nhat-ky-cong-truong/doc-mau.md`, 2.3.4 |
| **Kết quả** | `PlaceholderMapper.kt` và Docs API v1 batchUpdate trong `GoogleDocsExporter.kt`, unit test pass 100% |
| **Trạng thái** | ✅ Hoàn thành |

#### 2.3.6 Kiểm tra sau xuất + lưu link
| | |
|---|---|
| **Mô tả** | Kiểm tra Doc không còn sót `{{`. Lưu link vào `DailyReport.docUrl`, status = EXPORTED. Hiện nút mở Doc. |
| **Người làm** | Agent |
| **Đầu vào** | 2.3.5 |
| **Kết quả** | Lưu link `docUrl` vào `DailyReportEntity`, cập nhật status `EXPORTED`, hiển thị thẻ mở Google Doc trực tiếp trên trình duyệt |
| **Trạng thái** | ✅ Hoàn thành |

---

## Giai đoạn 3 – Chạy không cần mạng

### 3.1 Hàng đợi WorkManager

#### 3.1.1 SttWorker
| | |
|---|---|
| **Mô tả** | Worker gọi STT, ràng buộc `NetworkType.CONNECTED`. Thất bại → retry (backoff). Thành công → cập nhật Note và tự kích hoạt ClassifyWorker. |
| **Người làm** | Agent |
| **Đầu vào** | `docs/03-kien-truc.md` |
| **Kết quả** | `SttWorker.kt`, tự động retry exponential backoff khi có mạng, unit test pass |
| **Trạng thái** | ✅ Hoàn thành |

#### 3.1.2 ClassifyWorker
| | |
|---|---|
| **Mô tả** | Worker gọi phân loại LLM, chạy sau SttWorker thành công với ràng buộc mạng. |
| **Người làm** | Agent |
| **Đầu vào** | 1.5.3, 3.1.1 |
| **Kết quả** | `ClassifyWorker.kt`, tự động phân loại nền khi có mạng, fallback nhãn an toàn, unit test pass |
| **Trạng thái** | ✅ Hoàn thành |

#### 3.1.3 ExportWorker
| | |
|---|---|
| **Mô tả** | Worker xuất Google Doc, ràng buộc mạng. Retry khi lỗi. |
| **Người làm** | Agent |
| **Đầu vào** | 2.3.4 + 2.3.5 |
| **Kết quả** | `ExportWorker.kt`, xuất Doc và cập nhật status EXPORTED, tự động retry khi mất mạng |
| **Trạng thái** | ✅ Hoàn thành |

#### 3.1.4 Hiển thị trạng thái hàng đợi
| | |
|---|---|
| **Mô tả** | UI: hiện trạng thái (đang chờ mạng / đang xử lý / xong / lỗi) cho mỗi ghi chú. |
| **Người làm** | Agent |
| **Đầu vào** | 3.1.1 + 3.1.2 |
| **Kết quả** | Giao diện `NotesListScreen` hiển thị badge `⏳ Chờ mạng`, `🤖 Đang phân loại`, thông báo tệp âm thanh gốc an toàn; `MainViewModel` đồng bộ qua WorkManager |
| **Trạng thái** | ✅ Hoàn thành |

---

### 3.2 Whisper offline

#### 3.2.1 Tích hợp whisper.cpp
| | |
|---|---|
| **Mô tả** | Thêm dependency whisper.cpp (AAR prebuilt `dev.ffmpegkit-maintained:whisper-android`). Chuyển đổi âm thanh M4A sang WAV 16kHz mono qua `AudioConverter`. Class `WhisperOfflineStt` implements `SpeechToText`. |
| **Người làm** | Agent |
| **Đầu vào** | `docs/quyet-dinh/0001-stt-lai-online-offline.md` |
| **Kết quả** | `AudioConverter.kt`, `WhisperOfflineStt.kt`, nhận diện tiếng Việt offline bằng whisper.cpp trên máy, unit test pass 100% |
| **Trạng thái** | ✅ Hoàn thành |

#### 3.2.2 Logic chọn online/offline
| | |
|---|---|
| **Mô tả** | Có mạng → Groq API. Không mạng → whisper.cpp (nếu người dùng bật trong settings) hoặc chờ mạng. Fallback tự động khi Groq lỗi. |
| **Người làm** | Agent |
| **Đầu vào** | 3.2.1 + 3.1.1 |
| **Kết quả** | `SttWorker.kt` thông minh: kiểm tra kết nối mạng, ưu tiên Groq, tự động chuyển sang whisper offline khi mất mạng nếu model đã tải |
| **Trạng thái** | ✅ Hoàn thành |

#### 3.2.3 Quản lý model offline
| | |
|---|---|
| **Mô tả** | Settings: cho người dùng tải/xóa model whisper (Tiny, Base, Small). Hiển thị dung lượng, thanh tiến trình tải. |
| **Người làm** | Agent |
| **Đầu vào** | 3.2.1 |
| **Kết quả** | `WhisperModelManager.kt`, giao diện Phần 4 trong `SettingsDialog.kt` tải model từ HuggingFace với tiến trình % và nút xóa model |
| **Trạng thái** | ✅ Hoàn thành |

---

## Giai đoạn 4 – Mở rộng

### 4.1 Thư viện Mẫu & Tự tạo/căn chỉnh Mẫu (Template Library & Builder)

#### 4.1.1 Cấu trúc Mẫu động & Mẫu sẵn có (Nhật ký công trường & Nhật ký cá nhân)
| | |
|---|---|
| **Mô tả** | Thiết kế cấu trúc lưu trữ Mẫu động (`TemplateEntity` trong Room hoặc cấu hình JSON trong `filesDir/templates/`). Đóng gói sẵn 2 mẫu chuẩn: (1) "Nhật ký công trường" (thi công, vật tư, an toàn, nhân lực, thời tiết) và (2) "Nhật ký cá nhân / Hằng ngày" (công việc đã làm, việc tồn đọng, ghi chú suy nghĩ, bài học kinh nghiệm). |
| **Người làm** | Agent |
| **Đầu vào** | `docs/quyet-dinh/0005-kien-truc-theo-mau.md`, `templates/nhat-ky-cong-truong/schema.json` |
| **Kết quả** | `TemplateItemEntity`, `TemplateDao`, `DefaultTemplateRepository` hỗ trợ CRUD mẫu động từ Room, tự động seed 2 mẫu mặc định (Công trường & Cá nhân), 29/29 unit tests pass 100% |
| **Trạng thái** | ✅ Hoàn thành |

#### 4.1.2 Bộ công cụ Tạo & Căn chỉnh Mẫu (Template Builder & Library UI)
| | |
|---|---|
| **Mô tả** | Giao diện "Thư viện mẫu": xem danh sách các mẫu, nhân bản, chỉnh sửa, xóa mẫu tùy chỉnh. Giao diện "Bộ tạo/căn chỉnh mẫu": người dùng tự đặt tên, mô tả, link Doc mẫu trên Google Drive, tự thêm/sửa/xóa các trường placeholder `{{...}}`, căn chỉnh số dòng tối đa và số ký tự mỗi dòng, nhập prompt hướng dẫn LLM tóm tắt theo phong cách mong muốn. |
| **Người làm** | Agent |
| **Đầu vào** | 4.1.1, nguyên tắc UI `design-taste-frontend` App Screens |
| **Kết quả** | Màn hình `TemplateLibraryScreen` và `TemplateEditorScreen` trực quan, tinh gọn, kiểm tra tính hợp lệ của schema |
| **Trạng thái** | 🔲 Chưa bắt đầu |

#### 4.1.3 Bộ chọn Mẫu (Template Selector) & Tổng hợp theo Mẫu động
| | |
|---|---|
| **Mô tả** | Thành phần `TemplateSelector` trên màn hình chính/chuẩn bị báo cáo: cho phép người dùng tùy chọn mẫu phù hợp (chuyển đổi giữa Nhật ký công trường và Nhật ký cá nhân). Cập nhật `ReportSynthesizer` và `BudgetValidator` nạp động schema và prompt từ Mẫu được chọn thay vì đọc cố định asset. |
| **Người làm** | Agent |
| **Đầu vào** | 4.1.1, 4.1.2 |
| **Kết quả** | Chọn mẫu linh hoạt, tổng hợp LLM tuân thủ chính xác schema động của mẫu được chọn |
| **Trạng thái** | 🔲 Chưa bắt đầu |

#### 4.1.4 Xuất Google Doc theo Mẫu tùy biến
| | |
|---|---|
| **Mô tả** | Xuất Google Doc tương thích với mọi mẫu do người dùng tạo: sao chép template Google Doc riêng của mẫu đó trên Drive của người dùng và batchUpdate thay thế chính xác các placeholder đã định nghĩa trong mẫu. |
| **Người làm** | Agent |
| **Đầu vào** | 4.1.2, 4.1.3, `ExportWorker` |
| **Kết quả** | Xuất tài liệu thành công cho cả Nhật ký công trường, Nhật ký cá nhân và các mẫu do người dùng tự tạo |
| **Trạng thái** | 🔲 Chưa bắt đầu |

---

### 4.2 Ảnh hiện trường kèm ghi chú (Photo & Caption)

#### 4.2.1 Chụp & đính kèm ảnh vào ghi chú
| | |
|---|---|
| **Mô tả** | Thêm trường `photoUri` vào `NoteEntity` (Room migration). Tích hợp launcher chụp ảnh/chọn ảnh, lưu tệp ảnh gốc an toàn vào thư mục riêng của app `filesDir/photos/`. |
| **Người làm** | Agent |
| **Đầu vào** | `docs/04-du-lieu.md` |
| **Kết quả** | Chụp/chọn ảnh lưu cục bộ an toàn, cập nhật entity Room kèm migration test |
| **Trạng thái** | 🔲 Chưa bắt đầu |

#### 4.2.2 Chú thích và hiển thị ảnh trên UI
| | |
|---|---|
| **Mô tả** | Thêm thumbnail ảnh trong thẻ ghi chú, cho phép phóng to xem trước ảnh (dialog/modal) và sửa ghi chú/chú thích ảnh. Áp dụng chuẩn visual design-taste-frontend. |
| **Người làm** | Agent |
| **Đầu vào** | 4.2.1 |
| **Kết quả** | UI thẻ ghi chú có thumbnail ảnh tinh gọn, phản hồi trực quan, xem chi tiết ảnh |
| **Trạng thái** | 🔲 Chưa bắt đầu |

#### 4.2.3 Tải ảnh lên Google Drive & chèn vào Google Doc
| | |
|---|---|
| **Mô tả** | Khi xuất báo cáo, tải ảnh hiện trường lên thư mục Google Drive tương ứng và dùng Docs API chèn ảnh (`insertInlineImage`) vào khu vực placeholder hình ảnh hiện trường. |
| **Người làm** | Agent |
| **Đầu vào** | 4.2.1, 2.3.5 |
| **Kết quả** | Google Doc có ảnh hiện trường và chú thích minh họa sắc nét theo vị trí quy định |
| **Trạng thái** | 🔲 Chưa bắt đầu |

---

### 4.3 Trích xuất việc cần làm (To-do / Action Items) & Chia sẻ nhanh

#### 4.3.1 Prompt LLM trích xuất To-do & Vấn đề tồn đọng
| | |
|---|---|
| **Mô tả** | Tạo prompt LLM trích xuất các công việc phát sinh cần xử lý (To-do), phân định rõ việc tồn đọng, người nhận việc/đơn vị liên quan từ các bản ghi âm trong ngày. |
| **Người làm** | Agent |
| **Đầu vào** | `prompts/`, ghi chú trong ngày |
| **Kết quả** | `TodoExtractor.kt` phân tích danh sách note và trả về cấu trúc to-do rõ ràng |
| **Trạng thái** | 🔲 Chưa bắt đầu |

#### 4.3.2 Quản lý Việc cần làm (To-do Entity & UI)
| | |
|---|---|
| **Mô tả** | Tạo `TodoEntity` và `TodoDao` trong Room DB. Màn hình hoặc Tab "Việc cần làm" hiển thị danh sách to-do với checkbox đánh dấu hoàn thành, cho phép thêm việc thủ công. |
| **Người làm** | Agent |
| **Đầu vào** | 4.3.1 |
| **Kết quả** | Màn hình To-do trực quan, lưu trữ Room bền vững |
| **Trạng thái** | 🔲 Chưa bắt đầu |

#### 4.3.3 Chia sẻ nhanh qua Zalo/Email
| | |
|---|---|
| **Mô tả** | Tính năng 1 chạm: sao chép nhanh tóm tắt công việc / tồn đọng định dạng đẹp hoặc mở Android Share Sheet để gửi ngay cho sếp/đội thợ qua Zalo, Telegram, Email. |
| **Người làm** | Agent |
| **Đầu vào** | 4.3.2 |
| **Kết quả** | Nút Chia sẻ / Copy nhanh trên giao diện, hỗ trợ Android Send Intent |
| **Trạng thái** | 🔲 Chưa bắt đầu |

---

### 4.4 Tìm kiếm & Bộ lọc ghi chú (Search & Filter)

#### 4.4.1 Truy vấn tìm kiếm trong Room
| | |
|---|---|
| **Mô tả** | Cập nhật `NoteDao` bổ sung hàm tìm kiếm theo từ khóa văn bản (FTS hoặc SQL LIKE), lọc theo dải ngày tạo, lọc theo nhãn danh mục (TIEN_DO, VAT_TU, CONG_NHAN, SU_CO,...). |
| **Người làm** | Agent |
| **Đầu vào** | `com.sitevoice.app.data.dao.NoteDao` |
| **Kết quả** | DAO queries tìm kiếm & lọc mượt mà kèm unit test |
| **Trạng thái** | 🔲 Chưa bắt đầu |

#### 4.4.2 UI Tìm kiếm và Filter Chips
| | |
|---|---|
| **Mô tả** | Thêm SearchBar và hàng Filter Chips (Ngày, Danh mục) vào màn hình danh sách ghi chú. Tìm kiếm tức thì theo từ khóa nhập vào, giao diện không giật lag. |
| **Người làm** | Agent |
| **Đầu vào** | 4.4.1 |
| **Kết quả** | UI thanh tìm kiếm và lọc danh mục chuẩn App Screens của design-taste-frontend |
| **Trạng thái** | 🔲 Chưa bắt đầu |

---

## Tổng hợp số lượng task

| Giai đoạn | Số task |
|-----------|---------|
| 0 – Chuẩn bị | 4 |
| 1 – Lõi chạy được | 18 |
| 2 – Ra tài liệu | 13 |
| 3 – Không cần mạng | 7 |
| 4 – Mở rộng | 12 |
| **Tổng** | **54** |

---

## Mẫu ghi log

Quy tắc và cấu trúc ghi mới được duy trì tại [AGENTS.md](../AGENTS.md) và [WORK_LOG.md](WORK_LOG.md), tránh sao chép một mẫu ở nhiều nơi.

- Thêm một mục cuối WORK_LOG theo ngày thực hiện; dùng task ID trong kế hoạch này nếu có.
- Ghi kết quả thực tế, file/module đổi, kiểm tra đã/chưa chạy và lý do, lỗi/cách xử lý, việc còn lại. Lỗi/hoãn/hủy phải ghi đúng trạng thái, không đánh dấu hoàn thành.
- Cập nhật trạng thái task ở đây và lộ trình nếu có thay đổi; cập nhật SUMMARY khi cần bàn giao hoặc trạng thái dự án đổi.
- Khi đổi thiết kế được phép, liên kết tới tài liệu gốc/quyết định tương ứng; không chép lại đặc tả hoặc lịch sử cũ.

Các dòng ngày/tên/kết quả trong mẫu trước đây chỉ là ví dụ, không phải bằng chứng task đã thực hiện. Nhật ký cũ được giữ nguyên bảng và không nhận bản ghi mới.
