package com.sitevoice.app.formsetup

import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * Trình quét cấu trúc Google Doc để tìm các ỨNG VIÊN chỗ điền.
 * Tuyệt đối thực hiện bằng CODE (quy tắc AGENTS.md & docs/10-thiet-lap-form.md Mục 20).
 */
object FormCandidateScanner {

    private val DOTS_OR_UNDERSCORES = Pattern.compile("(\\.\\.{2,}|_{2,})")
    private val CHECKBOX_PATTERN = Pattern.compile("([☐☑]|\\[\\s?\\])")
    private val HEADING_PATTERN = Pattern.compile("^\\s*(\\d+[\\.\\)]\\s*[^:\\n]+:|[^:\\n]{2,30}:)\\s*$")

    data class TextElement(
        val content: String,
        val startIndex: Int,
        val endIndex: Int
    )

    data class ParagraphBlock(
        val text: String,
        val startIndex: Int,
        val endIndex: Int,
        val elements: List<TextElement>
    )

    /**
     * Quét nội dung tài liệu Google Doc từ JSON response của Docs API documents.get
     */
    fun scanDocument(docJson: JSONObject): List<FormCandidate> {
        val candidates = mutableListOf<FormCandidate>()
        val body = docJson.optJSONObject("body") ?: return candidates
        val content = body.optJSONArray("content") ?: return candidates

        val paragraphs = extractParagraphs(content)
        var candidateIdCounter = 1
        var currentHeading = ""

        var i = 0
        while (i < paragraphs.size) {
            val p = paragraphs[i]
            val trimmedText = p.text.trim()

            // Kiểm tra xem đoạn này có phải là tiêu đề không (VD: "3. Công việc thi công trong ngày:", "a. Chủ đầu tư:")
            if (isHeading(p.text)) {
                currentHeading = trimmedText
                i++
                continue
            }

            // Tín hiệu 1 & 2: Đoạn trống liên tiếp ngay dưới tiêu đề -> Gộp thành 1 chỗ điền danh_sach
            if (trimmedText.isEmpty() && currentHeading.isNotBlank()) {
                val emptyStart = p.startIndex
                var emptyEnd = p.endIndex
                var count = 1
                var j = i + 1

                while (j < paragraphs.size && paragraphs[j].text.trim().isEmpty()) {
                    emptyEnd = paragraphs[j].endIndex
                    count++
                    j++
                }

                // Gộp các đoạn trống liên tiếp
                candidates.add(
                    FormCandidate(
                        id = candidateIdCounter++,
                        startIndex = emptyStart,
                        endIndex = emptyEnd,
                        truoc = currentHeading.takeLast(40),
                        sau = if (j < paragraphs.size) paragraphs[j].text.trim().take(40) else "",
                        tinHieu = "doan_trong",
                        tieuDeGanNhat = currentHeading,
                        soDongLienTiep = count,
                        loaiGoiY = if (count >= 2) "danh_sach" else "van_ban"
                    )
                )

                i = j
                continue
            }

            // Tín hiệu 3: Ký tự ô tích (☐, ☑, [ ])
            val checkboxMatcher = CHECKBOX_PATTERN.matcher(p.text)
            var hasCheckbox = false
            while (checkboxMatcher.find()) {
                hasCheckbox = true
                val matchStart = p.startIndex + checkboxMatcher.start()
                val matchEnd = p.startIndex + checkboxMatcher.end()
                val textBefore = p.text.substring(0, checkboxMatcher.start()).trim().takeLast(40)
                val textAfter = p.text.substring(checkboxMatcher.end()).trim().take(40)

                candidates.add(
                    FormCandidate(
                        id = candidateIdCounter++,
                        startIndex = matchStart,
                        endIndex = matchEnd,
                        truoc = if (textBefore.isNotBlank()) textBefore else currentHeading,
                        sau = textAfter,
                        tinHieu = "o_tich",
                        tieuDeGanNhat = currentHeading,
                        soDongLienTiep = 1,
                        loaiGoiY = "o_tich"
                    )
                )
            }

            // Tín hiệu 4: Dãy chấm hoặc gạch dưới (..... hoặc _____)
            val dotsMatcher = DOTS_OR_UNDERSCORES.matcher(p.text)
            while (dotsMatcher.find()) {
                val matchStart = p.startIndex + dotsMatcher.start()
                val matchEnd = p.startIndex + dotsMatcher.end()
                val textBefore = p.text.substring(0, dotsMatcher.start()).trim().takeLast(40)
                val textAfter = p.text.substring(dotsMatcher.end()).trim().take(40)

                val isDateContext = textBefore.contains("ngày", ignoreCase = true) ||
                        textBefore.contains("tháng", ignoreCase = true) ||
                        textBefore.contains("năm", ignoreCase = true)

                val isNumberContext = textAfter.contains("người", ignoreCase = true) ||
                        textAfter.contains("công", ignoreCase = true) ||
                        textAfter.contains("ca", ignoreCase = true)

                val loaiGoiY = when {
                    isDateContext -> "ngay"
                    isNumberContext -> "so"
                    else -> "van_ban"
                }

                candidates.add(
                    FormCandidate(
                        id = candidateIdCounter++,
                        startIndex = matchStart,
                        endIndex = matchEnd,
                        truoc = if (textBefore.isNotBlank()) textBefore else currentHeading,
                        sau = textAfter,
                        tinHieu = "dong_cham",
                        tieuDeGanNhat = currentHeading,
                        soDongLienTiep = 1,
                        loaiGoiY = loaiGoiY
                    )
                )
            }

            i++
        }

        return candidates
    }

