package com.sitevoice.app.formsetup

import org.json.JSONArray
import org.json.JSONObject

/**
 * Xử lý Bước 7 theo docs/10-thiet-lap-form.md:
 * Ghi các thẻ placeholder {{MA}} vào bản sao Google Doc bằng batchUpdate.
 * QUY TẮC CỐT LÕI: Áp dụng các sửa đổi theo thứ tự chỉ số GIẢM DẦN (từ cuối Doc về đầu)
 * để tuyệt đối không làm lệch vị trí chỉ số của các đoạn phía trước.
 */
object DocPlaceholderInjector {

    fun buildBatchUpdatePayload(candidates: List<FormCandidate>): JSONObject {
        // Lọc các ứng viên không bị bỏ qua và sắp xếp theo startIndex GIẢM DẦN
        val sorted = candidates
            .filter { !it.boQua && it.ma.isNotBlank() }
            .sortedByDescending { it.startIndex }

        val requests = JSONArray()

        for (c in sorted) {
            val placeholder = "{{${c.ma}}}"

            // 1. Xóa nội dung cũ (dòng chấm, ô tích, đoạn trống)
            if (c.endIndex > c.startIndex) {
                val deleteRequest = JSONObject().apply {
                    put("deleteContentRange", JSONObject().apply {
                        put("range", JSONObject().apply {
                            put("startIndex", c.startIndex)
                            put("endIndex", c.endIndex)
                        })
                    })
                }
                requests.put(deleteRequest)
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
