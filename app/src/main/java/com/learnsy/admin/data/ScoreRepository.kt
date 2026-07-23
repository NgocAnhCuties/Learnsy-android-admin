package com.learnsy.admin.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable

// Native app gọi thẳng Supabase Postgrest, không qua Cloudflare Function
// /api/score nữa (đó chỉ là proxy để giấu SUPA_KEY khỏi client web).
// Rate limit (5 POST/studentId/60s trong score.js) cần dời sang
// Supabase RLS policy hoặc Edge Function nếu muốn giữ, native client
// không tự enforce được rate limit phía server.

@Serializable
data class SubmitScorePayload(
    val student_name: String,
    val student_id: String? = null,
    val lesson_id: String,
    val lesson_title: String,
    val score: Int,
    val total: Int,
    val diem10: Double,
    val pct: Int,
    val xep_loai: String,
    val question_count: Int,
    val submitted_at: String
)

class ScoreRepository {
    private val table = SupabaseConfig.client.from("quiz_results")

    suspend fun submit(
        lessonId: String,
        lessonTitle: String,
        studentName: String,
        studentId: String?,
        score: Int,
        total: Int
    ): QuizResult {
        val diem10 = calcDiem10(score, total)
        val pct = Math.round((score.toDouble() / total) * 100).toInt()
        val rank = xepLoai(diem10)
        val submittedAt = java.time.Instant.now().toString()

        val payload = SubmitScorePayload(
            student_name = studentName.trim().take(100).ifBlank { "Ẩn danh" },
            student_id = studentId,
            lesson_id = lessonId.trim().take(200),
            lesson_title = lessonTitle.trim().take(200).ifBlank { "Không rõ" },
            score = score,
            total = total,
            diem10 = diem10,
            pct = pct,
            xep_loai = rank.label,
            question_count = total,
            submitted_at = submittedAt
        )

        table.upsert(payload, onConflict = "student_id,lesson_id")

        return QuizResult(
            studentName = payload.student_name,
            studentId = payload.student_id,
            lessonId = payload.lesson_id,
            lessonTitle = payload.lesson_title,
            score = score,
            total = total,
            diem10 = diem10,
            pct = pct,
            xepLoaiLabel = rank.label,
            questionCount = total,
            submittedAt = submittedAt
        )
    }

    suspend fun byLesson(lessonId: String, limit: Long = 100, offset: Long = 0): List<QuizResult> {
        return table.select {
            filter { eq("lesson_id", lessonId) }
            order("submitted_at", Order.DESCENDING)
            limit(limit)
            range(offset, offset + limit - 1)
        }.decodeList<QuizResult>()
    }

    suspend fun byStudent(studentId: String, limit: Long = 100, offset: Long = 0): List<QuizResult> {
        return table.select {
            filter { eq("student_id", studentId) }
            order("submitted_at", Order.DESCENDING)
            limit(limit)
            range(offset, offset + limit - 1)
        }.decodeList<QuizResult>()
    }

    suspend fun summaryByLesson(lessonId: String): ScoreSummary? {
        val rows = byLesson(lessonId, limit = 500)
        return buildSummary(lessonId, rows)
    }

    // Tương đương ResultsPanel load() — 50 kết quả gần nhất, mọi bài
    suspend fun recentResults(limit: Long = 50): List<QuizResult> {
        return table.select {
            order("submitted_at", Order.DESCENDING)
            limit(limit)
        }.decodeList<QuizResult>()
    }

    // Tương đương handleClearAll() — xoá toàn bộ quiz_results (.not('id','is',null)).
    // id luôn có giá trị (UUID), nên neq với chuỗi rỗng tương đương "xoá tất cả".
    suspend fun clearAll() {
        table.delete {
            filter {
                neq("id", "")
            }
        }
    }
}
