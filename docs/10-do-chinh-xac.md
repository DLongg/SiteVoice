# 10. Nâng cao độ chính xác & Khung kiểm chuẩn (Model Harness & Benchmark)

Tài liệu này quy định chiến lược nâng cao độ chính xác phân loại ghi chú và bóc tách nội dung mà không phụ thuộc vào việc đổi model LLM lớn hơn. Thay vào đó, áp dụng kiến trúc **Model Harness (Giàn giáo bao bọc quanh model)** kết hợp **Bộ dữ liệu vàng kiểm chuẩn (Golden Dataset Benchmark)**.

---

## 1. Nguyên lý cốt lõi

1. **Đo lường trước, tối ưu sau**: Không suy đoán chất lượng phân loại bằng mắt. Mọi thay đổi về prompt, quy tắc hoặc model đều phải chạy qua bộ kiểm chuẩn tự động; thay đổi nào làm tụt điểm thì loại bỏ.
2. **Quy tắc tất định đi trước (Deterministic Fast-Path)**: Quy tắc chạy offline trên máy, tốc độ 0ms, miễn phí, không bao giờ "ảo giác" và giải thích được lý do gán nhãn. Model chỉ xử lý các ca thực sự khó.
3. **Thác nước ưu tiên (Priority Cascade)**: Tầng trên luôn thắng tầng dưới:
   - *Ưu tiên 1*: Nhãn người dùng đã sửa (`labelSource = "user"`).
   - *Ưu tiên 2*: Từ khóa mở đầu trực tiếp (`"Thi công:"`, `"Nghiệm thu:"`, `"An toàn:"`, `"TVGS:"`...).
   - *Ưu tiên 3*: Quy tắc từ vựng độ tin cậy cao (Strong Domain Lexicon).
   - *Ưu tiên 4*: Khung LLM có giàn giáo (Context window + Few-shot cache + Consensus voting).
   - *Ưu tiên 5*: Hậu kiểm Guardrails & Ràng buộc Schema Enum.
4. **Ngưỡng không đối xứng (Asymmetric Thresholding)**: Gán sai nhãn tệ hơn nhiều so với để `chua_phan_loai`. Khi có sự nghi ngờ hoặc bất đồng, hệ thống ưu tiên đưa vào `chua_phan_loai` để người dùng xác nhận 1 chạm.

---

## 2. Pseudo-code Đường ống phân loại (Pipeline)

```kotlin
class HybridClassificationPipeline(
    private val normalizer: TextNormalizer,
    private val splitter: ClauseSplitter,
    private val ruleEngine: DeterministicRuleEngine,
    private val guardrails: SemanticGuardrails,
    private val llmClassifier: NoteClassifier,
    private val correctionCache: UserCorrectionRepository
) {
    suspend fun classify(noteId: Long, rawText: String, surroundingContext: List<NoteEntity> = emptyList()): ClassificationResult {
        // Bước 1: Sửa lỗi nhận dạng giọng nói & chuẩn hóa từ điển
        val cleanText = normalizer.normalize(rawText)

        // Bước 2: Tách câu / phân đoạn mệnh đề
        val clauses = splitter.split(cleanText)

        // Bước 3: Kiểm tra tầng quy tắc đi trước model (Tầng 1 -> 3)
        val ruleResult = ruleEngine.evaluate(cleanText, clauses)
        if (ruleResult != null && ruleResult.confidence >= 0.95) {
            return ruleResult // Thoát ngay, không tốn token mạng
        }

        // Bước 4: Chuẩn bị ngữ cảnh cho LLM (Tầng 4)
        val fewShots = correctionCache.getSimilarCorrections(cleanText, limit = 3)
        val contextPrompt = buildContextPrompt(cleanText, surroundingContext, fewShots)

        // Bước 5: Gọi LLM với ràng buộc Enum và Bỏ phiếu đồng thuận
        val llmResult = llmClassifier.classifyWithConsensus(noteId, contextPrompt)

        // Bước 6: Hậu kiểm Guardrails & Kiểm tra chéo (Tầng 5)
        val finalResult = guardrails.verify(cleanText, llmResult)

        return finalResult
    }
}
```

---

## 3. Cấu trúc Bộ dữ liệu vàng (Golden Benchmark Dataset)

