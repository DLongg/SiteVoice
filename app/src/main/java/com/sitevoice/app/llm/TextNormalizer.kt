package com.sitevoice.app.llm

import java.util.regex.Pattern

object TextNormalizer {

    private val RE_TVGS_SPACED = Pattern.compile("(?iu)\\b(t\\s+v\\s+g\\s+s|t\\.v\\.g\\.s)\\b")
    private val RE_CDT_SPACED = Pattern.compile("(?iu)\\b(c\\s+[dđ]\\s+t|c\\.[dđ]\\.t)\\b")
    private val RE_BCH_SPACED = Pattern.compile("(?iu)\\b(b\\s+c\\s+h|b\\.c\\.h)\\b")
    private val RE_ATLD_SPACED = Pattern.compile("(?iu)\\b(a\\s+t\\s+l\\s+[dđ]|a\\.t\\.l\\.[dđ])\\b")
    private val RE_VSMT_SPACED = Pattern.compile("(?iu)\\b(v\\s+s\\s+m\\s+t|v\\.s\\.m\\.t)\\b")

    private val RE_MAC_BETONG = Pattern.compile("(?iu)\\b(mác|mac)\\s*(\\d{2,3})\\b")
    private val RE_COFFA = Pattern.compile("(?iu)\\b(cốt pha|coffa|cop pha)\\b")

    /**
     * Chuẩn hóa từ ngữ, chữ viết tắt, thuật ngữ công trường trước khi đưa vào phân loại.
     */
    fun normalize(text: String): String {
        if (text.isBlank()) return text

        var result = text.trim()

        // 1. Chuẩn hóa viết tắt rời rạc do STT nhận nhầm chữ cái
        result = RE_TVGS_SPACED.matcher(result).replaceAll("TVGS")
        result = RE_CDT_SPACED.matcher(result).replaceAll("CĐT")
        result = RE_BCH_SPACED.matcher(result).replaceAll("BCH")
        result = RE_ATLD_SPACED.matcher(result).replaceAll("ATLĐ")
        result = RE_VSMT_SPACED.matcher(result).replaceAll("VSMT")

        // 2. Chuẩn hóa thuật ngữ bê tông, cốp pha
        result = RE_MAC_BETONG.matcher(result).replaceAll("mác $2")
        result = RE_COFFA.matcher(result).replaceAll("ván khuôn")

        // 3. Phân biệt đúc / đục trong ngữ cảnh công trường
        result = result.replace("(?iu)\\bmáy đúc\\b".toRegex(), "máy đục")
        result = result.replace("(?iu)\\bđúc phá dỡ\\b".toRegex(), "đục phá dỡ")
        result = result.replace("(?iu)\\bđúc đầu cọc\\b".toRegex(), "đục đầu cọc")
        result = result.replace("(?iu)\\bđúc tường\\b".toRegex(), "đục tường")

        // 4. Chuẩn hóa dấu câu khoảng trắng thừa
        result = result.replace("\\s+".toRegex(), " ")
        result = result.replace("\\s+([.,;:!?])".toRegex(), "$1")

        return result
    }
}
