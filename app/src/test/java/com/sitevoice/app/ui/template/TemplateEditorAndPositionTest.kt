package com.sitevoice.app.ui.template

import com.sitevoice.app.formsetup.FormCandidate
import com.sitevoice.app.formsetup.FormLlmDescriber
import com.sitevoice.app.template.TemplateSchema
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateEditorAndPositionTest {

    @Test
    fun `test R3 position metadata round-trip in TemplateSchema`() {
        val root = JSONObject().apply {
            put("id", "custom_1")
            put("ten", "Nhật ký công trường")
            put("gioi_han_trang", 2)
            put("truong_tong_hop", JSONObject().apply {
                put("may_moc", JSONObject().apply {
                    put("ten", "Máy móc thiết bị")
                    put("kieu", "danh_sach")
                    put("nguon_ghi_chu", JSONArray().apply { put("nhap_tay") })
                    put("so_dong_toi_da", 5)
                    put("ky_tu_moi_dong", 100)
                    put("start_index", 142)
                    put("end_index", 168)
                    put("tieu_de_gan_nhat", "II. LỰC LƯỢNG THI CÔNG")
                    put("truoc", "2. Máy móc thiết bị:")
                    put("sau", "III. NỘI DUNG THI CÔNG")
                    put("tin_hieu", "dong_cham")
                })
                put("y_kien_cdt", JSONObject().apply {
                    put("ten", "Ý kiến Chủ đầu tư")
                    put("kieu", "van_ban")
                    put("nguon_ghi_chu", JSONArray().apply { put("y_kien_cdt") })
                    put("ky_tu_toi_da", 200)
                    put("start_index", 410)
                    put("end_index", 424)
                    put("tieu_de_gan_nhat", "IV. Ý KIẾN CÁC BÊN")
                    put("truoc", "1. Ý kiến Chủ đầu tư:")
                    put("sau", "2. Ý kiến TVGS:")
                    put("tin_hieu", "the_ma")
                })
            })
        }

        val schema = TemplateSchema.fromJson(root.toString())
        assertEquals(2, schema.summaryFields.size)

        val f1 = schema.summaryFields["may_moc"]
        assertNotNull(f1)
        assertEquals(142, f1!!.startIndex)
        assertEquals(168, f1.endIndex)
        assertEquals("II. LỰC LƯỢNG THI CÔNG", f1.tieuDeGanNhat)
        assertEquals("2. Máy móc thiết bị:", f1.truoc)
        assertEquals("III. NỘI DUNG THI CÔNG", f1.sau)
        assertEquals("dong_cham", f1.tinHieu)

        val f2 = schema.summaryFields["y_kien_cdt"]
        assertNotNull(f2)
        assertEquals(410, f2!!.startIndex)
        assertEquals(424, f2.endIndex)
        assertEquals("the_ma", f2.tinHieu)
    }

    @Test
    fun `test R4 render unified prompt from FieldEditorState list`() {
        val fields = listOf(
            FieldEditorState(
                key = "cong_viec_thi_cong",
                label = "Công việc thi công",
                type = "danh_sach",
                source = "thi_cong",
                maxLines = 15,
                maxCharsPerLine = 95,
                startIndex = 200,
                endIndex = 350,
                tieuDeGanNhat = "III. NỘI DUNG THI CÔNG"
            ),
            FieldEditorState(
                key = "may_moc",
                label = "Máy móc thiết bị",
                type = "danh_sach",
                source = "nhap_tay",
                maxLines = 5,
                maxCharsPerLine = 95,
                startIndex = 120,
                endIndex = 180,
                tieuDeGanNhat = "II. LỰC LƯỢNG THI CÔNG"
            ),
            FieldEditorState(
                key = "y_kien_nha_thau",
                label = "Ý kiến Nhà thầu",
                type = "van_ban",
                source = "y_kien_nha_thau",
                maxLines = 3,
                maxCharsPerLine = 200,
                startIndex = 500,
                endIndex = 550,
                tieuDeGanNhat = "IV. Ý KIẾN CÁC BÊN"
            )
        )

        val candidates = fields.mapIndexed { idx, f ->
            FormCandidate(
                id = idx + 1,
                startIndex = f.startIndex,
                endIndex = f.endIndex,
                truoc = f.truoc,
                sau = f.sau,
                tinHieu = f.tinHieu,
                tieuDeGanNhat = f.tieuDeGanNhat,
                soDongLienTiep = f.maxLines.coerceAtLeast(1),
                loaiGoiY = f.type,
                ma = f.key.trim().uppercase().replace(Regex("[^A-Z0-9_]"), "_"),
                ten = f.label.trim().ifBlank { f.key },
                loai = f.type,
                nguon = f.source,
                boQua = false
            )
        }

        val describer = FormLlmDescriber(llmClient = null)
        val prompt = describer.generateUnifiedPrompt(candidates, "Nhật ký thi công")

        assertTrue(prompt.contains("CONG_VIEC_THI_CONG"))
        assertTrue(prompt.contains("MAY_MOC"))
        assertTrue(prompt.contains("Y_KIEN_NHA_THAU"))
        assertTrue(prompt.contains("Công việc thi công"))
        assertTrue(prompt.contains("Máy móc thiết bị"))
        assertTrue(prompt.contains("Ý kiến Nhà thầu"))
    }
}
