package com.sitevoice.app

import com.sitevoice.app.template.TemplateSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TemplateSchemaTest {

    @Test
    fun testParseSchemaJson() {
        val schemaFile = File("src/main/assets/templates/nhat-ky-cong-truong/schema.json")
        val altFile = File("app/src/main/assets/templates/nhat-ky-cong-truong/schema.json")
        val target = if (schemaFile.exists()) schemaFile else altFile

        assertTrue("schema.json must exist in assets", target.exists())

        val jsonContent = target.readText(Charsets.UTF_8)
        val schema = TemplateSchema.fromJson(jsonContent)

        assertEquals("nhat-ky-cong-truong", schema.id)
        assertEquals(2, schema.maxPages)
        assertTrue(schema.noteCategories.contains("thi_cong"))
        assertTrue(schema.noteCategories.contains("nghiem_thu"))
        assertTrue(schema.noteCategories.contains("an_toan"))

        // Kiểm tra ngân sách từng trường
        val thiCongField = schema.summaryFields["cong_viec_thi_cong"]
        assertNotNull(thiCongField)
        assertEquals(15, thiCongField?.maxLines)
        assertEquals(100, thiCongField?.maxCharsPerLine)

        val nghiemThuField = schema.summaryFields["nghiem_thu"]
        assertNotNull(nghiemThuField)
        assertEquals(6, nghiemThuField?.maxLines)

        val anToanField = schema.summaryFields["an_toan"]
        assertNotNull(anToanField)
        assertEquals(3, anToanField?.maxLines)

        val yKienCdtField = schema.summaryFields["y_kien_cdt"]
        assertNotNull(yKienCdtField)
        assertEquals(200, yKienCdtField?.maxChars)
    }
}
