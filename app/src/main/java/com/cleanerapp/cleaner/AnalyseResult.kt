package com.cleanerapp.cleaner

data class CategoryAnalysis(
    val name: String,
    val sizeBytes: Long,
    val filesCount: Int
)
data class AnalyseResult(
    val categories: List<CategoryAnalysis>
) {
    val totalBytes: Long
        get() {
            var total = 0L
            for (cat in categories) total += cat.sizeBytes
            return total
        }

    val totalFiles: Int
        get() {
            var total = 0
            for (cat in categories) total += cat.filesCount
            return total
        }
}