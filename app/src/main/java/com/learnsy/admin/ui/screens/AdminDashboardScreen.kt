package com.learnsy.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learnsy.admin.data.DashboardStats
import com.learnsy.admin.data.Lesson
import com.learnsy.admin.data.buildStats
import com.learnsy.admin.ui.branding.AtomBadge
import com.learnsy.admin.ui.components.*
import com.learnsy.admin.ui.theme.Baloo2FontFamily
import com.learnsy.admin.ui.theme.LearnsyColors

enum class DashboardTab(val label: String) {
    OVERVIEW("Tổng quan"), CHARTS("Biểu đồ"), RESULTS("Kết quả"), SETTINGS("Cài đặt")
}

// Tương đương AdminDashboard trong dashboard.jsx
@Composable
fun AdminDashboardScreen(
    lessons: List<Lesson>,
    dark: Boolean,
    colors: LearnsyColors,
    adminName: String,
    onDarkToggle: () -> Unit,
    onClose: () -> Unit,
    onLogout: () -> Unit = {}
) {
    var tab by remember { mutableStateOf(DashboardTab.OVERVIEW) }
    val stats = remember(lessons) { buildStats(lessons) }

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(modifier = Modifier.fillMaxWidth().background(colors.surface).statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp, 11.dp, 14.dp, 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        shape = RoundedCornerShape(99.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Quản lý", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Logo — khối "L" trên nền gradient hồng-tím bo góc,
                        // đồng bộ với header index (app học sinh).
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(
                                    Brush.verticalGradient(listOf(Color(0xFFF9A8D4), Color(0xFFC084FC))),
                                    RoundedCornerShape(9.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AtomBadge(size = 17.dp, badgeColor = Color.White, backgroundColor = Color.Transparent)
                        }
                        Text("Learnsy", fontWeight = FontWeight.Black, fontSize = 15.sp, fontFamily = Baloo2FontFamily, color = colors.text)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(Color(0xFFA855F7).copy(alpha = 0.1f))
                                .border(1.5.dp, Color(0xFFA855F7).copy(alpha = 0.3f), RoundedCornerShape(99.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text("Admin", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFA855F7))
                        }
                    }

                    // Dark/Light — Box + clickable thuần, giống header index
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(if (dark) Color(0x26F59E0B) else Color(0x1AA855F7))
                            .border(1.5.dp, if (dark) Color(0x4DF59E0B) else Color(0x40A855F7), RoundedCornerShape(17.dp))
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { onDarkToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            null,
                            tint = if (dark) Color(0xFFF59E0B) else Color(0xFFA855F7),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    DashboardTab.values().forEach { t ->
                        val active = tab == t
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(if (active) Color(0xFFF472B6).copy(alpha = 0.14f) else Color.Transparent)
                                .border(
                                    1.5.dp,
                                    if (active) Color(0xFFF472B6).copy(alpha = 0.3f) else Color.Transparent,
                                    RoundedCornerShape(99.dp)
                                )
                                .clickable { tab = t }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                t.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (active) Color(0xFFF472B6) else colors.text3
                            )
                        }
                    }
                }
            }

            // Body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp, 14.dp, 12.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tab) {
                    DashboardTab.OVERVIEW -> OverviewTab(lessons, stats, adminName, colors)
                    DashboardTab.CHARTS -> ChartsTab(lessons, stats, colors)
                    DashboardTab.RESULTS -> ResultsPanel(colors)
                    DashboardTab.SETTINGS -> SettingsPanel(dark, colors, onDarkToggle, lessons, onLogout)
                }
            }
        }

        DiToastHost()
    }
}

@Composable
private fun OverviewTab(
    lessons: List<Lesson>,
    stats: DashboardStats,
    adminName: String,
    colors: LearnsyColors
) {
    val hour = remember { java.time.LocalTime.now().hour }
    val greeting = when {
        hour < 5 -> "Làm việc khuya vậy 🌙"
        hour < 12 -> "Chào buổi sáng ☀️"
        hour < 18 -> "Chào buổi chiều 🌤️"
        else -> "Chào buổi tối 🌙"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFFF0F5), Color(0xFFF0E6FF))))
            .border(1.5.dp, Color(0xFFF472B6).copy(alpha = 0.3f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFF472B6), Color(0xFFA855F7)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MenuBook, null, tint = Color.White)
        }
        Column {
            Text(greeting, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
            Text("Xin chào, $adminName 👋", fontSize = 17.sp, fontWeight = FontWeight.Black, color = colors.text)
            Text(
                "${lessons.size} bộ câu hỏi · ${stats.totalQ} câu · ${stats.subjects.size} môn học",
                fontSize = 12.sp, color = colors.text3
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            KpiCard(
                icon = { Icon(Icons.Default.MenuBook, null, tint = Color(0xFFA855F7)) },
                label = "Bộ câu hỏi", value = "${stats.total}", sub = "${stats.subjects.size} môn học",
                color = Color(0xFFA855F7), modifier = Modifier.weight(1f)
            )
            KpiCard(
                icon = { Icon(Icons.Default.Info, null, tint = Color(0xFFF472B6)) },
                label = "Tổng câu hỏi", value = "${stats.totalQ}", sub = "Tất cả các bộ",
                color = Color(0xFFF472B6), modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            KpiCard(
                icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981)) },
                label = "Trắc nghiệm", value = "${stats.types.multiple + stats.types.multiSelect}", sub = "MC + multi-select",
                color = Color(0xFF10B981), modifier = Modifier.weight(1f)
            )
            KpiCard(
                icon = { Icon(Icons.Default.Star, null, tint = Color(0xFFF97316)) },
                label = "Điền từ", value = "${stats.types.fillBlank}", sub = "Fill-in-the-blank",
                color = Color(0xFFF97316), modifier = Modifier.weight(1f)
            )
        }
    }

    SectionCard(colors) {
        SectionTitle("Tỷ lệ loại câu hỏi", colors)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            RadialProgressView(stats.types.trueFalse, stats.totalQ.coerceAtLeast(1), Color(0xFFA855F7), "Đúng/Sai", colors)
            RadialProgressView(stats.types.multiple, stats.totalQ.coerceAtLeast(1), Color(0xFFF472B6), "Trắc nghiệm", colors)
            RadialProgressView(stats.types.multiSelect, stats.totalQ.coerceAtLeast(1), Color(0xFF10B981), "Chọn nhiều", colors)
            RadialProgressView(stats.types.fillBlank, stats.totalQ.coerceAtLeast(1), Color(0xFFF97316), "Điền từ", colors)
        }
    }

    SectionCard(colors) {
        SectionTitle("Bài học theo tháng", colors)
        AreaChartView(
            data = stats.monthly.map { ChartBar(it.label, it.count) },
            color = Color(0xFFA855F7), colors = colors
        )
    }

    if (stats.subjects.isNotEmpty()) {
        SectionCard(colors) {
            SectionTitle("Phân bố theo môn học", colors)
            val paletteColors = listOf(Color(0xFFF472B6), Color(0xFFA855F7), Color(0xFF10B981), Color(0xFFF97316), Color(0xFF06B6D4), Color(0xFFEAB308))
            DonutChartView(
                slices = stats.subjects.entries.mapIndexed { i, e -> DonutSlice(e.key, e.value, paletteColors[i % paletteColors.size]) },
                colors = colors
            )
        }
    }
}

