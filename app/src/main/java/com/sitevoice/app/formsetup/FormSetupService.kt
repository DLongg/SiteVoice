package com.sitevoice.app.formsetup

import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.llm.LlmClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class FormSetupAnalysis(
    val workingDocId: String,
    val workingDocUrl: String,
    val originalDocTitle: String,
    val candidates: List<FormCandidate>
)

class FormSetupService(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val llmClient: LlmClient? = null
) {
    companion object {
        private const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
        private const val DOCS_API_BASE = "https://docs.googleapis.com/v1"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Bước 1, 2, 3, 4:
     * - Tạo bản sao làm việc trên Drive (không sửa file gốc)
     * - Đọc cấu trúc bằng Docs API documents.get
     * - Code tìm ứng viên chỗ điền
     * - Mô tả ứng viên (gán nhãn mã, phân loại nguồn)
     */
    suspend fun analyzeForm(
        originalDocId: String,
        accessToken: String
    ): Result<FormSetupAnalysis> = withContext(Dispatchers.IO) {
        try {
            // 1. Tạo bản sao làm việc trên Drive
            val copyPayload = JSONObject().apply {
                put("name", "[SiteVoice Mẫu] Bản sao tự động")
            }
            val copyRequest = Request.Builder()
                .url("$DRIVE_API_BASE/files/$originalDocId/copy")
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .post(copyPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val copyResponse = httpClient.newCall(copyRequest).execute()
            val copyBody = copyResponse.body?.string().orEmpty()
            if (!copyResponse.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Lỗi tạo bản sao Form (${copyResponse.code}): $copyBody")
                )
            }
            val copyJson = JSONObject(copyBody)
            val workingDocId = copyJson.optString("id")
            val workingTitle = copyJson.optString("name", "Bản sao tự động")

            // 2. Đọc cấu trúc tài liệu bằng Docs API
            val getDocRequest = Request.Builder()
                .url("$DOCS_API_BASE/documents/$workingDocId")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val getDocResponse = httpClient.newCall(getDocRequest).execute()
            val getDocBody = getDocResponse.body?.string().orEmpty()
            if (!getDocResponse.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Lỗi đọc cấu trúc tài liệu (${getDocResponse.code}): $getDocBody")
                )
            }
            val docJson = JSONObject(getDocBody)

            // 3. CODE quét tìm ứng viên chỗ điền
            val rawCandidates = FormCandidateScanner.scanDocument(docJson)

            // 4. LLM & Rules mô tả ứng viên
            val describer = FormLlmDescriber(llmClient)
            val describedCandidates = describer.describeCandidates(rawCandidates)

            Result.success(
                FormSetupAnalysis(
                    workingDocId = workingDocId,
                    workingDocUrl = "https://docs.google.com/document/d/$workingDocId/edit",
                    originalDocTitle = workingTitle,
                    candidates = describedCandidates
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Quét lại bản sao làm việc sau khi người dùng mở ra xem và gõ sửa trên Google Docs.
     */
    suspend fun rescanWorkingDoc(
        workingDocId: String,
        workingTitle: String,
        accessToken: String
    ): Result<FormSetupAnalysis> = withContext(Dispatchers.IO) {
        try {
            val getDocRequest = Request.Builder()
                .url("$DOCS_API_BASE/documents/$workingDocId")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val getDocResponse = httpClient.newCall(getDocRequest).execute()
            val getDocBody = getDocResponse.body?.string().orEmpty()
            if (!getDocResponse.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Lỗi đọc cấu trúc tài liệu (${getDocResponse.code}): $getDocBody")
                )
            }
            val docJson = JSONObject(getDocBody)

            val rawCandidates = FormCandidateScanner.scanDocument(docJson)
            val describer = FormLlmDescriber(llmClient)
            val describedCandidates = describer.describeCandidates(rawCandidates)

            Result.success(
                FormSetupAnalysis(
                    workingDocId = workingDocId,
                    workingDocUrl = "https://docs.google.com/document/d/$workingDocId/edit",
                    originalDocTitle = workingTitle,
                    candidates = describedCandidates
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Bước 7:
     * - Ghi các placeholder {{MA}} vào bản sao Google Doc bằng batchUpdate
     * - Tạo đối tượng TemplateItemEntity sẵn sàng lưu vào Room
     */
    suspend fun applyPlaceholdersAndGenerateTemplate(
        workingDocId: String,
        templateName: String,
        candidates: List<FormCandidate>,
        accessToken: String
    ): Result<TemplateItemEntity> = withContext(Dispatchers.IO) {
        try {
            // 1. Tạo payload batchUpdate xóa dấu chấm và chèn {{MA}} theo thứ tự giảm dần
            val batchPayload = DocPlaceholderInjector.buildBatchUpdatePayload(candidates)

            if (batchPayload.getJSONArray("requests").length() > 0) {
                val batchRequest = Request.Builder()
                    .url("$DOCS_API_BASE/documents/$workingDocId:batchUpdate")
                    .header("Authorization", "Bearer $accessToken")
                    .header("Content-Type", "application/json")
                    .post(batchPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val batchResponse = httpClient.newCall(batchRequest).execute()
                val batchBody = batchResponse.body?.string().orEmpty()
                if (!batchResponse.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("Lỗi chèn placeholder vào Google Doc (${batchResponse.code}): $batchBody")
                    )
                }
            }

            // 2. Tạo schemaJson động cho Mẫu
            val schemaJsonObj = JSONObject().apply {
                put("id", "custom_auto_" + System.currentTimeMillis())
                put("name", templateName)
                put("version", "1.0")
                put("max_pages", 2)

                val summaryFieldsArray = JSONArray()
                val validCandidates = candidates.filter { !it.boQua && it.ma.isNotBlank() }

                for (c in validCandidates) {
                    val fieldObj = JSONObject().apply {
                        put("key", c.ma.lowercase())
                        put("label", c.ten)
                        put("type", c.loai)
                        put("placeholder", "{{${c.ma}}}")
                        put("source", c.nguon)
                        put("max_lines", if (c.loai == "danh_sach") 8 else 3)
                        put("max_chars_per_line", 80)
                    }
                    summaryFieldsArray.put(fieldObj)
                }
                put("summary_fields", summaryFieldsArray)
            }

            val docUrl = "https://docs.google.com/document/d/$workingDocId/edit"
            val templateEntity = TemplateItemEntity(
                id = "custom_auto_" + System.currentTimeMillis(),
                name = templateName,
                description = "Mẫu tự động sinh từ Google Doc (đã chèn ${candidates.count { !it.boQua }} thẻ placeholder)",
                docTemplateUrl = docUrl,
                systemPrompt = "Bạn là trợ lý tổng hợp báo cáo công trường theo biểu mẫu quy định. Hãy tóm tắt trung thực, súc tích và khống chế đúng số dòng cho từng mục.",
                schemaJson = schemaJsonObj.toString(2),
                isBuiltIn = false,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            Result.success(templateEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
