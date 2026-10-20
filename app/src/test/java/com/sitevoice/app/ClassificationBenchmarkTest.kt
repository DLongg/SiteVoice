package com.sitevoice.app

import org.json.JSONArray
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader

data class GoldenSample(
    val id: Long,
    val text: String,
    val expectedLabel: String,
    val category: String
)

data class BenchmarkMetrics(
    val totalSamples: Int,
    val correctCount: Int,
    val accuracyPercentage: Double,
    val categoryAccuracies: Map<String, Double>,
    val confusionMatrix: Map<String, Map<String, Int>>
)

class ClassificationBenchmarkTest {

    fun loadGoldenDataset(): List<GoldenSample> {
        val stream = javaClass.classLoader?.getResourceAsStream("golden_dataset.json")
            ?: throw IllegalStateException("Không tìm thấy file golden_dataset.json trong resources!")

        val jsonString = InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }
        val array = JSONArray(jsonString)
        val list = mutableListOf<GoldenSample>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                GoldenSample(
                    id = obj.getLong("id"),
                    text = obj.getString("text"),
                    expectedLabel = obj.getString("expected_label"),
                    category = obj.optString("category", "default")
                )
            )
        }
        return list
    }

    /**
     * Mô phỏng bộ phân loại dựa trên Heuristics từ vựng hiện tại (Baseline ban đầu trước khi có Harness nâng cao).
     */
    fun baselineHeuristicClassifier(text: String): String {
        val lower = text.lowercase().trim()

        // 1. Cue words mở đầu
        if (lower.startsWith("thi công:") || lower.startsWith("thi công :")) return "thi_cong"
        if (lower.startsWith("nghiệm thu:") || lower.startsWith("nghiệm thu :")) return "nghiem_thu"
        if (lower.startsWith("an toàn:") || lower.startsWith("an toàn :")) return "an_toan"
        if (lower.startsWith("chủ đầu tư:") || lower.startsWith("cđt:")) return "y_kien_cdt"
        if (lower.startsWith("tư vấn giám sát:") || lower.startsWith("tvgs:") || lower.startsWith("giám sát:")) return "y_kien_tvgs"
        if (lower.startsWith("nhà thầu:")) return "y_kien_nha_thau"

        // 2. Từ khóa đặc thù đơn giản
        if (lower.contains("chủ đầu tư") || lower.contains("cđt") || lower.contains("c d t")) {
            if (lower.contains("yêu cầu") || lower.contains("nhắc nhở") || lower.contains("đồng ý") || lower.contains("nhắc")) {
                return "y_kien_cdt"
            }
        }

        if (lower.contains("tư vấn giám sát") || lower.contains("tvgs") || lower.contains("t v g s") || lower.contains("kỹ sư giám sát")) {
            if (lower.contains("yêu cầu") || lower.contains("nhắc nhở") || lower.contains("đề nghị")) {
                return "y_kien_tvgs"
            }
        }

        if (lower.contains("nhà thầu") && (lower.contains("kiến nghị") || lower.contains("đề nghị") || lower.contains("đề xuất") || lower.contains("xin phê duyệt"))) {
            return "y_kien_nha_thau"
        }

        if (lower.contains("dây an toàn") || lower.contains("mũ bảo hộ") || lower.contains("lan can") || lower.contains("rào chắn") || lower.contains("sự cố") || lower.contains("vệ sinh môi trường") || lower.contains("bình chữa cháy") || lower.contains("kính hàn")) {
            return "an_toan"
        }

        if (lower.contains("nghiệm thu") || lower.contains("bàn giao") || lower.contains("biên bản") || lower.contains("tổ mẫu") || lower.contains("độ sụt")) {
            return "nghiem_thu"
        }

        if (lower.contains("đổ bê tông") || lower.contains("cốt thép") || lower.contains("cát") || lower.contains("xi măng") || lower.contains("xây tường") || lower.contains("cẩu tháp") || lower.contains("bảo dưỡng") || lower.contains("hàn") || lower.contains("coffa") || lower.contains("cốt pha") || lower.contains("lắp đặt") || lower.contains("máy đục")) {
            return "thi_cong"
        }

        return "chua_phan_loai"
    }

    fun runBenchmark(predictFn: (String) -> String): BenchmarkMetrics {
        val samples = loadGoldenDataset()
        var correct = 0
        val catTotal = mutableMapOf<String, Int>()
        val catCorrect = mutableMapOf<String, Int>()

        val labels = listOf("thi_cong", "nghiem_thu", "an_toan", "y_kien_cdt", "y_kien_tvgs", "y_kien_nha_thau", "chua_phan_loai")
        val matrix = mutableMapOf<String, MutableMap<String, Int>>()
        for (actual in labels) {
            matrix[actual] = mutableMapOf()
            for (pred in labels) {
                matrix[actual]!![pred] = 0
            }
        }

        for (sample in samples) {
            val predicted = predictFn(sample.text)
            val expected = sample.expectedLabel

            catTotal[sample.category] = (catTotal[sample.category] ?: 0) + 1

            if (predicted == expected) {
                correct++
                catCorrect[sample.category] = (catCorrect[sample.category] ?: 0) + 1
            }

            val actualRow = matrix.getOrPut(expected) { mutableMapOf() }
            actualRow[predicted] = (actualRow[predicted] ?: 0) + 1
        }

        val catAcc = mutableMapOf<String, Double>()
        for ((cat, total) in catTotal) {
            val corr = catCorrect[cat] ?: 0
            catAcc[cat] = (corr.toDouble() / total.toDouble()) * 100.0
        }

        val accuracy = (correct.toDouble() / samples.size.toDouble()) * 100.0
        return BenchmarkMetrics(
            totalSamples = samples.size,
            correctCount = correct,
            accuracyPercentage = accuracy,
            categoryAccuracies = catAcc,
            confusionMatrix = matrix
        )
    }

    @Test
    fun testGoldenDatasetLoading() {
        val samples = loadGoldenDataset()
        assertTrue("Bộ dữ liệu vàng phải có ít nhất 50 mẫu", samples.size >= 50)
        println("=> Đã nạp thành công ${samples.size} mẫu trong Golden Dataset.")
    }

    @Test
    fun testBaselineBenchmarkExecution() {
        val metrics = runBenchmark { text -> baselineHeuristicClassifier(text) }

        println("\n=======================================================")
        println("📊 KẾT QUẢ KIỂM CHUẨN ĐỘ CHÍNH XÁC PHÂN LOẠI (BASELINE)")
        println("=======================================================")
        println("Tổng số mẫu kiểm thử: ${metrics.totalSamples}")
        println("Số câu chính xác    : ${metrics.correctCount}")
        println("Độ chính xác tổng   : String.format(\"%.2f%%\", metrics.accuracyPercentage) -> ${String.format("%.2f%%", metrics.accuracyPercentage)}")
        println("-------------------------------------------------------")
        println("Độ chính xác theo nhóm kiểm thử:")
        for ((cat, acc) in metrics.categoryAccuracies) {
            println("  - $cat: ${String.format("%.1f%%", acc)}")
        }
        println("-------------------------------------------------------")
        println("Confusion Matrix (Hàng: Thực tế -> Cột: Dự đoán):")
        val labels = listOf("thi_cong", "nghiem_thu", "an_toan", "y_kien_cdt", "y_kien_tvgs", "y_kien_nha_thau", "chua_phan_loai")
        print(String.format("%-16s", "Thực tế \\ Dự đoán"))
        for (l in labels) print(String.format("%-10s", l.take(8)))
        println()

        for (actual in labels) {
            print(String.format("%-16s", actual))
            val row = metrics.confusionMatrix[actual] ?: emptyMap()
            for (pred in labels) {
                print(String.format("%-10d", row[pred] ?: 0))
            }
            println()
        }
        println("=======================================================\n")

        // Kiểm tra baseline tối thiểu đạt trên 70%
        assertTrue("Baseline heuristics phải đạt ít nhất 70% độ chính xác", metrics.accuracyPercentage >= 70.0)
    }
}