@Composable
private fun ChartsTab(lessons: List<Lesson>, stats: DashboardStats, colors: LearnsyColors) {
    val typeBars = listOf(
        ChartBar("Đúng/Sai", stats.types.trueFalse, Color(0xFFA855F7)),
        ChartBar("Trắc nghiệm", stats.types.multiple, Color(0xFFF472B6)),
        ChartBar("Chọn nhiều", stats.types.multiSelect, Color(0xFF10B981)),
        ChartBar("Điền từ", stats.types.fillBlank, Color(0xFFF97316))
    )

    SectionCard(colors) {
        SectionTitle("Số câu hỏi theo loại", colors)
        BarChartView(typeBars, Color(0xFFF472B6), colors)
    }

    SectionCard(colors) {
        SectionTitle("Xu hướng bài học", colors)
        AreaChartView(stats.monthly.map { ChartBar(it.label, it.count) }, Color(0xFF10B981), colors)
    }

    if (stats.subjects.isNotEmpty()) {
        SectionCard(colors) {
            SectionTitle("Môn học (ngang)", colors)
            val subjEntries = stats.subjects.entries.sortedByDescending { it.value }
            val maxCount = subjEntries.maxOf { it.value }
            val paletteColors = listOf(Color(0xFFF472B6), Color(0xFFA855F7), Color(0xFF10B981), Color(0xFFF97316), Color(0xFF06B6D4), Color(0xFFEAB308))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                subjEntries.forEachIndexed { i, e ->
                    val c = paletteColors[i % paletteColors.size]
                    val pct = e.value.toFloat() / maxCount
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(e.key, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = colors.text3, modifier = Modifier.width(72.dp), maxLines = 1)
                        Box(modifier = Modifier.weight(1f).height(22.dp).clip(RoundedCornerShape(99.dp)).background(Color.Black.copy(alpha = 0.04f))) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(pct)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(c),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text("${e.value}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(end = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    SectionCard(colors) {
        SectionTitle("Tóm tắt dữ liệu", colors)
        val avgPerSet = if (stats.total > 0) stats.totalQ / stats.total else 0
        val biggest = lessons.maxByOrNull { it.questions.size }?.title?.take(12) ?: "–"
        val topSubject = stats.subjects.entries.maxByOrNull { it.value }?.key?.take(10) ?: "–"
        val topType = listOf(
            "Đúng/Sai" to stats.types.trueFalse, "Trắc nghiệm" to stats.types.multiple,
            "Chọn nhiều" to stats.types.multiSelect, "Điền từ" to stats.types.fillBlank
        ).maxByOrNull { it.second }?.first ?: "–"

        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            MiniSummary("TB câu/bộ", "$avgPerSet", Color(0xFFF472B6), colors, Modifier.weight(1f))
            MiniSummary("Bộ lớn nhất", biggest, Color(0xFFA855F7), colors, Modifier.weight(1f))
        }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            MiniSummary("Môn nhiều", topSubject, Color(0xFF10B981), colors, Modifier.weight(1f))
            MiniSummary("Loại nhiều", topType, Color(0xFFF97316), colors, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniSummary(label: String, value: String, color: Color, colors: LearnsyColors, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(11.dp, 13.dp)
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.text3)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
private fun SectionCard(colors: LearnsyColors, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.5.dp, colors.border, RoundedCornerShape(20.dp))
            .padding(15.dp, 14.dp),
        content = content
    )
}

@Composable
private fun SectionTitle(text: String, colors: LearnsyColors) {
    Text(
        text.uppercase(),
        modifier = Modifier.padding(bottom = 13.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        color = colors.text2
    )
}
