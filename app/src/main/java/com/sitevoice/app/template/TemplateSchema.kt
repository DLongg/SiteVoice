package com.sitevoice.app.template

import org.json.JSONObject

data class SummaryFieldConfig(
    val type: String, // "danh_sach" hoặc "van_ban"
    val maxLines: Int = 0,
    val maxCharsPerLine: Int = 100,
    val maxChars: Int = 0,
    val sourceCategories: List<String> = emptyList(),
    val showInDoc: Boolean = true
)

data class TemplateSchema(
    val id: String,
    val name: String,
    val groupByType: String,
    val maxPages: Int,
    val noteCategories: List<String>,
    val summaryFields: Map<String, SummaryFieldConfig>
) {
    companion object {
        fun fromJson(jsonStr: String): TemplateSchema {
            val json = JSONObject(jsonStr)
            val id = json.getString("id")
            val name = json.getString("ten")
            val groupByType = json.optString("gom_theo", "ngay")
            val maxPages = json.optInt("gioi_han_trang", 2)

            val categoriesArray = json.optJSONArray("muc_ghi_chu")
            val categories = mutableListOf<String>()
            if (categoriesArray != null) {
                for (i in 0 until categoriesArray.length()) {
                    categories.add(categoriesArray.getString(i))
                }
            }

            val summaryFieldsObj = json.optJSONObject("truong_tong_hop")
            val summaryFields = mutableMapOf<String, SummaryFieldConfig>()
            if (summaryFieldsObj != null) {
                for (key in summaryFieldsObj.keys()) {
                    val fieldObj = summaryFieldsObj.getJSONObject(key)
                    val type = fieldObj.optString("kieu", "danh_sach")
                    val maxLines = fieldObj.optInt("toi_da_dong", 0)
                    val maxCharsPerLine = fieldObj.optInt("ky_tu_moi_dong", 100)
                    val maxChars = fieldObj.optInt("toi_da_ky_tu", 0)
                    val showInDoc = fieldObj.optBoolean("hien_trong_doc", true)

                    val sourcesArray = fieldObj.optJSONArray("muc_nguon")
                    val sources = mutableListOf<String>()
                    if (sourcesArray != null) {
                        for (i in 0 until sourcesArray.length()) {
                            sources.add(sourcesArray.getString(i))
                        }
                    }

                    summaryFields[key] = SummaryFieldConfig(
                        type = type,
                        maxLines = maxLines,
                        maxCharsPerLine = maxCharsPerLine,
                        maxChars = maxChars,
                        sourceCategories = sources,
                        showInDoc = showInDoc
                    )
                }
            }

            return TemplateSchema(
                id = id,
                name = name,
                groupByType = groupByType,
                maxPages = maxPages,
                noteCategories = categories,
                summaryFields = summaryFields
            )
        }
    }
}
