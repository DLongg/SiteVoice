package com.sitevoice.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TemplateAssetTest {

    @Test
    fun testTemplateAssetsExistAndReadable() {
        val assetDir = File("src/main/assets/templates/nhat-ky-cong-truong")
        assertTrue("Thư mục assets mẫu phải tồn tại", assetDir.exists())

        val schemaFile = File(assetDir, "schema.json")
        assertTrue("File schema.json phải tồn tại trong assets", schemaFile.exists())

        val schemaContent = schemaFile.readText()
        assertTrue("Nội dung schema.json không được rỗng", schemaContent.isNotBlank())

        // Kiểm tra các trường khóa bắt buộc theo schema
        assertTrue("Schema phải có id nhat-ky-cong-truong", schemaContent.contains("\"id\": \"nhat-ky-cong-truong\""))
        assertTrue("Schema phải có gioi_han_trang: 2", schemaContent.contains("\"gioi_han_trang\": 2"))
        assertTrue("Schema phải có cấu hình nhap_tay", schemaContent.contains("\"nhap_tay\""))
        assertTrue("Schema phải có cấu hình muc_ghi_chu", schemaContent.contains("\"muc_ghi_chu\""))
        assertTrue("Schema phải có cấu hình truong_tong_hop", schemaContent.contains("\"truong_tong_hop\""))

        // Kiểm tra file docx mẫu
        val docMauFile = File(assetDir, "Nhat_ky_thi_cong_mau.docx")
        assertTrue("File Nhat_ky_thi_cong_mau.docx phải tồn tại trong assets", docMauFile.exists())
        assertTrue("Kích thước file docx phải lớn hơn 0", docMauFile.length() > 0)
    }
}
