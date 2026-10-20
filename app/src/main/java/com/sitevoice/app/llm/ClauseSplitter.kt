package com.sitevoice.app.llm

object ClauseSplitter {

    private val SPLIT_CONJUNCTIONS = listOf(
        ", chiều ",
        ", buổi chiều ",
        ", sáng ",
        ", buổi sáng ",
        ", tối ",
        ", buổi tối ",
        ", sau đó ",
        ", đồng thời ",
        ", tiếp theo ",
        ", TVGS ",
        ", CĐT ",
        ", chủ đầu tư ",
        ", tư vấn giám sát ",
        ", nhà thầu "
    )

    /**
     * Tách một đoạn văn bản thành các mệnh đề / câu con độc lập.
     */
    fun splitIntoClauses(text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return emptyList()

        // 1. Tách theo dấu câu cơ bản: chấm, chấm phẩy, xuống dòng
        val rawSentences = trimmed.split(Regex("[.;\n]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (rawSentences.isEmpty()) return listOf(trimmed)

        // 2. Với mỗi câu, kiểm tra xem có thể tách tiếp bằng các liên từ hay không
        val result = mutableListOf<String>()

        for (sentence in rawSentences) {
            var workingParts = listOf(sentence)

            for (conj in SPLIT_CONJUNCTIONS) {
                val nextParts = mutableListOf<String>()
                for (part in workingParts) {
                    if (part.contains(conj, ignoreCase = true)) {
                        // Tách nhưng giữ lại chủ thể cho mệnh đề sau
                        val idx = part.indexOf(conj, ignoreCase = true)
                        val part1 = part.substring(0, idx).trim()
                        val part2 = part.substring(idx + 1).trim() // bỏ dấu phẩy, giữ lại liên từ/chủ thể

                        if (part1.isNotBlank()) nextParts.add(part1)
                        if (part2.isNotBlank()) nextParts.add(part2)
                    } else {
                        nextParts.add(part)
                    }
                }
                workingParts = nextParts
            }

            result.addAll(workingParts)
        }

        return if (result.isEmpty()) listOf(trimmed) else result
    }
}
