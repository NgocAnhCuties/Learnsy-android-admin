package com.learnsy.admin.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Tương đương xepLoai() trong score.js ─────────────────────────
data class Rank(val label: String, val emoji: String, val color: String)

fun xepLoai(diem10: Double): Rank = when {
    diem10 >= 9   -> Rank("Xuất sắc", "🏆", "#10b981")
    diem10 >= 8   -> Rank("Giỏi", "🥇", "#f59e0b")
    diem10 >= 6.5 -> Rank("Khá", "🥈", "#a855f7")
    diem10 >= 5   -> Rank("Trung bình", "👍", "#f472b6")
    else          -> Rank("Cần cố gắng", "📚", "#ef4444")
}

fun calcDiem10(score: Int, total: Int): Double {
    if (total <= 0) return 0.0
    return Math.round((score.toDouble() / total) * 100) / 10.0
}

// ── quiz_results row ──────────────────────────────────────────────
@Serializable
data class QuizResult(
    val id: String? = null,
    @SerialName("student_name") val studentName: String,
    @SerialName("student_id") val studentId: String? = null,
    @SerialName("lesson_id") val lessonId: String,
    @SerialName("lesson_title") val lessonTitle: String,
    val score: Int,
    val total: Int,
    val diem10: Double,
    val pct: Int,
    @SerialName("xep_loai") val xepLoaiLabel: String,
    @SerialName("question_count") val questionCount: Int,
    @SerialName("submitted_at") val submittedAt: String
)

// ── Summary aggregate (tương đương summary=1 trong score.js) ────
data class ScoreDist(
    val nineToTen: Int = 0,
    val sevenToNine: Int = 0,
    val fiveToSeven: Int = 0,
    val belowFive: Int = 0
)

data class ScoreSummary(
    val lessonId: String,
    val count: Int,
    val avgDiem10: Double,
    val maxDiem10: Double,
    val minDiem10: Double,
    val dist: ScoreDist,
    val top5: List<QuizResult>,
    val rows: List<QuizResult>
)

fun buildSummary(lessonId: String, rows: List<QuizResult>): ScoreSummary? {
    if (rows.isEmpty()) return null
    val diem10s = rows.map { it.diem10 }
    val avg = Math.round((diem10s.sum() / diem10s.size) * 10) / 10.0

    var d9 = 0; var d7 = 0; var d5 = 0; var dLow = 0
    diem10s.forEach { d ->
        when {
            d >= 9 -> d9++
            d >= 7 -> d7++
            d >= 5 -> d5++
            else -> dLow++
        }
    }

    val top5 = rows.sortedByDescending { it.diem10 }.take(5)

    return ScoreSummary(
        lessonId = lessonId,
        count = rows.size,
        avgDiem10 = avg,
        maxDiem10 = diem10s.max(),
        minDiem10 = diem10s.min(),
        dist = ScoreDist(d9, d7, d5, dLow),
        top5 = top5,
        rows = rows
    )
}
