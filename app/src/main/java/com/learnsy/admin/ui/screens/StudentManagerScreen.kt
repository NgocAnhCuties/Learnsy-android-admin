package com.learnsy.admin.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.learnsy.admin.data.AvatarRepository
import com.learnsy.admin.data.Student
import com.learnsy.admin.ui.*
import com.learnsy.admin.ui.theme.LearnsyColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Tương đương StudentManager trong student-manager.jsx.
@Composable
fun StudentManagerScreen(
    colors: LearnsyColors,
    dark: Boolean,
    refreshKey: Any = Unit,
    vm: StudentListViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    val (avatarState, requestAvatarPick) = rememberAvatarUiState()
    val context = LocalContext.current
    val avatarRepo = remember { AvatarRepository(context) }
    val scope = rememberCoroutineScope()

    var statusOpen by remember { mutableStateOf(false) }
    var modal by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Student?>(null) }
    var plainPassView by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<Student?>(null) }
    var confirmBulkDelete by remember { mutableStateOf(false) }
    var nowUTC7 by remember { mutableStateOf("") }

    // Tương đương exportCSV() trong student-manager.jsx — xuất danh sách đang lọc ra file .csv
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val header = "Username,Tên hiển thị,Lớp,Trạng thái,Ngày tạo"
            val rows = vm.filteredStudents().joinToString("\n") { s ->
                val name = "\"" + (s.displayName ?: "").replace("\"", "\"\"") + "\""
                val date = runCatching {
                    java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("vi", "VN"))
                        .format(java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(s.createdAt.take(19)))
                }.getOrDefault("")
                listOf(s.username, name, s.className, if (s.isActive) "Hoạt động" else "Khoá", date).joinToString(",")
            }
            val csv = "\uFEFF" + header + "\n" + rows // BOM để Excel đọc đúng tiếng Việt
            context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
            ToastCenter.show("Đã xuất file CSV", "✅", Color(0xFF10B981))
        } catch (e: Exception) {
            ToastCenter.show("Xuất file thất bại: ${e.message}", "❌", Color(0xFFEF4444))
        }
    }

    LaunchedEffect(refreshKey) {
        vm.load()
        vm.ping()
    }
    LaunchedEffect(Unit) {
        while (true) {
            val utc7 = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
            nowUTC7 = String.format("%02d:%02d:%02d", utc7.hour, utc7.minute, utc7.second)
            delay(1000)
        }
    }
    LaunchedEffect(Unit) {
        while (true) { delay(3_600_000); vm.ping() }
    }

    val filtered = vm.filteredStudents()
    val active = state.students.count { it.isActive }
    val locked = state.students.size - active

    Column(modifier = Modifier.fillMaxSize().padding(12.dp, 14.dp, 12.dp, 100.dp)) {

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            listOf(
                Triple("Tổng", state.students.size, Color(0xFFA855F7)),
                Triple("Hoạt động", active, Color(0xFF10B981)),
                Triple("Khoá", locked, Color(0xFFEF4444)),
                Triple("Lớp", vm.classes().size, Color(0xFFF472B6))
            ).forEach { (label, value, color) ->
                Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(22.dp)).padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("$value", fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
                    Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = colors.text3)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 20.dp)) {
            Text("Tài khoản học sinh", fontSize = 15.sp, fontWeight = FontWeight.Black, color = colors.text, modifier = Modifier.weight(1f))

            IconButton(
                onClick = { statusOpen = !statusOpen },
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                    .background(
                        if (statusOpen) (if (state.srvStatus == "online") Color(0xFF10B981) else Color(0xFFEF4444))
                        else (if (state.srvStatus == "online") Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f))
                    )
            ) {
                Icon(Icons.Default.Dns, "Trạng thái hệ thống", tint = if (statusOpen) Color.White else if (state.srvStatus == "online") Color(0xFF059669) else Color(0xFFEF4444), modifier = Modifier.size(14.dp))
            }
            IconButton(onClick = {
                val filename = "students_${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}.csv"
                exportLauncher.launch(filename)
            }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Download, "Xuất CSV", tint = Color(0xFFA855F7), modifier = Modifier.size(13.dp))
            }
            IconButton(onClick = { vm.toggleBulkMode() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Checklist, "Chọn nhiều", tint = if (state.bulkMode) Color.White else Color(0xFFA855F7), modifier = Modifier.size(13.dp))
            }
            Button(
                onClick = { modal = "add" },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.rose),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("Thêm", fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }

        if (statusOpen) {
            StatusPanel(state = state, saving = state.saving, nowUTC7 = nowUTC7, colors = colors, dark = dark)
            Spacer(Modifier.height(14.dp))
        }

        if (state.bulkMode && state.bulkSelected.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFA855F7).copy(alpha = 0.08f)).border(1.5.dp, Color(0xFFA855F7).copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .padding(10.dp, 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Đã chọn ${state.bulkSelected.size}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFA855F7), modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.bulkToggleActive(true) { } }) { Text("Mở khoá", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Black) }
                TextButton(onClick = { vm.bulkToggleActive(false) { } }) { Text("Khoá", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Black) }
                TextButton(onClick = { confirmBulkDelete = true }) { Text("Xoá ${state.bulkSelected.size}", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Black) }
            }
            Spacer(Modifier.height(10.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            OutlinedTextField(
                value = state.search, onValueChange = vm::setSearch,
                placeholder = { Text("Tìm học sinh...") }, singleLine = true, modifier = Modifier.weight(1f)
            )
        }
        // Tương đương 3 <select> filterClass/filterStatus/sortBy trong student-manager.jsx
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            StudentClassDropdown(colors = colors, classes = vm.classes(), selected = state.filterClass, onSelect = vm::setFilterClass, modifier = Modifier.weight(1f))
            StudentStatusDropdown(colors = colors, status = state.filterStatus, onSelect = vm::setFilterStatus, modifier = Modifier.weight(1f))
            StudentSortDropdown(colors = colors, sortBy = state.sortBy, onSelect = vm::setSortBy, modifier = Modifier.weight(1f))
        }

        if (state.loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.rose)
            }
        } else if (filtered.isEmpty()) {
            Text("Không có học sinh nào.", fontSize = 12.sp, color = colors.text3, modifier = Modifier.padding(vertical = 20.dp))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filtered.forEach { student ->
                    StudentCard(
                        student = student, colors = colors,
                        bulkMode = state.bulkMode, selected = student.id in state.bulkSelected,
                        avatarUrl = avatarState.urls[student.id],
                        uploading = avatarState.uploadingId == student.id,
                        onToggleSelect = { vm.toggleBulkSelect(student.id) },
                        onToggleActive = { vm.toggleActive(student) },
                        onEdit = { selected = student; modal = "edit" },
                        onDelete = { confirmDelete = student }
                    )
                }
            }
        }
    }

    if (modal == "add") {
        AddStudentModal(
            colors = colors, dark = dark, classes = vm.classes(), saving = state.saving,
            onDismiss = { modal = null },
            onSubmit = { username, name, className, password ->
                vm.addStudent(username, name, className, password,
                    onSuccess = { student, plain ->
                        selected = student; plainPassView = plain; modal = "view"
                    },
                    onError = { msg -> ToastCenter.show(msg, "❌", Color(0xFFEF4444)) }
                )
            }
        )
    }

    if (modal == "edit" && selected != null) {
        EditStudentModal(
            student = selected!!, colors = colors, dark = dark, saving = state.saving, classes = vm.classes(),
            avatarUrl = avatarState.urls[selected!!.id], uploading = avatarState.uploadingId == selected!!.id,
            onAvatarClick = { requestAvatarPick(selected!!.id) },
            onDismiss = { modal = null; selected = null },
            onSubmit = { name, className ->
                vm.editStudent(selected!!.id, name, className) { ok, msg ->
                    ToastCenter.show(msg, if (ok) "✅" else "❌", if (ok) Color(0xFF10B981) else Color(0xFFEF4444))
                    if (ok) { modal = null; selected = null }
                }
            }
        )
    }

    if (modal == "view" && selected != null) {
        ViewCreatedModal(
            student = selected!!, plainPassword = plainPassView, colors = colors, dark = dark,
            onDismiss = { modal = null; selected = null; plainPassView = null }
        )
    }

    confirmDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Xoá tài khoản?") },
            text = { Text("@${s.username} · ${s.displayName ?: ""}\nHành động này không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    scope.launch { runCatching { avatarRepo.delete(s.id) } }
                    vm.deleteStudent(s.id) { ok, msg -> ToastCenter.show(msg, if (ok) "🗑️" else "❌", Color(0xFFEF4444)) }
                }) { Text("Xoá", color = Color(0xFFEF4444)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Huỷ") } }
        )
    }

    if (confirmBulkDelete) {
        AlertDialog(
            onDismissRequest = { confirmBulkDelete = false },
            title = { Text("Xoá ${state.bulkSelected.size} tài khoản?") },
            text = { Text("Toàn bộ lịch sử bài làm và ảnh đại diện sẽ bị xóa vĩnh viễn.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmBulkDelete = false
                    val ids = state.bulkSelected.toList()
                    scope.launch { ids.forEach { id -> runCatching { avatarRepo.delete(id) } } }
                    vm.bulkDelete { ok, fail -> ToastCenter.show("Đã xóa $ok tài khoản${if (fail > 0) " ($fail lỗi)" else ""}!", "🗑️", Color(0xFFEF4444)) }
                }) { Text("Xoá tất cả", color = Color(0xFFEF4444)) }
            },
            dismissButton = { TextButton(onClick = { confirmBulkDelete = false }) { Text("Huỷ") } }
        )
    }
}