Bộ dữ liệu vàng được lưu tại `app/src/test/resources/golden_dataset.json` gồm 60 câu thực tế từ công trường xây dựng Việt Nam:

```json
[
  {
    "id": 1,
    "text": "Sáng nay đổ bê tông sàn tầng 3 trục A đến D khối lượng khoảng 45 khối",
    "expected_label": "thi_cong",
    "category": "single_intent",
    "notes": "Công việc thi công điển hình kèm khối lượng"
  },
  {
    "id": 2,
    "text": "Nghiệm thu ván khuôn cốt thép dầm sàn tầng 2 đạt yêu cầu chuyển giai đoạn",
    "expected_label": "nghiem_thu",
    "category": "single_intent",
    "notes": "Nghiệm thu đạt yêu cầu"
  },
  {
    "id": 3,
    "text": "Đã đục xong tường tầng 3, TVGS yêu cầu làm lại mạch vữa do bị rỗng",
    "expected_label": "y_kien_tvgs",
    "category": "multi_intent",
    "notes": "Có cả thi công và ý kiến TVGS, ý chính là ý kiến giám sát"
  },
  {
    "id": 4,
    "text": "Nhắc nhở đội thợ cốt pha đeo dây an toàn khi làm việc mép biên tầng 5",
    "expected_label": "an_toan",
    "category": "single_intent",
    "notes": "An toàn lao động"
  },
  {
    "id": 5,
    "text": "Xong rồi, 5 người",
    "expected_label": "thi_cong",
    "category": "context_dependent",
    "notes": "Phụ thuộc ngữ cảnh câu trước"
  },
  {
    "id": 6,
    "text": "Thời tiết hôm nay thế nào nhỉ",
    "expected_label": "chua_phan_loai",
    "category": "ambiguous",
    "notes": "Không đủ thông tin để gán mục nghiệp vụ"
  }
]
```

### Phân bổ danh mục trong Golden Dataset:
- `single_intent` (30 câu): Các câu đơn ý điển hình cho từng danh mục (`thi_cong`, `nghiem_thu`, `an_toan`, `y_kien_cdt`, `y_kien_tvgs`, `y_kien_nha_thau`).
- `multi_intent` (15 câu): Câu gộp nhiều ý (thi công + nghiệm thu, thi công + ý kiến...).
- `dialect_and_typo` (5 câu): Chứa từ viết tắt, từ lóng công trường, lỗi nghe nhầm âm thanh ("đúc/đục", "TVGS", "BCH", "mác 300").
- `context_dependent` (5 câu): Câu ngắn phụ thuộc ngữ cảnh lân cận.
- `ambiguous` (5 câu): Câu mập mờ, bắt buộc hệ thống phải đưa vào `chua_phan_loai`.

---

## 4. Chỉ số đo lường (Metrics)

Script kiểm chuẩn `ClassificationBenchmarkTest.kt` tự động chạy và tính toán:

1. **Overall Accuracy**:
   $$\text{Accuracy} = \frac{\text{Số câu gán nhãn chính xác}}{\text{Tổng số câu trong bộ dữ liệu}} \times 100\%$$
   - *Mục tiêu*: Baseline hiện tại $\ge 75\% \to$ Sau khi có Harness $\ge 95\%$.

2. **Precision & Recall từng danh mục**:
   - $\text{Precision} = \frac{TP}{TP + FP}$ (Tránh gán nhầm sang danh mục khác).
   - $\text{Recall} = \frac{TP}{TP + FN}$ (Tránh bỏ sót câu thuộc danh mục).

3. **Fast-Path Ratio (Tỷ lệ xử lý không cần mạng)**:
   - Đo tỷ lệ % số câu được giải quyết chính xác ngay tại Tầng 1, 2, 3 bằng quy tắc tất định mà không cần gửi request lên LLM.
   - *Mục tiêu*: Đạt $\ge 60\%$, giúp tiết kiệm chi phí token và phản hồi tức thì 0ms.

4. **Ma trận nhầm lẫn (Confusion Matrix)**:
   - Hiển thị bảng lưới trực quan phát hiện cặp nhãn thường xuyên bị phân loại sai (ví dụ `thi_cong` bị nhầm sang `nghiem_thu` hay ngược lại) để bổ sung từ khóa rào chắn.
