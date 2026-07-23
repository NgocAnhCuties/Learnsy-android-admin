package com.learnsy.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.learnsy.admin.data.Lesson
import com.learnsy.admin.data.LessonFilter
import com.learnsy.admin.data.SortBy
import com.learnsy.admin.data.CardBlur
import com.learnsy.admin.ui.LessonListViewModel
import com.learnsy.admin.ui.ToastCenter
import com.learnsy.admin.ui.theme.LearnsyColors

// Tương đương phần list chính của app.jsx, ghép LessonListViewModel với
// LessonEditorScreen (soạn bài) đã port trước đó.
@Composable
fun LessonListScreen(
    colors: LearnsyColors,
    refreshKey: Any = Unit,
    onEditingChanged: (Boolean) -> Unit = {},
    listVm: LessonListViewModel = viewModel()
) {
    val listState by listVm.uiState.collectAsState()
    var editingLesson by remember { mutableStateOf<Lesson?>(null) }
    var deleteTarget by remember { mutableStateOf<Lesson?>(null) }
    // Tương đương confirm_({title:'Tạo bài tập mới?',...}) trong app.jsx
    var showCreateConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(refreshKey) { listVm.load() }
    LaunchedEffect(editingLesson) { onEditingChanged(editingLesson != null) }
    // Trước đây listState.error được ghi nhưng không nơi nào đọc — lỗi tải bài
    // học (vd. sai Supabase URL/key, RLS chặn, mất mạng) bị nuốt câm, màn hình
    // chỉ hiện "Chưa có bài tập nào" giống hệt trường hợp thật sự trống dữ liệu.
    LaunchedEffect(listState.error) {
        listState.error?.let { msg -> ToastCenter.show("Lỗi tải bài học: $msg", "❌", Color(0xFFEF4444)) }
    }

    if (editingLesson != null) {
        LessonEditorScreen(
            colors = colors,
            lesson = editingLesson!!,
            allLessons = listState.lessons,
            onBack = { editingLesson = null; listVm.load() }
        )
        return
    }

    if (showCreateConfirm) {
        AlertDialog(
            onDismissRequest = { showCreateConfirm = false },
            title = { Text("Tạo bài tập mới?") },
            text = { Text("Bài tập mới sẽ được tạo và lưu vào Supabase.") },
            confirmButton = {
                TextButton(onClick = {
                    showCreateConfirm = false
                    listVm.createLesson(
                        onCreated = { id ->
                            val newLesson = listState.lessons.find { it.id == id } ?: Lesson(id = id)
                            editingLesson = newLesson
                        },
                        onError = { msg -> ToastCenter.show(msg, "❌", Color(0xFFEF4444)) }
                    )
                }) { Text("Tạo ngay", color = colors.lav) }
            },
            dismissButton = { TextButton(onClick = { showCreateConfirm = false }) { Text("Huỷ") } }
        )
    }

    deleteTarget?.let { lesson ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Xoá bài học?") },
            text = { Text(lesson.title.ifBlank { "(Chưa đặt tên)" }) },
            confirmButton = {
                TextButton(onClick = { deleteTarget = null; listVm.deleteLesson(lesson.id) }) {
                    Text("Xoá", color = Color(0xFFEF4444))
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Huỷ") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp, 14.dp, 12.dp, 100.dp)) {
        // Tương đương khối "Bài học" + nút thêm trong app.jsx (dòng ~1012-1032):
        // tiêu đề lớn "Bài học" + phụ đề tổng số bài/câu hỏi, viền dưới phân cách.
        Column(
            modifier = Modifier.fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = colors.border,
                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }.padding(bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bài học", fontSize = 18.sp, fontWeight = FontWeight.Black, color = colors.text)
                    val totalQ = listState.lessons.sumOf { it.questions.size }
                    Text(
                        "${listState.lessons.size} bài · $totalQ câu hỏi",
                        fontSize = 12.sp, color = colors.text3,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (listState.lessons.isNotEmpty()) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(colors.grad)
                            .clickable { showCreateConfirm = true }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Thêm bài mới", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }

        if (listState.lessons.isNotEmpty()) {
            OutlinedTextField(
                value = listState.searchQuery, onValueChange = listVm::setSearchQuery,
                placeholder = { Text("🔍 Tìm kiếm bài tập...") }, singleLine = true,
                shape = RoundedCornerShape(999.dp),
                trailingIcon = {
                    if (listState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { listVm.setSearchQuery("") }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, "Xoá tìm kiếm", tint = colors.text3, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            )

            // Tương đương hàng "Bộ lọc + sắp xếp" gộp trong app.jsx (dòng ~1044-1113):
            // filter segmented (flex:1) cạnh dropdown sort+blur (flex-shrink:0), CÙNG 1 hàng.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp))
                        .background(colors.bg).border(1.5.dp, colors.border, RoundedCornerShape(999.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val allCount = listState.lessons.size
                    val enCount = listState.lessons.count { it.subject == "Tiếng Anh" }
                    val otherCount = allCount - enCount
                    listOf(
                        Triple(LessonFilter.ALL, "Tất cả", allCount),
                        Triple(LessonFilter.ENGLISH, "Tiếng Anh", enCount),
                        Triple(LessonFilter.OTHER, "Các môn", otherCount)
                    ).forEach { (f, label, count) ->
                        val active = listState.filter == f
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp))
                                .then(if (active) Modifier.background(colors.grad) else Modifier)
                                .clickable { listVm.setFilter(f) }
                                .padding(horizontal = 4.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$label · $count", fontSize = 11.5.sp, fontWeight = FontWeight.Black,
                                color = if (active) Color.White else colors.text3,
                                maxLines = 1
                            )
                        }
                    }
                }

                SortAndBlurDropdown(
                    colors = colors,
                    sortBy = listState.sortBy, onSortSelect = listVm::setSortBy,
                    cardBlur = listState.cardBlur, onBlurSelect = listVm::setCardBlur
                )
            }
        }

        if (listState.loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.rose)
            }
        } else {
            val display = listVm.filteredSortedLessons()
            if (display.isEmpty() && listState.lessons.isEmpty()) {
                // Tương đương empty state trong app.jsx (dòng ~1117-1130):
                // dashed border, icon hoa trong vòng tròn gradient, tiêu đề + phụ đề, nút CTA riêng.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.surface)
                        .border(1.5.dp, colors.border2, RoundedCornerShape(24.dp))
                        .padding(vertical = 40.dp, horizontal = 20.dp)
                ) {
                    Box(
                        modifier = Modifier.size(76.dp).clip(RoundedCornerShape(999.dp))
                            .background(colors.gradSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        com.learnsy.admin.ui.components.FlowerIcon(size = 40, color = Color(0xFFFFB7C9))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Chưa có bài tập nào", fontSize = 16.sp, fontWeight = FontWeight.Black, color = colors.text2)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Bấm nút bên dưới để tạo bài tập\nđầu tiên cho lớp của bạn nhé!",
                        fontSize = 12.5.sp, color = colors.text3, lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(colors.grad)
                            .clickable { showCreateConfirm = true }
                            .padding(horizontal = 26.dp, vertical = 11.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Thêm bài đầu tiên", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            } else if (display.isEmpty()) {
                // Tương đương thông báo rỗng theo filter trong app.jsx
                val msg = when (listState.filter) {
                    LessonFilter.ENGLISH -> "Chưa có bài tập Tiếng Anh nào"
                    LessonFilter.OTHER -> "Chưa có bài tập các môn nào"
                    LessonFilter.ALL -> "Không tìm thấy bài học phù hợp"
                }
                Text(msg, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text3, modifier = Modifier.padding(vertical = 24.dp))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    display.forEach { lesson ->
                        LessonCard(
                            lesson = lesson, colors = colors, cardBlur = listState.cardBlur,
                            onOpen = { editingLesson = lesson },
                            onDuplicate = { listVm.duplicateLesson(lesson) { } },
                            onDelete = { deleteTarget = lesson }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortAndBlurDropdown(
    colors: LearnsyColors,
    sortBy: SortBy, onSortSelect: (SortBy) -> Unit,
    cardBlur: CardBlur, onBlurSelect: (CardBlur) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val sortOptions = listOf(
        SortBy.NEWEST to "Mới nhất", SortBy.OLDEST to "Cũ nhất",
        SortBy.NAME to "Tên A-Z", SortBy.COUNT to "Nhiều câu nhất"
    )
    val sortLabel = sortOptions.find { it.first == sortBy }?.second ?: "Mới nhất"
    val blurOptions = listOf(CardBlur.OFF to "Tắt", CardBlur.FIFTY to "50%", CardBlur.EIGHTY_FIVE to "85%")

    Box {
        Row(
            modifier = Modifier.clip(RoundedCornerShape(999.dp))
                .background(if (expanded) colors.lavL else colors.bg2)
                .border(1.5.dp, if (expanded) colors.lav else colors.border, RoundedCornerShape(999.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Sort, null, tint = if (expanded) colors.lav else colors.text3, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text(sortLabel, fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = if (expanded) colors.lav else colors.text3)
            Spacer(Modifier.width(3.dp))
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null, tint = if (expanded) colors.lav else colors.text3, modifier = Modifier.size(14.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                "SẮP XẾP THEO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = colors.text4,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
            sortOptions.forEach { (value, text) ->
                val active = sortBy == value
                DropdownMenuItem(
                    text = { Text(text, fontWeight = if (active) FontWeight.Black else FontWeight.Bold, color = if (active) colors.lav else colors.text2) },
                    trailingIcon = { if (active) Icon(Icons.Default.Check, null, tint = colors.lav, modifier = Modifier.size(14.dp)) },
                    onClick = { onSortSelect(value) }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = colors.border)
            Text(
                "ĐỘ MỜ THẺ", fontSize = 10.sp, fontWeight = FontWeight.Black, color = colors.text4,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                blurOptions.forEach { (value, text) ->
                    val active = cardBlur == value
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (active) colors.lavL else Color.Transparent)
                            .border(1.5.dp, if (active) colors.lav else colors.border, RoundedCornerShape(8.dp))
                            .clickable { onBlurSelect(value) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (active) colors.lav else colors.text3)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniBadge(text: String, color: Color, bg: Color) {
    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 7.dp, vertical = 2.dp)) {
        Text(text, fontSize = 9.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
private fun LessonCard(
    lesson: Lesson, colors: LearnsyColors, cardBlur: CardBlur,
    onOpen: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit
) {
    val blurRadius = when (cardBlur) {
        CardBlur.OFF -> 0.dp
        CardBlur.FIFTY -> 3.dp
        CardBlur.EIGHTY_FIVE -> 7.dp
    }
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp), ambientColor = colors.lav.copy(alpha = 0.15f), spotColor = colors.lav.copy(alpha = 0.15f))
            .clip(RoundedCornerShape(24.dp)).background(colors.surface)
            .clickable(onClick = onOpen)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon — pastel tím-hồng nhạt, đồng bộ tông "mềm" của app.jsx (không dùng gradient đậm)
        Box(
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFFDEBF3)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Description, null, tint = Color(0xFFC4A0F0), modifier = Modifier.size(24.dp))
        }
        Column(
            modifier = Modifier.weight(1f).let {
                if (blurRadius > 0.dp) it.blur(blurRadius) else it
            }
        ) {
            Text(lesson.title.ifBlank { "Chưa đặt tên" }, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = colors.text, maxLines = 1)
            Text(
                "${lesson.subject.ifBlank { "Tiếng Anh" }} · ${lesson.questions.size} câu",
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.text3,
                modifier = Modifier.padding(top = 4.dp)
            )
            val tfCount = lesson.questions.count { it is com.learnsy.admin.data.Question.TrueFalse }
            val tnCount = lesson.questions.count { it is com.learnsy.admin.data.Question.Multiple || it is com.learnsy.admin.data.Question.MultiSelect }
            val dtCount = lesson.questions.count { it is com.learnsy.admin.data.Question.FillBlank }
            if (lesson.password.isNotBlank() || tfCount > 0 || tnCount > 0 || dtCount > 0) {
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (lesson.password.isNotBlank()) MiniBadge("Mật khẩu", Color(0xFF8B93F0), Color(0xFFEEF2FF))
                    if (tfCount > 0) MiniBadge("$tfCount ĐS", Color(0xFFC4A0F0), Color(0xFFF3ECFC))
                    if (tnCount > 0) MiniBadge("$tnCount TN", Color(0xFF6EC9A0), Color(0xFFEAFAF3))
                    if (dtCount > 0) MiniBadge("$dtCount ĐT", Color(0xFFF0A870), Color(0xFFFFF3E8))
                }
            }
        }
        // Actions — gộp Sao chép + Xoá vào 1 menu ⋮ (khớp app.jsx, thay vì 2 nút rời)
        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(999.dp))
                    .background(if (menuOpen) colors.lavL else colors.bg)
            ) {
                Icon(Icons.Default.MoreVert, "Tuỳ chọn", tint = if (menuOpen) colors.lav else colors.text3, modifier = Modifier.size(15.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Sao chép", fontWeight = FontWeight.Bold, color = colors.text2) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = colors.lav, modifier = Modifier.size(15.dp)) },
                    onClick = { menuOpen = false; onDuplicate() }
                )
                DropdownMenuItem(
                    text = { Text("Xoá", fontWeight = FontWeight.Black, color = Color(0xFFEF4444)) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp)) },
                    onClick = { menuOpen = false; onDelete() }
                )
            }
        }
        // Mũi tên điều hướng — nền tròn nhạt, khớp app.jsx
        Box(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(999.dp)).background(colors.lavPale),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.lav2, modifier = Modifier.size(18.dp))
        }
    }
}
