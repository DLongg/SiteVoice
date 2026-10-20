package com.sitevoice.app.formsetup

import org.json.JSONArray
import org.json.JSONObject

/**
 * Xử lý Bước 7 theo docs/10-thiet-lap-form.md:
 * Ghi các thẻ placeholder {{MA}} vào bản sao Google Doc bằng batchUpdate.
 * QUY TẮC CỐT LÕI:
 * 1. Áp dụng các sửa đổi theo thứ tự chỉ số GIẢM DẦN (từ cuối Doc về đầu).
 * 2. TUYỆT ĐỐI KHÔNG xóa ký tự \n kết thúc đoạn/segment (tránh lỗi Google Docs:
 *    "The range cannot include the newline character at the end of the segment").
 *    Với các đoạn trống (doan_trong), chỉ chèn {{MA}} vào đầu đoạn mà không gọi deleteContentRange.
 */
object DocPlaceholderInjector {

    fun buildBatchUpdatePayload(
        candidates: List<FormCandidate>,
        maxDocIndex: Int = Int.MAX_VALUE
    ): JSONObject {
        // Lọc các ứng viên không bị bỏ qua và sắp xếp theo startIndex GIẢM DẦN
        val sorted = candidates
            .filter { !it.boQua && it.ma.isNotBlank() }
            .sortedByDescending { it.startIndex }

        val requests = JSONArray()

        for (c in sorted) {
            val placeholder = "{{${c.ma}}}"

            // 1. Xóa nội dung cũ (dòng chấm, ô tích)
            // LƯU Ý: Với doan_trong, đoạn trống chỉ chứa ký tự \n. Docs API cấm xóa newline kết thúc segment.
            // Do đó với doan_trong chỉ cần insertText tại startIndex.
            if (c.tinHieu != "doan_trong" && c.endIndex > c.startIndex) {
                // Đảm bảo không vượt quá ký tự áp chót của tài liệu (không xóa \n cuối)
                val safeEnd = if (maxDocIndex != Int.MAX_VALUE) {
                    minOf(c.endIndex, maxDocIndex - 1)
                } else {
                    c.endIndex
                }

                if (safeEnd > c.startIndex) {
                    val deleteRequest = JSONObject().apply {
                        put("deleteContentRange", JSONObject().apply {
                            put("range", JSONObject().apply {
                                put("startIndex", c.startIndex)
                                put("endIndex", safeEnd)
                            })
                        })
                    }
                    requests.put(deleteRequest)
                }
            }

            // 2. Chèn placeholder {{MA}} vào đúng vị trí bắt đầu
            val insertRequest = JSONObject().apply {
                put("insertText", JSONObject().apply {
                    put("location", JSONObject().apply {
                        put("index", c.startIndex)
                    })
                    put("text", placeholder)
                })
            }
            requests.put(insertRequest)
        }

        return JSONObject().apply {
            put("requests", requests)
        }
    }
}
