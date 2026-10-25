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

    @Test
    fun testHomophoneCoffaVariants() {
        assertEquals("cốp pha dầm sàn", TextNormalizer.normalize("cốc pha rầm sàn"))
        assertEquals("cốp pha cột", TextNormalizer.normalize("cốt pha cột"))
        assertEquals("lắp dựng cốp pha tầng 3", TextNormalizer.normalize("lắp dựng coffa tầng 3"))
        assertEquals("tháo dỡ cốp pha", TextNormalizer.normalize("tháo dỡ cop pha"))
        // Case-insensitive
        assertEquals("CỐP PHA DẦM", TextNormalizer.normalize("CỐC PHA RẦM").replace("rầm", "dầm").replace("RẦM", "DẦM"))
    }

    @Test
    fun testHomophoneRamSanToDamSan() {
        val input = "Đang gia công rầm sàn trục A đến D tầng 4"
        val result = TextNormalizer.normalize(input)
        assertTrue("Phải chuẩn hóa 'rầm sàn' thành 'dầm sàn'", result.contains("dầm sàn"))
        assertTrue("Phải bảo toàn trục A đến D và tầng 4", result.contains("trục A đến D tầng 4"))
    }

    @Test
    fun testHomophoneNhiemThuToNghiemThu() {
        val input = "Kỹ sư tiến hành nhiệm thu công việc xây tường"
        val result = TextNormalizer.normalize(input)
        assertTrue("Phải chuẩn hóa 'nhiệm thu' thành 'nghiệm thu'", result.contains("nghiệm thu"))
    }

    @Test
    fun testHomophoneCotPhepToCotThep() {
        val input = "Gia công buộc cốt phép đài móng M1"
        val result = TextNormalizer.normalize(input)
        assertTrue("Phải chuẩn hóa 'cốt phép' thành 'cốt thép'", result.contains("cốt thép đài móng M1"))
    }

    @Test
    fun testHomophoneGianDaoToGianGiao() {
        val input = "Lắp dựng hệ thống giàn dáo ngoài tầng 5"
        val result = TextNormalizer.normalize(input)
        assertTrue("Phải chuẩn hóa 'giàn dáo' thành 'giàn giáo'", result.contains("giàn giáo"))
    }

    @Test
    fun testHomophoneChatTuongToTratTuong() {
        val input = "Tổ thợ đang chát tường trong nhà khu vệ sinh"
        val result = TextNormalizer.normalize(input)
        assertTrue("Phải chuẩn hóa 'chát tường' thành 'trát tường'", result.contains("trát tường"))
    }

    @Test
    fun testHomophoneBaoDuToBaoDuong() {
        val input1 = "Tưới nước bảo dữ ẩm sàn bê tông tầng 2"
        val result1 = TextNormalizer.normalize(input1)
        assertTrue("Phải chuẩn hóa 'bảo dữ' thành 'bảo dưỡng'", result1.contains("bảo dưỡng ẩm sàn bê tông tầng 2"))

        val input2 = "Bảo dữ xong rồi."
        val result2 = TextNormalizer.normalize(input2)
        assertTrue("Phải chuẩn hóa 'Bảo dữ' thành 'Bảo dưỡng'", result2.contains("Bảo dưỡng"))

        val inputAlreadyCorrect = "Bảo dưỡng bê tông đúng 7 ngày"
        val resultAlreadyCorrect = TextNormalizer.normalize(inputAlreadyCorrect)
        assertEquals("Không được làm biến dạng 'bảo dưỡng'", "Bảo dưỡng bê tông đúng 7 ngày", resultAlreadyCorrect)
    }

    @Test
    fun testAllSevenHomophonesInComplexSentence() {
        val input = "Sáng nhiệm thu cốc pha và cốt phép rầm sàn trục 2-5, chiều dựng giàn dáo để chát tường và bảo dữ ẩm."
        val normalized = TextNormalizer.normalize(input)

        assertTrue(normalized.contains("nghiệm thu"))
        assertTrue(normalized.contains("cốp pha"))
        assertTrue(normalized.contains("cốt thép"))
        assertTrue(normalized.contains("dầm sàn"))
        assertTrue(normalized.contains("giàn giáo"))
        assertTrue(normalized.contains("trát tường"))
        assertTrue(normalized.contains("bảo dưỡng"))
        assertTrue("Phải giữ nguyên tọa độ trục 2-5", normalized.contains("trục 2-5"))
    }
}
