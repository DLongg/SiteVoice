package com.sitevoice.app.template

import org.json.JSONObject

data class BudgetViolation(
    val fieldName: String,
    val message: String,
    val current: Int,
    val limit: Int
)

data class BudgetValidationResult(
    val isValid: Boolean,
    val violations: List<BudgetViolation>
)

class BudgetValidator(
    private val schema: TemplateSchema
) {
    fun validate(reportJson: String): BudgetValidationResult {
        val violations = mutableListOf<BudgetViolation>()

        try {
            var clean = reportJson.trim()
            if (clean.startsWith("```json")) clean = clean.removePrefix("```json")
            if (clean.startsWith("```")) clean = clean.removePrefix("```")
            if (clean.endsWith("```")) clean = clean.removeSuffix("```")
            clean = clean.trim()

            val json = JSONObject(clean)

            for ((fieldName, config) in schema.summaryFields) {
                if (!json.has(fieldName)) continue

                if (config.type == "danh_sach") {
                    val array = json.optJSONArray(fieldName)
                    if (array != null) {
                        // Kiểm tra số dòng
                        if (config.maxLines > 0 && array.length() > config.maxLines) {
                            violations.add(
                                BudgetViolation(
                                    fieldName = fieldName,
                                    message = "Vượt số dòng tối đa",
                                    current = array.length(),
                                    limit = config.maxLines
                                )
                            )
                        }

                        // Kiểm tra độ dài từng dòng
                        if (config.maxCharsPerLine > 0) {
                            for (i in 0 until array.length()) {
                                val line = array.optString(i, "")
                                if (line.length > config.maxCharsPerLine) {
                                    violations.add(
                                        BudgetViolation(
                                            fieldName = "$fieldName[$i]",
                                            message = "Dòng vượt quá ký tự quy định",
                                            current = line.length,
                                            limit = config.maxCharsPerLine
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else if (config.type == "van_ban") {
                    val text = json.optString(fieldName, "")
                    if (config.maxChars > 0 && text.length > config.maxChars) {
                        violations.add(
                            BudgetViolation(
                                fieldName = fieldName,
                                message = "Vượt tổng số ký tự",
                                current = text.length,
                                limit = config.maxChars
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            violations.add(
                BudgetViolation(
                    fieldName = "json_format",
                    message = "Lỗi cú pháp JSON: ${e.message}",
                    current = 0,
                    limit = 0
                )
            )
        }

        return BudgetValidationResult(
            isValid = violations.isEmpty(),
            violations = violations
        )
    }
}
