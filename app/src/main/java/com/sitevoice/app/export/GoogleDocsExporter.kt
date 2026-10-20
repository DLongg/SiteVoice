package com.sitevoice.app.export

import com.sitevoice.app.data.DailyInputEntity
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
    }

    suspend fun exportReport(
        date: String,
        dailyInput: DailyInputEntity?,
        reportContent: DailyReportContent,
        templateDocId: String,
        accessToken: String
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            val docTitle = "Nhật ký thi công $date"

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

            // Bước 2: Thay thế toàn bộ placeholder (Docs API v1 batchUpdate)
            val placeholderMap = PlaceholderMapper.buildPlaceholderMap(date, dailyInput, reportContent)
            val requestsArray = JSONArray()

            for ((placeholder, replacement) in placeholderMap) {
                val replaceRequest = JSONObject().apply {
                    val replaceAllTextObj = JSONObject().apply {
                        val containsTextObj = JSONObject().apply {
                            put("text", placeholder)
                            put("matchCase", true)
                        }
                        put("containsText", containsTextObj)
                        put("replaceText", replacement)
                    }
                    put("replaceAllText", replaceAllTextObj)
                }
                requestsArray.put(replaceRequest)
            }

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
                return@withContext Result.failure(
                    IOException("Lỗi thay thế placeholder (${batchResponse.code}): $batchBody")
                )
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
}