@Composable
private fun StudentClassDropdown(colors: LearnsyColors, classes: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val label = selected.ifBlank { "Tất cả lớp" }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(15.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Tất cả lớp") }, onClick = { expanded = false; onSelect("") })
            classes.forEach { c ->
                DropdownMenuItem(text = { Text(c) }, onClick = { expanded = false; onSelect(c) })
            }
        }
    }
}

@Composable
private fun StudentStatusDropdown(colors: LearnsyColors, status: StatusFilter, onSelect: (StatusFilter) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(StatusFilter.ALL to "Tất cả", StatusFilter.ACTIVE to "Hoạt động", StatusFilter.LOCKED to "Đã khoá")
    val label = options.find { it.first == status }?.second ?: "Tất cả"
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(15.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; onSelect(value) })
            }
        }
    }
}

@Composable
private fun StudentSortDropdown(colors: LearnsyColors, sortBy: StudentSortBy, onSelect: (StudentSortBy) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(StudentSortBy.NEWEST to "Mới nhất", StudentSortBy.NAME to "Tên A-Z", StudentSortBy.CLASS to "Theo lớp")
    val label = options.find { it.first == sortBy }?.second ?: "Mới nhất"
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(15.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; onSelect(value) })
            }
        }
    }
}

@Composable
private fun StatusPanel(state: StudentListUiState, saving: Boolean, nowUTC7: String, colors: LearnsyColors, dark: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface)
            .border(1.5.dp, colors.border, RoundedCornerShape(18.dp)).padding(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusMini(if (state.srvStatus == "online") "Online" else if (state.srvStatus == "offline") "Offline" else "...", if (state.srvStatus == "online") Color(0xFF059669) else if (state.srvStatus == "offline") Color(0xFFEF4444) else colors.text3, Modifier.weight(1f))
            StatusMini(state.pingMs?.let { "${it}ms" } ?: "—", colors.text, Modifier.weight(1f))
            StatusMini(if (saving) "Đang lưu" else "Đã lưu", if (saving) Color(0xFFA855F7) else Color(0xFF059669), Modifier.weight(1f))
            StatusMini(nowUTC7.ifBlank { "--:--:--" }, colors.text, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.bg).padding(10.dp, 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            InfoRow("Backend", "Supabase (PostgREST)", colors)
            InfoRow("Bảng đang theo dõi", "students", colors)
            InfoRow("Số học sinh tải được", "${state.students.size}", colors)
            InfoRow("Tự ping mỗi", "1 giờ", colors)
        }
        Spacer(Modifier.height(12.dp))
        Text("HOẠT ĐỘNG GẦN ĐÂY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = colors.text3, modifier = Modifier.padding(bottom = 6.dp))
        if (state.activityLog.isEmpty()) {
            Text("Chưa có hoạt động nào", fontSize = 11.sp, color = colors.text3, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.activityLog.forEach { item ->
                    val color = when (item.type) {
                        "error" -> Color(0xFFEF4444); "insert" -> Color(0xFFA855F7); "delete" -> Color(0xFFEF4444)
                        "update" -> Color(0xFFF59E0B); "fn" -> Color(0xFFF472B6); "ping" -> colors.text3; else -> Color(0xFF059669)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
                        Text(item.msg, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.text, modifier = Modifier.weight(1f))
                        val time = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(item.ts))
                        Text(time, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.text3)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMini(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.1f)).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
private fun InfoRow(label: String, value: String, colors: LearnsyColors) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.text3)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Black, color = colors.text)
    }
}

@Composable
private fun StudentCard(
    student: Student,
    colors: LearnsyColors,
    bulkMode: Boolean,
    selected: Boolean,
    avatarUrl: String?,
    uploading: Boolean,
    onToggleSelect: () -> Unit,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (selected) colors.rosePale else colors.surface)
            .border(1.dp, if (selected) colors.rose else colors.border, RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        if (bulkMode) Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })

        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(colors.rose, colors.lav))),
            contentAlignment = Alignment.Center
        ) {
            if (uploading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
            } else if (avatarUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(avatarUrl),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text((student.displayName ?: student.username).take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(student.displayName ?: student.username, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = colors.text, maxLines = 1)
            Row {
                Text("@${student.username}", fontSize = 11.sp, color = colors.text3)
                if (student.className.isNotBlank()) Text(" · ${student.className}", fontSize = 11.sp, color = colors.text3)
            }
        }

        Switch(checked = student.isActive, onCheckedChange = { onToggleActive() }, modifier = Modifier.scale(0.8f))
        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Edit, "Sửa", tint = colors.lav, modifier = Modifier.size(13.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Delete, "Xoá", tint = Color(0xFFEF4444), modifier = Modifier.size(13.dp))
        }
    }
}
