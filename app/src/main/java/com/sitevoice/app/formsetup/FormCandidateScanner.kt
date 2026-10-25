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
    private val USER_PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([A-Za-z0-9_]+)\\}\\}")
    private val HEADING_PATTERN = Pattern.compile(
        "^\\s*((?:[IVXLCDM]+|[A-Za-z]|\\d+(?:\\.\\d+)*)[\\.\\)]\\s*.*|[^:\\n]{2,150}:)\\s*$",
        Pattern.CASE_INSENSITIVE
    )
    private val HEADING_KEYWORDS = listOf(
        "máy móc", "thiết bị", "nhân lực", "thời tiết", "công việc thi công",
        "nghiệm thu", "an toàn", "ý kiến", "chủ đầu tư", "tư vấn giám sát", "nhà thầu",
        "lực lượng thi công", "vật tư chính"
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

            // 0. Quét các thẻ placeholder {{MA}} có sẵn hoặc do người dùng tự gõ tay
            val tagMatcher = USER_PLACEHOLDER_PATTERN.matcher(p.text)
            var hasUserTags = false
            while (tagMatcher.find()) {
                hasUserTags = true
                val matchStart = p.startIndex + tagMatcher.start()
                val matchEnd = p.startIndex + tagMatcher.end()
                val userMa = tagMatcher.group(1).orEmpty()
                val textBefore = p.text.substring(0, tagMatcher.start()).trim().takeLast(40)
                val textAfter = p.text.substring(tagMatcher.end()).trim().take(40)

                candidates.add(
                    FormCandidate(
                        id = candidateIdCounter(),
                        startIndex = matchStart,
                        endIndex = matchEnd,
                        truoc = if (textBefore.isNotBlank()) textBefore else currentHeadingRef().takeLast(40),
                        sau = textAfter,
                        tinHieu = "the_ma",
                        tieuDeGanNhat = currentHeadingRef(),
                        soDongLienTiep = 1,
                        loaiGoiY = "van_ban",
                        ma = userMa,
                        ten = userMa,
                        doTinCay = 1.0f,
                        originalMa = userMa
                    )
                )
            }
            if (hasUserTags && !DOTS_OR_UNDERSCORES.matcher(p.text).find() && !CHECKBOX_PATTERN.matcher(p.text).find()) {
                i++
                continue
            }

            // 1. Kiểm tra dòng có prefix đề mục kèm dòng chấm (VD: "a. Chủ đầu tư: ............")
            val dotsMatcher = DOTS_OR_UNDERSCORES.matcher(p.text)
            if (dotsMatcher.find()) {
                val prefix = p.text.substring(0, dotsMatcher.start()).trim()
                val suffix = p.text.substring(dotsMatcher.start()).trim()

                if (prefix.isNotBlank() && (isHeading(prefix) || isHeadingPrefix(prefix))) {
                    updateHeading(prefix)

                    // Nếu phần còn lại của dòng là dòng chấm thuần túy (kéo dài hoặc kèm các dòng chấm tiếp theo)
                    if (suffix.matches(Regex("^[\\.\\s_-]{2,}$"))) {
                        val dotStart = p.startIndex + dotsMatcher.start()
                        var dotEnd = p.startIndex + dotsMatcher.end()
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
                                truoc = prefix.takeLast(40),
                                sau = if (j < paragraphs.size) paragraphs[j].text.trim().take(40) else "",
                                tinHieu = "dong_cham",
                                tieuDeGanNhat = prefix,
                                soDongLienTiep = count,
                                loaiGoiY = loai
                            )
                        )

                        i = j
                        continue
                    }
                }
            }

            // 2. Kiểm tra đề mục thuần túy (không chứa chấm hoặc ô tích)
            if (isHeading(p.text)) {
                updateHeading(trimmedText)
                i++
                continue
            }

            // Chống tràn đề mục bằng đoạn văn liền kề (paragraphs[i - 1]) nếu đoạn trước là nhãn/heading
            var effectiveHeading = currentHeadingRef()
            if (i > 0) {
                val prevText = paragraphs[i - 1].text.trim()
                if (prevText.isNotBlank() &&
                    !DOTS_OR_UNDERSCORES.matcher(prevText).find() &&
                    !CHECKBOX_PATTERN.matcher(prevText).find() &&
                    !USER_PLACEHOLDER_PATTERN.matcher(prevText).find() &&
                    prevText.length <= 150 &&
                    (isHeading(prevText) || isHeadingPrefix(prevText))) {
                    effectiveHeading = prevText
                    updateHeading(prevText)
                }
            }

            // 3. Đoạn trống liên tiếp ngay dưới tiêu đề -> Gộp thành 1 chỗ điền danh_sach
            if (trimmedText.isEmpty() && effectiveHeading.isNotBlank()) {
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
                        truoc = effectiveHeading.takeLast(40),
                        sau = if (j < paragraphs.size) paragraphs[j].text.trim().take(40) else "",
                        tinHieu = "doan_trong",
                        tieuDeGanNhat = effectiveHeading,
                        soDongLienTiep = count,
                        loaiGoiY = if (count >= 2) "danh_sach" else "van_ban"
                    )
                )

                i = j
                continue
            }

            // 4. Dòng thuần chấm / gạch dưới liên tiếp (Consecutive dot lines) dưới cùng một đề mục
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
                        truoc = if (effectiveHeading.isNotBlank()) effectiveHeading.takeLast(40) else "",
                        sau = if (j < paragraphs.size) paragraphs[j].text.trim().take(40) else "",
                        tinHieu = "dong_cham",
                        tieuDeGanNhat = effectiveHeading,
                        soDongLienTiep = count,
                        loaiGoiY = loai
                    )
                )

                i = j
                continue
            }

            // 5. Ký tự ô tích (☐, ☑, [ ], [x])
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
                        truoc = if (textBefore.isNotBlank()) textBefore else effectiveHeading,
                        sau = textAfter,
                        tinHieu = "o_tich",
                        tieuDeGanNhat = effectiveHeading,
                        soDongLienTiep = 1,
                        loaiGoiY = "o_tich"
                    )
                )
            }

            // 6. Cụm chấm nội dòng (Inline dots) - Tách riêng từng cụm trên cùng dòng (VD: Ngày ... tháng ... năm ...)
            val inlineDotsMatcher = DOTS_OR_UNDERSCORES.matcher(p.text)
            while (inlineDotsMatcher.find()) {
                val matchStart = p.startIndex + inlineDotsMatcher.start()
                val matchEnd = p.startIndex + inlineDotsMatcher.end()
                val textBefore = p.text.substring(0, inlineDotsMatcher.start()).trim().takeLast(40)
                val textAfter = p.text.substring(inlineDotsMatcher.end()).trim().take(40)

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
                        truoc = if (textBefore.isNotBlank()) textBefore else effectiveHeading,
                        sau = textAfter,
                        tinHieu = "dong_cham",
                        tieuDeGanNhat = effectiveHeading,
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
        val columnHeaders = mutableMapOf<Int, String>()

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
                val hasTag = USER_PLACEHOLDER_PATTERN.matcher(cellText).find()

                // Lưu tiêu đề cột ở hàng đầu tiên (Header Row) nếu là ô nhãn
                if (r == 0 && cellText.isNotEmpty() && !hasDots && !hasCheckbox && !hasTag) {
                    columnHeaders[c] = cellText
                    leftLabel = cellText
                    if (isHeading(leftLabel)) {
                        updateHeading(leftLabel)
                    }
                    continue
                }

                // 1. Nếu ô có chữ và không có chấm/ô tích/thẻ -> Đây là ô nhãn (label cell)
                if (cellText.isNotEmpty() && !hasDots && !hasCheckbox && !hasTag) {
                    leftLabel = cellText
                    if (isHeading(leftLabel)) {
                        updateHeading(leftLabel)
                    }
                    continue
                }

                // Ngữ cảnh tiêu đề và nhãn truyền từ trái sang phải, hoặc kế thừa từ tiêu đề cột
                val colHeader = columnHeaders[c].orEmpty()
                val fallbackHeading = if (colHeader.isNotBlank()) colHeader else currentHeadingRef()
                var effectiveHeading = if (leftLabel.isNotBlank()) leftLabel else fallbackHeading
                var truocContext = if (leftLabel.isNotBlank()) leftLabel.takeLast(40) else effectiveHeading.takeLast(40)

                // 2. Nếu ô rỗng hoàn toàn (chỉ chứa \n hoặc khoảng trắng)
                if (cellText.isEmpty()) {
                    val firstP = cellParagraphs.firstOrNull()
                    val pStart = firstP?.startIndex ?: cell.optInt("startIndex", -1)
                    val pEnd = pStart

                    if (pStart != -1) {
                        val loai = if (isNumberContext(leftLabel) || isNumberContext(colHeader)) "so" else "van_ban"
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
                    val loai = if (count >= 2) "danh_sach" else (if (isNumberContext(leftLabel) || isNumberContext(colHeader)) "so" else "van_ban")

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
                var cellLocalLabel = leftLabel
                while (pi < cellParagraphs.size) {
                    val p = cellParagraphs[pi]
                    val pTrimmed = p.text.trim()

                    // Quét thẻ placeholder {{MA}} trong ô bảng
                    val tagMatcher = USER_PLACEHOLDER_PATTERN.matcher(p.text)
                    var pHasTags = false
                    while (tagMatcher.find()) {
                        pHasTags = true
                        val matchStart = p.startIndex + tagMatcher.start()
                        val matchEnd = p.startIndex + tagMatcher.end()
                        val userMa = tagMatcher.group(1).orEmpty()
                        val textBefore = p.text.substring(0, tagMatcher.start()).trim().takeLast(40)
                        val textAfter = p.text.substring(tagMatcher.end()).trim().take(40)
                        candidates.add(
                            FormCandidate(
                                id = candidateIdCounter(),
                                startIndex = matchStart,
                                endIndex = matchEnd,
                                truoc = if (textBefore.isNotBlank()) textBefore else truocContext,
                                sau = textAfter,
                                tinHieu = "the_ma",
                                tieuDeGanNhat = effectiveHeading,
                                soDongLienTiep = 1,
                                loaiGoiY = "van_ban",
                                ma = userMa,
                                ten = userMa,
                                doTinCay = 1.0f,
                                originalMa = userMa
                            )
                        )
                    }
                    if (pHasTags && !DOTS_OR_UNDERSCORES.matcher(p.text).find() && !CHECKBOX_PATTERN.matcher(p.text).find()) {
                        pi++
                        continue
                    }

                    // Nếu đoạn văn trong ô chỉ là nhãn (không chấm, không ô tích)
                    // Cập nhật cellLocalLabel và effectiveHeading cho các đoạn sau trong cùng ô
                    if (pTrimmed.isNotEmpty() &&
                        !DOTS_OR_UNDERSCORES.matcher(pTrimmed).find() &&
                        !CHECKBOX_PATTERN.matcher(pTrimmed).find() &&
                        !USER_PLACEHOLDER_PATTERN.matcher(pTrimmed).find()) {
                        cellLocalLabel = pTrimmed
                        effectiveHeading = cellLocalLabel
                        truocContext = cellLocalLabel.takeLast(40)
                        if (isHeading(cellLocalLabel)) {
                            updateHeading(cellLocalLabel)
                        }
                        pi++
                        continue
                    }

                    // Nếu dòng có prefix nhãn kèm dòng chấm (VD: "a. Chủ đầu tư: ............") trong ô
                    val dotsMatcher = DOTS_OR_UNDERSCORES.matcher(p.text)
                    if (dotsMatcher.find()) {
                        val prefix = p.text.substring(0, dotsMatcher.start()).trim()
                        val suffix = p.text.substring(dotsMatcher.start()).trim()
                        if (prefix.isNotBlank() && (isHeading(prefix) || isHeadingPrefix(prefix))) {
                            cellLocalLabel = prefix
                            effectiveHeading = prefix
                            truocContext = prefix.takeLast(40)
                            updateHeading(prefix)

                            if (suffix.matches(Regex("^[\\.\\s_-]{2,}$"))) {
                                val dotStart = p.startIndex + dotsMatcher.start()
                                var dotEnd = p.startIndex + dotsMatcher.end()
                                var count = 1
                                var pj = pi + 1
                                while (pj < cellParagraphs.size && (isFullDotLine(cellParagraphs[pj].text) || cellParagraphs[pj].text.trim().isEmpty())) {
                                    if (isFullDotLine(cellParagraphs[pj].text)) {
                                        dotEnd = cellParagraphs[pj].endIndex
                                        count++
                                    }
                                    pj++
                                }
                                val loai = if (count >= 2) "danh_sach" else "van_ban"
                                candidates.add(
                                    FormCandidate(
                                        id = candidateIdCounter(),
                                        startIndex = dotStart,
                                        endIndex = dotEnd,
                                        truoc = truocContext,
                                        sau = if (pj < cellParagraphs.size) cellParagraphs[pj].text.trim().take(40) else "",
                                        tinHieu = "o_bang",
                                        tieuDeGanNhat = effectiveHeading,
                                        soDongLienTiep = count,
                                        loaiGoiY = loai
                                    )
                                )
                                pi = pj
                                continue
                            }
                        }
                    }

                    // Dòng thuần chấm trong ô
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
                        val loai = if (count >= 2) "danh_sach" else (if (isNumberContext(cellLocalLabel) || isNumberContext(colHeader)) "so" else "van_ban")
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
                        val isNum = isNumberContext(textAfter) || isNumberContext(textBefore) || isNumberContext(cellLocalLabel) || isNumberContext(colHeader)
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

        // Tuyệt đối không coi dòng chứa chấm hoặc ô tích hoặc thẻ {{MA}} là dòng tiêu đề thuần túy
        if (DOTS_OR_UNDERSCORES.matcher(trimmed).find() || CHECKBOX_PATTERN.matcher(trimmed).find()) {
            return false
        }
        if (USER_PLACEHOLDER_PATTERN.matcher(trimmed).find()) {
            return false
        }

        // 1. Khớp HEADING_PATTERN chuẩn (La Mã, Chữ cái, Số phân cấp, kết thúc bằng dấu :)
        if (HEADING_PATTERN.matcher(trimmed).matches()) return true

        // 2. Mở rộng nhận diện dấu *, -, +, • ở đầu dòng không có chấm
        if (trimmed.matches(Regex("^[-*+•]\\s+.*$")) && trimmed.length <= 150) {
            val afterBullet = trimmed.replace(Regex("^[-*+•]\\s+"), "").trim()
            if (afterBullet.isNotEmpty() && !afterBullet.matches(Regex("^[\\.\\s_-]{2,}$"))) {
                return true
            }
        }

        // 3. Khớp cụm từ khóa đề mục chuẩn kể cả khi thiếu dấu : ở cuối
        val lower = trimmed.lowercase()
        val hasKeyword = HEADING_KEYWORDS.any { kw ->
            lower.startsWith(kw) || lower.contains(kw)
        }
        if (hasKeyword && trimmed.length <= 120 && !lower.startsWith("hôm nay") && !lower.startsWith("lưu ý:")) {
            if (!lower.contains("diễn ra") && !lower.contains("khi làm việc") && !lower.contains("phải đội")) {
                return true
            }
        }

        return false
    }

    fun isHeadingPrefix(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > 100) return false
        if (DOTS_OR_UNDERSCORES.matcher(trimmed).find() || CHECKBOX_PATTERN.matcher(trimmed).find() || USER_PLACEHOLDER_PATTERN.matcher(trimmed).find()) return false

        if (trimmed.matches(Regex("^((?:[IVXLCDM]+|[A-Za-z]|\\d+(?:\\.\\d+)*)[\\.\\)]|[-*+•])\\s*.*$", RegexOption.IGNORE_CASE))) {
            return true
        }
        if (trimmed.endsWith(":")) {
            return true
        }
        val lower = trimmed.lowercase()
        return HEADING_KEYWORDS.any { kw -> lower.contains(kw) }
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
