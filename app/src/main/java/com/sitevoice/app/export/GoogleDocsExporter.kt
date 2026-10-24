package com.sitevoice.app.export

import com.sitevoice.app.data.DailyInputEntity
import com.sitevoice.app.data.TemplateItemEntity
import com.sitevoice.app.template.DailyReportContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.regex.Pattern

data class ExportResult(
    val docId: String,
    val docUrl: String,
    val title: String
)

class GoogleDocsExporter(
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    companion object {
        private const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
        private const val DOCS_API_BASE = "https://docs.googleapis.com/v1"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val LINGERING_TAG_PATTERN: Pattern = Pattern.compile("\\{\\{[^{}\\r\\n]{1,100}\\}\\}")

        /**
         * Quét toàn bộ tài liệu Google Doc để tìm các thẻ {{...}} còn sót lại.
         * Quét body paragraphs, table cells, headers, footers và footnotes.
         */
        fun findLingeringPlaceholders(docJson: JSONObject): Set<String> {
            val lingeringTags = mutableSetOf<String>()

            // 1. Quét nội dung phần thân tài liệu (body)
            val body = docJson.optJSONObject("body")
            if (body != null) {
                scanContentArray(body.optJSONArray("content"), lingeringTags)
            }

            // 2. Quét tiêu đề trang (headers)
            val headers = docJson.optJSONObject("headers")
            if (headers != null) {
                for (key in headers.keys()) {
                    val headerObj = headers.optJSONObject(key) ?: continue
                    scanContentArray(headerObj.optJSONArray("content"), lingeringTags)
                }
            }

            // 3. Quét chân trang (footers)
            val footers = docJson.optJSONObject("footers")
            if (footers != null) {
                for (key in footers.keys()) {
                    val footerObj = footers.optJSONObject(key) ?: continue
                    scanContentArray(footerObj.optJSONArray("content"), lingeringTags)
                }
            }

            // 4. Quét chú thích cuối trang (footnotes)
            val footnotes = docJson.optJSONObject("footnotes")
            if (footnotes != null) {
                for (key in footnotes.keys()) {
                    val footnoteObj = footnotes.optJSONObject(key) ?: continue
                    scanContentArray(footnoteObj.optJSONArray("content"), lingeringTags)
                }
            }

            return lingeringTags
        }

        private fun scanContentArray(content: JSONArray?, lingeringTags: MutableSet<String>) {
            if (content == null) return
            for (i in 0 until content.length()) {
                val element = content.optJSONObject(i) ?: continue

                // Đoạn văn (Paragraph)
                val paragraph = element.optJSONObject("paragraph")
                if (paragraph != null) {
                    val elements = paragraph.optJSONArray("elements")
                    if (elements != null) {
                        val fullParaText = StringBuilder()
                        for (j in 0 until elements.length()) {
                            val elem = elements.optJSONObject(j) ?: continue
                            val textRun = elem.optJSONObject("textRun")
                            if (textRun != null) {
                                fullParaText.append(textRun.optString("content"))
                            }
                        }
                        scanTextForTags(fullParaText.toString(), lingeringTags)
                    }
                }

                // Bảng (Table)
                val table = element.optJSONObject("table")
                if (table != null) {
                    val tableRows = table.optJSONArray("tableRows")
                    if (tableRows != null) {
                        for (r in 0 until tableRows.length()) {
                            val row = tableRows.optJSONObject(r) ?: continue
                            val tableCells = row.optJSONArray("tableCells") ?: continue
                            for (c in 0 until tableCells.length()) {
                                val cell = tableCells.optJSONObject(c) ?: continue
                                scanContentArray(cell.optJSONArray("content"), lingeringTags)
                            }
                        }
                    }
                }

                // Mục lục (Table of Contents)
                val toc = element.optJSONObject("tableOfContents")
                if (toc != null) {
                    scanContentArray(toc.optJSONArray("content"), lingeringTags)
                }
            }
        }

        private fun scanTextForTags(text: String, lingeringTags: MutableSet<String>) {
            if (text.isBlank()) return
            val matcher = LINGERING_TAG_PATTERN.matcher(text)
            while (matcher.find()) {
                val tag = matcher.group()
                if (tag.isNotBlank()) {
                    lingeringTags.add(tag)
                }
            }
        }
    }

    suspend fun exportReport(
        date: String,
        dailyInput: DailyInputEntity?,
        reportContent: DailyReportContent,
        templateDocId: String,
        accessToken: String,
        template: TemplateItemEntity? = null,
        customTitle: String? = null
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            val docTitle = customTitle ?: (template?.let { "${it.name} $date" } ?: "Nhật ký thi công $date")

            // Bước 1: Sao chép bản Doc mẫu (Drive API v3)
            val copyPayload = JSONObject().apply {
                put("name", docTitle)
            }

            val copyRequest = Request.Builder()
                .url("$DRIVE_API_BASE/files/$templateDocId/copy")
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .post(copyPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val copyResponse = httpClient.newCall(copyRequest).execute()
            val copyBody = copyResponse.body?.string().orEmpty()

            if (!copyResponse.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Lỗi sao chép Doc mẫu (${copyResponse.code}): $copyBody")
                )
            }

            val copyJson = JSONObject(copyBody)
            val newDocId = copyJson.optString("id")
            if (newDocId.isBlank()) {
                return@withContext Result.failure(
                    IOException("Không nhận được Doc ID sau khi sao chép: $copyBody")
                )
            }

            // Bước 2: Pass 1 — Thay thế toàn bộ placeholder (Docs API v1 batchUpdate)
            val placeholderMap = PlaceholderMapper.buildPlaceholderMap(date, dailyInput, reportContent, template)
            if (placeholderMap.isNotEmpty()) {
                val requestsArray = JSONArray()

                for ((placeholder, replacement) in placeholderMap) {
                    val replaceRequest = JSONObject().apply {
                        val replaceAllTextObj = JSONObject().apply {
                            val containsTextObj = JSONObject().apply {
                                put("text", placeholder)
                                put("matchCase", false)
                            }
                            put("containsText", containsTextObj)
                            put("replaceText", replacement)
                        }
                        put("replaceAllText", replaceAllTextObj)
                    }
                    requestsArray.put(replaceRequest)
                }

                if (requestsArray.length() > 0) {
                    val batchPayload = JSONObject().apply {
                        put("requests", requestsArray)
                    }

                    val batchRequest = Request.Builder()
                        .url("$DOCS_API_BASE/documents/$newDocId:batchUpdate")
                        .header("Authorization", "Bearer $accessToken")
                        .header("Content-Type", "application/json")
                        .post(batchPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                    val batchResponse = httpClient.newCall(batchRequest).execute()
                    val batchBody = batchResponse.body?.string().orEmpty()

                    if (!batchResponse.isSuccessful) {
                        val errorMsg = if (batchBody.contains("The document must not be an Office file")) {
                            "File mẫu đang là định dạng Word (.docx). Vui lòng mở file trên Google Drive, vào 'Tệp' -> chọn 'Lưu dưới dạng Google Tài liệu' rồi dán link tài liệu mới vào Cài đặt."
                        } else {
                            "Lỗi thay thế placeholder (${batchResponse.code}): $batchBody"
                        }
                        return@withContext Result.failure(IOException(errorMsg))
                    }
                }
            }

            // Bước 3: Pass 2 — Quét sạch 2 lớp (Layer 2 clean sweep)
            val getDocRequest = Request.Builder()
                .url("$DOCS_API_BASE/documents/$newDocId")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val getDocResponse = httpClient.newCall(getDocRequest).execute()
            val getDocBody = getDocResponse.body?.string().orEmpty()

            if (!getDocResponse.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Lỗi đọc tài liệu kiểm tra lớp 2 (${getDocResponse.code}): $getDocBody")
                )
            }

            val docJson = JSONObject(getDocBody)
            val lingeringPlaceholders = findLingeringPlaceholders(docJson)

            if (lingeringPlaceholders.isNotEmpty()) {
                val cleanRequestsArray = JSONArray()
                for (tag in lingeringPlaceholders) {
                    val replaceRequest = JSONObject().apply {
                        val replaceAllTextObj = JSONObject().apply {
                            val containsTextObj = JSONObject().apply {
                                put("text", tag)
                                put("matchCase", false)
                            }
                            put("containsText", containsTextObj)
                            put("replaceText", "")
                        }
                        put("replaceAllText", replaceAllTextObj)
                    }
                    cleanRequestsArray.put(replaceRequest)
                }

                val cleanBatchPayload = JSONObject().apply {
                    put("requests", cleanRequestsArray)
                }

                val cleanBatchRequest = Request.Builder()
                    .url("$DOCS_API_BASE/documents/$newDocId:batchUpdate")
                    .header("Authorization", "Bearer $accessToken")
                    .header("Content-Type", "application/json")
                    .post(cleanBatchPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val cleanBatchResponse = httpClient.newCall(cleanBatchRequest).execute()
                val cleanBatchBody = cleanBatchResponse.body?.string().orEmpty()

                if (!cleanBatchResponse.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("Lỗi quét sạch placeholder lớp 2 (${cleanBatchResponse.code}): $cleanBatchBody")
                    )
                }
            }

            val docUrl = "https://docs.google.com/document/d/$newDocId/edit"
            Result.success(
                ExportResult(
                    docId = newDocId,
                    docUrl = docUrl,
                    title = docTitle
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportDailyReport(
        templateDocId: String,
        date: String,
        dailyInput: DailyInputEntity?,
        reportContent: DailyReportContent,
        accessToken: String,
        template: TemplateItemEntity? = null,
        customTitle: String? = null
    ): Result<ExportResult> = exportReport(
        date = date,
        dailyInput = dailyInput,
        reportContent = reportContent,
        templateDocId = templateDocId,
        accessToken = accessToken,
        template = template,
        customTitle = customTitle
    )
}
