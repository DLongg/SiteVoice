package com.sitevoice.app.formsetup

import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * Trình quét cấu trúc Google Doc để tìm các ỨNG VIÊN chỗ điền.
 * Tuyệt đối thực hiện bằng CODE (quy tắc AGENTS.md & docs/10-thiet-lap-form.md Mục 20).
 */
object FormCandidateScanner {

    private val DOTS_OR_UNDERSCORES = Pattern.compile("(\\.{2,}|_{2,})")
    private val CHECKBOX_PATTERN = Pattern.compile("([☐☑]|\\[[\\sxX]?\\])")
    private val HEADING_PATTERN = Pattern.compile(
        "^\\s*((?:[IVXLCDM]+|[A-Za-z]|\\d+(?:\\.\\d+)*)[\\.\\)]\\s*.*|[^:\\n]{2,150}:)\\s*$",
        Pattern.CASE_INSENSITIVE
    )

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

        var candidateIdCounter = 1
        var currentHeading = ""

        val pendingParagraphs = mutableListOf<ParagraphBlock>()

        fun flushParagraphs() {
            if (pendingParagraphs.isNotEmpty()) {
                scanParagraphs(
                    paragraphs = pendingParagraphs,
                    candidates = candidates,
                    candidateIdCounter = { candidateIdCounter++ },
                    currentHeadingRef = { currentHeading },
                    updateHeading = { currentHeading = it }
                )
                pendingParagraphs.clear()
            }
        }

        for (i in 0 until content.length()) {
            val element = content.optJSONObject(i) ?: continue

            // Xử lý paragraph ngoài bảng
            val paragraph = element.optJSONObject("paragraph")
            if (paragraph != null) {
                parseParagraph(paragraph, pendingParagraphs)
                continue
            }

            // Xử lý table
            val table = element.optJSONObject("table")
            if (table != null) {
                flushParagraphs()

                scanTable(
                    tableObj = table,
                    candidates = candidates,
                    candidateIdCounter = { candidateIdCounter++ },
                    currentHeadingRef = { currentHeading },
                    updateHeading = { currentHeading = it }
                )
            }
        }

        flushParagraphs()