    private fun isHeading(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        if (HEADING_PATTERN.matcher(trimmed).matches()) return true
        if (trimmed.startsWith("1.") || trimmed.startsWith("2.") ||
            trimmed.startsWith("3.") || trimmed.startsWith("4.") ||
            trimmed.startsWith("5.") || trimmed.startsWith("6.") ||
            trimmed.startsWith("7.") || trimmed.startsWith("8.") ||
            trimmed.startsWith("a.") || trimmed.startsWith("b.") ||
            trimmed.startsWith("c.") || trimmed.startsWith("d.")
        ) {
            return true
        }
        return false
    }

    private fun extractParagraphs(contentArray: JSONArray): List<ParagraphBlock> {
        val list = mutableListOf<ParagraphBlock>()

        for (i in 0 until contentArray.length()) {
            val element = contentArray.optJSONObject(i) ?: continue

            // Xử lý paragraph trực tiếp
            val paragraph = element.optJSONObject("paragraph")
            if (paragraph != null) {
                parseParagraph(paragraph, list)
            }

            // Xử lý table
            val table = element.optJSONObject("table")
            if (table != null) {
                val tableRows = table.optJSONArray("tableRows") ?: continue
                for (r in 0 until tableRows.length()) {
                    val row = tableRows.optJSONObject(r) ?: continue
                    val tableCells = row.optJSONArray("tableCells") ?: continue
                    for (c in 0 until tableCells.length()) {
                        val cell = tableCells.optJSONObject(c) ?: continue
                        val cellContent = cell.optJSONArray("content") ?: continue
                        for (ci in 0 until cellContent.length()) {
                            val cellElement = cellContent.optJSONObject(ci) ?: continue
                            val cellParagraph = cellElement.optJSONObject("paragraph")
                            if (cellParagraph != null) {
                                parseParagraph(cellParagraph, list)
                            }
                        }
                    }
                }
            }
        }

        return list
    }

    private fun parseParagraph(paragraph: JSONObject, outputList: MutableList<ParagraphBlock>) {
        val elements = paragraph.optJSONArray("elements") ?: return
        val textBuilder = StringBuilder()
        val textElements = mutableListOf<TextElement>()
        var pStart = -1
        var pEnd = -1

        for (j in 0 until elements.length()) {
            val el = elements.optJSONObject(j) ?: continue
            val start = el.optInt("startIndex", -1)
            val end = el.optInt("endIndex", -1)
            val textRun = el.optJSONObject("textRun")
            val content = textRun?.optString("content").orEmpty()

            if (pStart == -1 && start != -1) pStart = start
            if (end != -1) pEnd = end

            textBuilder.append(content)
            textElements.add(TextElement(content, start, end))
        }

        if (pStart != -1 && pEnd != -1) {
            outputList.add(
                ParagraphBlock(
                    text = textBuilder.toString(),
                    startIndex = pStart,
                    endIndex = pEnd,
                    elements = textElements
                )
            )
        }
    }
}
