package com.sitevoice.app

import com.sitevoice.app.template.BudgetValidator
import com.sitevoice.app.template.TemplateSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class BudgetValidatorTest {

    private lateinit var schema: TemplateSchema
    private lateinit var validator: BudgetValidator

    @Before
    fun setUp() {
        val schemaFile = File("src/main/assets/templates/nhat-ky-cong-truong/schema.json")
        val altFile = File("app/src/main/assets/templates/nhat-ky-cong-truong/schema.json")
        val target = if (schemaFile.exists()) schemaFile else altFile
        schema = TemplateSchema.fromJson(target.readText(Charsets.UTF_8))
        validator = BudgetValidator(schema)
    }

    @Test
    fun testValidReportPassesBudget() {
        val validJson = """
        {
            "cong_viec_thi_cong": [
                "Đổ bê tông sàn tầng 2 diện tích 250m2, hoàn thành lúc 15h30",
                "Lắp dựng cốp pha dầm sàn trục 1-4"
            ],
            "nghiem_thu": [
                "Nghiệm thu cốt thép dầm sàn tầng 2 đạt yêu cầu bản vẽ"
            ],
            "an_toan": [
                "Nhắc nhở công nhân đeo dây an toàn khi làm việc trên cao"
            ],
            "y_kien_cdt": "Yêu cầu tăng cường tưới nước bảo dưỡng bê tông.",
            "y_kien_tvgs": "Đồng ý chuyển bước thi công tiếp theo.",
            "y_kien_nha_thau": "Đề xuất cấp thêm mặt bằng kho tập kết vật tư."
        }
        """

        val result = validator.validate(validJson)
        assertTrue("Valid JSON should pass budget", result.isValid)
        assertTrue(result.violations.isEmpty())
    }

    @Test
    fun testExceedLineLimitDetected() {
        // Nghiem thu toi da 6 dong, truyen vao 7 dong
        val exceedLinesJson = """
        {
            "nghiem_thu": [
                "Dòng 1", "Dòng 2", "Dòng 3", "Dòng 4", "Dòng 5", "Dòng 6", "Dòng 7"
            ]
        }
        """

        val result = validator.validate(exceedLinesJson)
        assertFalse("Exceeding line limit should fail", result.isValid)
        assertTrue(result.violations.any { it.fieldName == "nghiem_thu" && it.current == 7 })
    }

    @Test
    fun testExceedCharLimitDetected() {
        // y_kien_cdt toi da 200 ky tu, truyen vao chuoi 250 ky tu
        val longText = "A".repeat(250)
        val exceedCharsJson = """
        {
            "y_kien_cdt": "$longText"
        }
        """

        val result = validator.validate(exceedCharsJson)
        assertFalse("Exceeding character limit should fail", result.isValid)
        assertTrue(result.violations.any { it.fieldName == "y_kien_cdt" && it.limit == 200 })
    }
}
