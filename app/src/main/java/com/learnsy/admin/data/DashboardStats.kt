package com.learnsy.admin.data

import java.time.LocalDate
import java.time.YearMonth

data class MonthlyCount(val label: String, val count: Int)

data class QuestionTypeCounts(
    val trueFalse: Int = 0,
    val multiple: Int = 0,
    val multiSelect: Int = 0,
    val fillBlank: Int = 0
)

data class DashboardStats(
    val total: Int,
    val totalQ: Int,
    val subjects: Map<String, Int>,
    val monthly: List<MonthlyCount>,
    val types: QuestionTypeCounts
)

// Tương đương buildStats() trong dashboard.jsx
fun buildStats(lessons: List<Lesson>): DashboardStats {
    val total = lessons.size
    val totalQ = lessons.sumOf { it.questions.size }

    val subjects = mutableMapOf<String, Int>()
    lessons.forEach { l ->
        val s = l.subject.ifBlank { "Khác" }
        subjects[s] = (subjects[s] ?: 0) + 1
    }

    // 6 tháng gần nhất, tương đương vòng lặp i=5..0
    val now = YearMonth.now()
    val monthly = (5 downTo 0).map { i ->
        val ym = now.minusMonths(i.toLong())
        val label = "T${ym.monthValue}"
        val count = lessons.count { l ->
            val created = parseCreatedAt(l.createdAt)
            created != null && created.year == ym.year && created.monthValue == ym.monthValue
        }
        MonthlyCount(label, count)
    }

    var tf = 0; var mc = 0; var ms = 0; var fb = 0
    lessons.forEach { l ->
        l.questions.forEach { q ->
            when (q) {
                is Question.TrueFalse -> tf++
                is Question.Multiple -> mc++
                is Question.MultiSelect -> ms++
                is Question.FillBlank -> fb++
            }
        }
    }

    return DashboardStats(total, totalQ, subjects, monthly, QuestionTypeCounts(tf, mc, ms, fb))
}

private fun parseCreatedAt(raw: String): LocalDate? {
    if (raw.isBlank()) return null
    return runCatching {
        java.time.Instant.parse(raw).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    }.getOrElse {
        runCatching { LocalDate.parse(raw.take(10)) }.getOrNull()
    }
}