        return candidates
    }

    private fun scanParagraphs(
        paragraphs: List<ParagraphBlock>,
        candidates: MutableList<FormCandidate>,
        candidateIdCounter: () -> Int,
        currentHeadingRef: () -> String,
        updateHeading: (String) -> Unit
    ) {
        var i = 0
        while (i < paragraphs.size) {
            val p = paragraphs[i]
            val trimmedText = p.text.trim()
            val currentHeading = currentHeadingRef()

            // 1. Kiểm tra đề mục (Hỗ trợ La Mã I., II., Chữ cái A., B., Mục con 1.1, 3.1, Số 1. và dấu : lên tới 150 ký tự)
            if (isHeading(p.text)) {
                updateHeading(trimmedText)
                i++
                continue
            }

            // 2. Đoạn trống liên tiếp ngay dưới tiêu đề -> Gộp thành 1 chỗ điền danh_sach
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

                candidates.add(
                    FormCandidate(
                        id = candidateIdCounter(),
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

            // 3. Dòng thuần chấm / gạch dưới liên tiếp (Consecutive dot lines) dưới cùng một đề mục
            if (isFullDotLine(p.text)) {
                val dotStart = p.startIndex
                var dotEnd = p.endIndex
                var count = 1
                var j = i + 1

                while (j < paragraphs.size && (isFullDotLine(paragraphs[j].text) || paragraphs[j].text.trim().isEmpty())) {
                    if (isFullDotLine(paragraphs[j].text)) {
                        dotEnd = paragraphs[j].endIndex
                        count++
                    }
                    j++
                }

                val loai = if (count >= 2) "danh_sach" else "van_ban"
                candidates.add(
                    FormCandidate(
                        id = candidateIdCounter(),
                        startIndex = dotStart,
                        endIndex = dotEnd,
                        truoc = if (currentHeading.isNotBlank()) currentHeading.takeLast(40) else "",
                        sau = if (j < paragraphs.size) paragraphs[j].text.trim().take(40) else "",
                        tinHieu = "dong_cham",
                        tieuDeGanNhat = currentHeading,
                        soDongLienTiep = count,
                        loaiGoiY = loai
                    )
                )

                i = j
                continue
            }

            // 4. Ký tự ô tích (☐, ☑, [ ], [x])
            val checkboxMatcher = CHECKBOX_PATTERN.matcher(p.text)
            while (checkboxMatcher.find()) {
                val matchStart = p.startIndex + checkboxMatcher.start()
                val matchEnd = p.startIndex + checkboxMatcher.end()
                val textBefore = p.text.substring(0, checkboxMatcher.start()).trim().takeLast(40)
                val textAfter = p.text.substring(checkboxMatcher.end()).trim().take(40)

                candidates.add(
                    FormCandidate(
                        id = candidateIdCounter(),
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

            // 5. Cụm chấm nội dòng (Inline dots) - Tách riêng từng cụm trên cùng dòng (VD: Ngày ... tháng ... năm ...)
            val dotsMatcher = DOTS_OR_UNDERSCORES.matcher(p.text)
            while (dotsMatcher.find()) {
                val matchStart = p.startIndex + dotsMatcher.start()
                val matchEnd = p.startIndex + dotsMatcher.end()
                val textBefore = p.text.substring(0, dotsMatcher.start()).trim().takeLast(40)
                val textAfter = p.text.substring(dotsMatcher.end()).trim().take(40)

                val isDate = isDateContext(textBefore) || isDateContext(textAfter)
                val isNumber = isNumberContext(textAfter) || isNumberContext(textBefore)

                val loaiGoiY = when {
                    isDate -> "ngay"
                    isNumber -> "so"
                    else -> "van_ban"
                }

                candidates.add(
                    FormCandidate(
                        id = candidateIdCounter(),
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
    }

    private fun scanTable(
        tableObj: JSONObject,
        candidates: MutableList<FormCandidate>,
        candidateIdCounter: () -> Int,
        currentHeadingRef: () -> String,
        updateHeading: (String) -> Unit
    ) {
        val tableRows = tableObj.optJSONArray("tableRows") ?: return

        for (r in 0 until tableRows.length()) {
            val row = tableRows.optJSONObject(r) ?: continue
            val tableCells = row.optJSONArray("tableCells") ?: continue
            if (tableCells.length() == 0) continue

            var leftLabel = ""

            for (c in 0 until tableCells.length()) {
                val cell = tableCells.optJSONObject(c) ?: continue
                val cellParagraphs = mutableListOf<ParagraphBlock>()
                val cellContent = cell.optJSONArray("content")
                if (cellContent != null) {
                    for (ci in 0 until cellContent.length()) {
                        val cellEl = cellContent.optJSONObject(ci) ?: continue
                        val cellP = cellEl.optJSONObject("paragraph")
                        if (cellP != null) {
                            parseParagraph(cellP, cellParagraphs)
                        }
                    }
                }

                val cellText = cellParagraphs.joinToString("\n") { it.text }.trim()
                val hasDots = DOTS_OR_UNDERSCORES.matcher(cellText).find()
                val hasCheckbox = CHECKBOX_PATTERN.matcher(cellText).find()

                // 1. Nếu ô có chữ và không có chấm/ô tích -> Đây là ô nhãn (label cell)
                if (cellText.isNotEmpty() && !hasDots && !hasCheckbox) {
                    leftLabel = cellText
                    if (isHeading(leftLabel)) {
                        updateHeading(leftLabel)
                    }
                    continue
                }

                // Ngữ cảnh tiêu đề và nhãn truyền từ trái sang phải
                val effectiveHeading = if (leftLabel.isNotBlank()) leftLabel else currentHeadingRef()
                val truocContext = if (leftLabel.isNotBlank()) leftLabel.takeLast(40) else effectiveHeading.takeLast(40)

                // 2. Nếu ô rỗng hoàn toàn (chỉ chứa \n hoặc khoảng trắng)
                if (cellText.isEmpty()) {
                    val firstP = cellParagraphs.firstOrNull()
                    val pStart = firstP?.startIndex ?: cell.optInt("startIndex", -1)
                    val pEnd = pStart

                    if (pStart != -1) {
                        val loai = if (isNumberContext(leftLabel)) "so" else "van_ban"
                        candidates.add(
                            FormCandidate(
                                id = candidateIdCounter(),
                                startIndex = pStart,
                                endIndex = pEnd,
                                truoc = truocContext,
                                sau = "",
                                tinHieu = "o_bang",
                                tieuDeGanNhat = effectiveHeading,
                                soDongLienTiep = 1,
                                loaiGoiY = loai
                            )
                        )
                    }
                    continue
                }

                // 3. Nếu toàn bộ ô là các dòng chấm thuần túy (consecutive dot lines trong ô)
                val fullDotParas = cellParagraphs.filter { isFullDotLine(it.text) }
                if (fullDotParas.isNotEmpty() && cellParagraphs.all { isFullDotLine(it.text) || it.text.trim().isEmpty() }) {
                    val firstDot = fullDotParas.first()
                    val lastDot = fullDotParas.last()
                    val count = fullDotParas.size
                    val loai = if (count >= 2) "danh_sach" else (if (isNumberContext(leftLabel)) "so" else "van_ban")

                    candidates.add(
                        FormCandidate(
                            id = candidateIdCounter(),
                            startIndex = firstDot.startIndex,
                            endIndex = lastDot.endIndex,
                            truoc = truocContext,
                            sau = "",
                            tinHieu = "o_bang",
                            tieuDeGanNhat = effectiveHeading,
                            soDongLienTiep = count,
                            loaiGoiY = loai
                        )
                    )
                    continue
                }

                // 4. Quét chi tiết các đoạn trong ô
                var pi = 0
                while (pi < cellParagraphs.size) {
                    val p = cellParagraphs[pi]

                    if (isFullDotLine(p.text)) {
                        val dotStart = p.startIndex
                        var dotEnd = p.endIndex
                        var count = 1
                        var pj = pi + 1
                        while (pj < cellParagraphs.size && (isFullDotLine(cellParagraphs[pj].text) || cellParagraphs[pj].text.trim().isEmpty())) {
                            if (isFullDotLine(cellParagraphs[pj].text)) {
                                dotEnd = cellParagraphs[pj].endIndex
                                count++
                            }
                            pj++
                        }
                        val loai = if (count >= 2) "danh_sach" else (if (isNumberContext(leftLabel)) "so" else "van_ban")
                        candidates.add(
                            FormCandidate(
                                id = candidateIdCounter(),
                                startIndex = dotStart,
                                endIndex = dotEnd,
                                truoc = truocContext,
                                sau = "",
                                tinHieu = "o_bang",
                                tieuDeGanNhat = effectiveHeading,
                                soDongLienTiep = count,
                                loaiGoiY = loai
                            )
                        )
                        pi = pj
                        continue
                    }

                    // Checkbox trong ô
                    val cbMatcher = CHECKBOX_PATTERN.matcher(p.text)
                    while (cbMatcher.find()) {
                        val matchStart = p.startIndex + cbMatcher.start()
                        val matchEnd = p.startIndex + cbMatcher.end()
                        val textBefore = p.text.substring(0, cbMatcher.start()).trim().takeLast(40)
                        val textAfter = p.text.substring(cbMatcher.end()).trim().take(40)
                        candidates.add(
                            FormCandidate(
                                id = candidateIdCounter(),
                                startIndex = matchStart,
                                endIndex = matchEnd,
                                truoc = if (textBefore.isNotBlank()) textBefore else truocContext,
                                sau = textAfter,
                                tinHieu = "o_tich",
                                tieuDeGanNhat = effectiveHeading,
                                soDongLienTiep = 1,
                                loaiGoiY = "o_tich"
                            )
                        )
                    }

                    // Inline dots trong ô
                    val dMatcher = DOTS_OR_UNDERSCORES.matcher(p.text)
                    while (dMatcher.find()) {
                        val matchStart = p.startIndex + dMatcher.start()
                        val matchEnd = p.startIndex + dMatcher.end()
                        val textBefore = p.text.substring(0, dMatcher.start()).trim().takeLast(40)
                        val textAfter = p.text.substring(dMatcher.end()).trim().take(40)

                        val isDate = isDateContext(textBefore) || isDateContext(textAfter)
                        val isNum = isNumberContext(textAfter) || isNumberContext(textBefore) || isNumberContext(leftLabel)
                        val loai = when {
                            isDate -> "ngay"
                            isNum -> "so"
                            else -> "van_ban"
                        }

                        candidates.add(
                            FormCandidate(
                                id = candidateIdCounter(),
                                startIndex = matchStart,
                                endIndex = matchEnd,
                                truoc = if (textBefore.isNotBlank()) textBefore else truocContext,
                                sau = textAfter,
                                tinHieu = "o_bang",
                                tieuDeGanNhat = effectiveHeading,
                                soDongLienTiep = 1,
                                loaiGoiY = loai
                            )
                        )
                    }

                    pi++
                }
            }
        }
    }

    fun isHeading(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.matches(Regex("^[\\.\\s_-]{2,}$"))) return false
        return HEADING_PATTERN.matcher(trimmed).matches()
    }

    fun isFullDotLine(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        if (!DOTS_OR_UNDERSCORES.matcher(trimmed).find()) return false
        return trimmed.matches(Regex("^[-*•]?\\s*[\\.\\s_-]{2,}$"))
    }

    private fun isNumberContext(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("người") ||
                lower.contains("công") ||
                lower.contains("ca") ||
                lower.contains("chỉ huy") ||
                lower.contains("công nhân") ||
                lower.contains("kỹ sư") ||
                lower.contains("số lượng") ||
                lower.contains("nhân lực")
    }

    private fun isDateContext(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("ngày") ||
                lower.contains("tháng") ||
                lower.contains("năm") ||
                lower.contains("thời gian")
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
