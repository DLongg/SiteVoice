package com.sitevoice.app

import com.sitevoice.app.llm.ClauseSplitter
import com.sitevoice.app.llm.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextNormalizerAndClauseSplitterTest {

    @Test
    fun testTextNormalizerAbbreviation() {
        val input = "Kỹ sư T V G S cùng đại diện C D T kiểm tra an toàn A T L Đ"
        val normalized = TextNormalizer.normalize(input)
        assertTrue(normalized.contains("TVGS"))
        assertTrue(normalized.contains("CĐT"))
        assertTrue(normalized.contains("ATLĐ"))
    }

    @Test
    fun testTextNormalizerDucVaDap() {
        val input = "Dùng máy đúc phá dỡ và đúc đầu cọc bê tông"
        val normalized = TextNormalizer.normalize(input)
        assertTrue(normalized.contains("máy đục"))
        assertTrue(normalized.contains("đục đầu cọc"))
    }

    @Test
    fun testClauseSplitterSingleSentence() {
        val text = "Đổ bê tông sàn tầng 3 trục A đến D"
        val clauses = ClauseSplitter.splitIntoClauses(text)
        assertEquals(1, clauses.size)
        assertEquals(text, clauses[0])
    }

    @Test
    fun testClauseSplitterMultiSentence() {
        val text = "Đã đục xong tường tầng 3, TVGS yêu cầu làm lại mạch vữa do bị rỗng"
        val clauses = ClauseSplitter.splitIntoClauses(text)
        assertTrue(clauses.size >= 2)
        assertEquals("Đã đục xong tường tầng 3", clauses[0])
        assertTrue(clauses[1].contains("TVGS"))
    }

    @Test
    fun testClauseSplitterConjunctions() {
        val text = "Sáng đổ bê tông sàn tầng 2, chiều nghiệm thu cốt thép dầm sàn"
        val clauses = ClauseSplitter.splitIntoClauses(text)
        assertTrue(clauses.size >= 2)
        assertEquals("Sáng đổ bê tông sàn tầng 2", clauses[0])
        assertTrue(clauses[1].contains("nghiệm thu"))
    }
}
