package com.sitevoice.app.template

import org.json.JSONObject

data class SummaryFieldConfig(
    val type: String, // "danh_sach" hoặc "van_ban"
    val maxLines: Int = 0,
    val maxCharsPerLine: Int = 100,
    val maxChars: Int = 0,
    val sourceCategories: List<String> = emptyList(),
    val showInDoc: Boolean = true,
    val startIndex: Int = -1,
    val endIndex: Int = -1,
    val tieuDeGanNhat: String = "",
    val truoc: String = "",
    val sau: String = "",
    val tinHieu: String = ""
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
            return try {
                val json = JSONObject(jsonStr)
                val id = json.optString("id", "custom_${System.currentTimeMillis()}")
                val name = json.optString("ten").ifBlank {
                    json.optString("name", "Mẫu tùy chỉnh")
                }
                val groupByType = json.optString("gom_theo").ifBlank {
                    json.optString("group_by").ifBlank {
                        json.optString("groupByType", json.optString("groupBy", "ngay"))
                    }
                }
                val maxPages = when {
                    json.has("gioi_han_trang") -> json.optInt("gioi_han_trang", 2)
                    json.has("max_pages") -> json.optInt("max_pages", 2)
                    json.has("maxPages") -> json.optInt("maxPages", 2)
                    else -> 2
                }

                val categoriesArray = json.optJSONArray("muc_ghi_chu")
                    ?: json.optJSONArray("note_categories")
                    ?: json.optJSONArray("noteCategories")
                val categories = mutableListOf<String>()
                if (categoriesArray != null) {
                    for (i in 0 until categoriesArray.length()) {
                        val cat = categoriesArray.optString(i)
                        if (cat.isNotBlank()) categories.add(cat)
                    }
                }

                val summaryFields = mutableMapOf<String, SummaryFieldConfig>()

                // 1. Kiểm tra nếu các trường được khai báo dạng JSONObject (Map)
                val summaryFieldsObj = json.optJSONObject("truong_tong_hop")
                    ?: json.optJSONObject("summary_fields")
                    ?: json.optJSONObject("summaryFields")

                if (summaryFieldsObj != null) {
                    for (key in summaryFieldsObj.keys()) {
                        val fieldObj = summaryFieldsObj.optJSONObject(key) ?: continue
                        val type = fieldObj.optString("kieu").ifBlank {
                            fieldObj.optString("type", "danh_sach")
                        }
                        val maxLines = when {
                            fieldObj.has("toi_da_dong") -> fieldObj.optInt("toi_da_dong", 0)
                            fieldObj.has("max_lines") -> fieldObj.optInt("max_lines", 0)
                            fieldObj.has("maxLines") -> fieldObj.optInt("maxLines", 0)
                            else -> 0
                        }
                        val maxCharsPerLine = when {
                            fieldObj.has("ky_tu_moi_dong") -> fieldObj.optInt("ky_tu_moi_dong", 100)
                            fieldObj.has("max_chars_per_line") -> fieldObj.optInt("max_chars_per_line", 100)
                            fieldObj.has("maxCharsPerLine") -> fieldObj.optInt("maxCharsPerLine", 100)
                            else -> 100
                        }
                        val maxChars = when {
                            fieldObj.has("toi_da_ky_tu") -> fieldObj.optInt("toi_da_ky_tu", 0)
                            fieldObj.has("max_chars") -> fieldObj.optInt("max_chars", 0)
                            fieldObj.has("maxChars") -> fieldObj.optInt("maxChars", 0)
                            else -> 0
                        }
                        val showInDoc = when {
                            fieldObj.has("hien_trong_doc") -> fieldObj.optBoolean("hien_trong_doc", true)
                            fieldObj.has("show_in_doc") -> fieldObj.optBoolean("show_in_doc", true)
                            fieldObj.has("showInDoc") -> fieldObj.optBoolean("showInDoc", true)
                            else -> true
                        }

                        val sources = mutableListOf<String>()
                        val sourcesArray = fieldObj.optJSONArray("muc_nguon")
                            ?: fieldObj.optJSONArray("source_categories")
                            ?: fieldObj.optJSONArray("sourceCategories")
                        if (sourcesArray != null) {
                            for (i in 0 until sourcesArray.length()) {
                                sources.add(sourcesArray.optString(i))
                            }
                        } else {
                            val singleSource = fieldObj.optString("source").ifBlank { fieldObj.optString("nguon") }
                            if (singleSource.isNotBlank()) sources.add(singleSource)
                        }

                        val startIndex = when {
                            fieldObj.has("start_index") -> fieldObj.optInt("start_index", -1)
                            fieldObj.has("startIndex") -> fieldObj.optInt("startIndex", -1)
                            else -> -1
                        }
                        val endIndex = when {
                            fieldObj.has("end_index") -> fieldObj.optInt("end_index", -1)
                            fieldObj.has("endIndex") -> fieldObj.optInt("endIndex", -1)
                            else -> -1
                        }
                        val tieuDeGanNhat = fieldObj.optString("tieu_de_gan_nhat").ifBlank {
                            fieldObj.optString("tieuDeGanNhat").ifBlank {
                                fieldObj.optString("heading", "")
                            }
                        }
                        val truoc = fieldObj.optString("truoc").ifBlank { fieldObj.optString("before", "") }
                        val sau = fieldObj.optString("sau").ifBlank { fieldObj.optString("after", "") }
                        val tinHieu = fieldObj.optString("tin_hieu").ifBlank { fieldObj.optString("tinHieu", "") }

                        summaryFields[key] = SummaryFieldConfig(
                            type = type,
                            maxLines = maxLines,
                            maxCharsPerLine = maxCharsPerLine,
                            maxChars = maxChars,
                            sourceCategories = sources,
                            showInDoc = showInDoc,
                            startIndex = startIndex,
                            endIndex = endIndex,
                            tieuDeGanNhat = tieuDeGanNhat,
                            truoc = truoc,
                            sau = sau,
                            tinHieu = tinHieu
                        )
                    }
                }

                // 2. Kiểm tra nếu các trường được khai báo dạng JSONArray (List)
                val summaryArray = json.optJSONArray("summary_fields")
                    ?: json.optJSONArray("summaryFields")
                    ?: json.optJSONArray("truong_tong_hop")
                if (summaryArray != null) {
                    for (i in 0 until summaryArray.length()) {
                        val fieldObj = summaryArray.optJSONObject(i) ?: continue
                        val key = fieldObj.optString("key").ifBlank {
                            fieldObj.optString("name").ifBlank {
                                fieldObj.optString("ma")
                            }
                        }.lowercase()
                        if (key.isBlank()) continue

                        if (summaryFields.containsKey(key)) continue

                        val type = fieldObj.optString("type").ifBlank {
                            fieldObj.optString("kieu", "danh_sach")
                        }
                        val maxLines = when {
                            fieldObj.has("max_lines") -> fieldObj.optInt("max_lines", 0)
                            fieldObj.has("maxLines") -> fieldObj.optInt("maxLines", 0)
                            fieldObj.has("toi_da_dong") -> fieldObj.optInt("toi_da_dong", 0)
                            else -> 0
                        }
                        val maxCharsPerLine = when {
                            fieldObj.has("max_chars_per_line") -> fieldObj.optInt("max_chars_per_line", 100)
                            fieldObj.has("maxCharsPerLine") -> fieldObj.optInt("maxCharsPerLine", 100)
                            fieldObj.has("ky_tu_moi_dong") -> fieldObj.optInt("ky_tu_moi_dong", 100)
                            else -> 100
                        }
                        val maxChars = when {
                            fieldObj.has("max_chars") -> fieldObj.optInt("max_chars", 0)
                            fieldObj.has("maxChars") -> fieldObj.optInt("maxChars", 0)
                            fieldObj.has("toi_da_ky_tu") -> fieldObj.optInt("toi_da_ky_tu", 0)
                            else -> 0
                        }
                        val showInDoc = when {
                            fieldObj.has("show_in_doc") -> fieldObj.optBoolean("show_in_doc", true)
                            fieldObj.has("showInDoc") -> fieldObj.optBoolean("showInDoc", true)
                            fieldObj.has("hien_trong_doc") -> fieldObj.optBoolean("hien_trong_doc", true)
                            else -> true
                        }

                        val sources = mutableListOf<String>()
                        val sourcesArray = fieldObj.optJSONArray("source_categories")
                            ?: fieldObj.optJSONArray("sourceCategories")
                            ?: fieldObj.optJSONArray("muc_nguon")
                        if (sourcesArray != null) {
                            for (j in 0 until sourcesArray.length()) {
                                sources.add(sourcesArray.optString(j))
                            }
                        } else {
                            val singleSource = fieldObj.optString("source").ifBlank { fieldObj.optString("nguon") }
                            if (singleSource.isNotBlank()) sources.add(singleSource)
                        }

                        val startIndex = when {
                            fieldObj.has("startIndex") -> fieldObj.optInt("startIndex", -1)
                            fieldObj.has("start_index") -> fieldObj.optInt("start_index", -1)
                            else -> -1
                        }
                        val endIndex = when {
                            fieldObj.has("endIndex") -> fieldObj.optInt("endIndex", -1)
                            fieldObj.has("end_index") -> fieldObj.optInt("end_index", -1)
                            else -> -1
                        }
                        val tieuDeGanNhat = fieldObj.optString("tieuDeGanNhat").ifBlank {
                            fieldObj.optString("tieu_de_gan_nhat").ifBlank {
                                fieldObj.optString("heading", "")
                            }
                        }
                        val truoc = fieldObj.optString("truoc").ifBlank { fieldObj.optString("before", "") }
                        val sau = fieldObj.optString("sau").ifBlank { fieldObj.optString("after", "") }
                        val tinHieu = fieldObj.optString("tinHieu").ifBlank { fieldObj.optString("tin_hieu", "") }

                        summaryFields[key] = SummaryFieldConfig(
                            type = type,
                            maxLines = maxLines,
                            maxCharsPerLine = maxCharsPerLine,
                            maxChars = maxChars,
                            sourceCategories = sources,
                            showInDoc = showInDoc,
                            startIndex = startIndex,
                            endIndex = endIndex,
                            tieuDeGanNhat = tieuDeGanNhat,
                            truoc = truoc,
                            sau = sau,
                            tinHieu = tinHieu
                        )
                    }
                }

                TemplateSchema(
                    id = id,
                    name = name,
                    groupByType = groupByType,
                    maxPages = maxPages,
                    noteCategories = categories,
                    summaryFields = summaryFields
                )
            } catch (_: Exception) {
                TemplateSchema(
                    id = "fallback",
                    name = "Mẫu tùy chỉnh",
                    groupByType = "ngay",
                    maxPages = 2,
                    noteCategories = emptyList(),
                    summaryFields = emptyMap()
                )
            }
        }
    }
}
