package com.learnsy.admin.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.learnsy.admin.data.ListeningItem
import com.learnsy.admin.ui.*
import com.learnsy.admin.ui.theme.LearnsyColors

enum class ListeningTab { LIST, FORM, STATS }

// Tương đương ListeningManager trong listening-panel.jsx (không gồm ListeningPreview —
// đã loại bỏ theo yêu cầu bỏ tính năng Thử đề).
@Composable
fun ListeningManagerScreen(
    colors: LearnsyColors,
    refreshKey: Any = Unit,
    listVm: ListeningListViewModel = viewModel(),
    formVm: ListeningFormViewModel = viewModel()
) {
    var tab by remember { mutableStateOf(ListeningTab.LIST) }
    var showImport by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }
    var mismatchDialog by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var deleteTarget by remember { mutableStateOf<ListeningItem?>(null) }
    var bulkDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Tương đương exportJSON() trong listening-panel.jsx — tải file .json chứa toàn bộ items
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val json = kotlinx.serialization.json.Json { prettyPrint = true }
            val data = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(ListeningItem.serializer()), listVm.uiState.value.items)
            context.contentResolver.openOutputStream(uri)?.use { it.write(data.toByteArray()) }
            ToastCenter.show("✓ Đã xuất file JSON", "✅", Color(0xFF059669))
        } catch (e: Exception) {
            ToastCenter.show("Xuất file thất bại: ${e.message}", "❌", Color(0xFFEF4444))
        }
    }

    val listState by listVm.uiState.collectAsState()
    val formState by formVm.uiState.collectAsState()

    LaunchedEffect(refreshKey) { listVm.load() }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Xoá câu Listening?") },
            text = { Text(com.learnsy.admin.data.stripHTML(item.text).take(60)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    listVm.deleteItem(item.id) { _, msg -> ToastCenter.show(msg, "🗑️", Color(0xFFEF4444)) }
                }) { Text("Xoá", color = Color(0xFFEF4444)) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Huỷ") } }
        )
    }
    if (bulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { bulkDeleteConfirm = false },
            title = { Text("Xoá ${listState.selected.size} câu Listening?") },
            text = { Text("Không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = {
                    bulkDeleteConfirm = false
                    listVm.bulkDelete { _, msg -> ToastCenter.show(msg, "🗑️", Color(0xFFEF4444)) }
                }) { Text("Xoá tất cả", color = Color(0xFFEF4444)) }
            },
            dismissButton = { TextButton(onClick = { bulkDeleteConfirm = false }) { Text("Huỷ") } }
        )
    }
    mismatchDialog?.let { pair ->
        val blanks = pair.first; val ans = pair.second
        AlertDialog(
            onDismissRequest = { mismatchDialog = null },
            title = { Text("Số chỗ trống không khớp") },
            text = { Text("Văn bản có $blanks chỗ trống (___) nhưng bạn nhập $ans đáp án. Vẫn tiếp tục lưu?") },
            confirmButton = {
                TextButton(onClick = {
                    mismatchDialog = null
                    formVm.confirmSaveAnyway(
                        listState.items,
                        onSaved = { item, isNew ->
                            if (isNew) listVm.load() else listVm.replaceItem(item)
                            tab = ListeningTab.LIST
                            ToastCenter.show(if (isNew) "+ Đã thêm câu Listening!" else "+ Đã cập nhật câu Listening!", "✅", Color(0xFF10B981))
                        },
                        onError = { msg -> ToastCenter.show("Lưu thất bại: $msg", "❌", Color(0xFFEF4444)) }
                    )
                }) { Text("Lưu anyway", color = Color(0xFFF59E0B)) }
            },
            dismissButton = { TextButton(onClick = { mismatchDialog = null }) { Text("Huỷ") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(colors.lavL)
                    .border(1.5.dp, colors.border2, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Headphones, null, tint = colors.lav, modifier = Modifier.size(17.dp)) }
            Column(modifier = Modifier.weight(1f)) {
                Text("Listening", fontSize = 18.sp, fontWeight = FontWeight.Black, color = colors.text)
                Text("${listState.items.size} câu · Đoạn văn + Điền từ + True/False/NM", fontSize = 12.sp, color = colors.text3)
            }
            IconButton(onClick = { tab = if (tab == ListeningTab.STATS) ListeningTab.LIST else ListeningTab.STATS }) {
                Icon(Icons.Default.BarChart, "Thống kê", tint = colors.lav)
            }
            IconButton(onClick = {
                val filename = "listening_items_${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}.json"
                exportLauncher.launch(filename)
            }) {
                Icon(Icons.Default.Download, "Xuất JSON", tint = Color(0xFF059669))
            }
            IconButton(onClick = { showImport = !showImport }) {
                Icon(Icons.Default.Upload, "Import JSON", tint = Color(0xFFD97706))
            }
            Button(
                onClick = { formVm.resetForm(); tab = if (tab == ListeningTab.FORM) ListeningTab.LIST else ListeningTab.FORM },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.lav),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Thêm", fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
        HorizontalDivider(color = colors.border)

        if (listState.loadError) {
            Text(
                "Không tải được dữ liệu Listening — kiểm tra bảng listening_items đã tạo trên Supabase chưa.",
                color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.background(Color(0xFFDC2626).copy(alpha = 0.08f), RoundedCornerShape(12.dp)).padding(12.dp)
            )
        }

        if (tab == ListeningTab.STATS) ListeningStatsPanel(listState.items, colors)

        if (showImport) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFD97706).copy(alpha = 0.06f))
                    .border(1.5.dp, Color(0xFFD97706).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text("Import JSON", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFD97706))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = importJson, onValueChange = { importJson = it },
                    placeholder = { Text("Dán JSON mảng [...] vào đây") },
                    minLines = 4, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            try {
                                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                                val parsed = json.decodeFromString<List<ListeningItem>>(importJson)
                                listVm.importItems(parsed) { ok, msg ->
                                    ToastCenter.show(msg, if (ok) "✅" else "❌", if (ok) Color(0xFF10B981) else Color(0xFFEF4444))
                                    if (ok) { importJson = ""; showImport = false }
                                }
                            } catch (e: Exception) {
                                ToastCenter.show("JSON không hợp lệ: ${e.message}", "❌", Color(0xFFEF4444))
                            }
                        },
                        enabled = importJson.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) { Text("Import", fontWeight = FontWeight.Black) }
                    TextButton(onClick = { showImport = false; importJson = "" }) { Text("Huỷ") }
                }
            }
        }

        if (listState.loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.lav)
            }
        }

        if (tab == ListeningTab.LIST && !listState.loading) {
            ListeningListContent(
                colors = colors, listVm = listVm, listState = listState, formVm = formVm,
                onOpenForm = { tab = ListeningTab.FORM },
                onRequestDelete = { deleteTarget = it },
                onRequestBulkDelete = { bulkDeleteConfirm = true }
            )
        }

        if (tab == ListeningTab.FORM) {
            ListeningFormContent(
                colors = colors, formVm = formVm, formState = formState, listState = listState,
                onCancel = { formVm.resetForm(); tab = ListeningTab.LIST },
                onSaved = { isNew ->
                    listVm.load()
                    tab = ListeningTab.LIST
                    ToastCenter.show(if (isNew) "+ Đã thêm câu Listening!" else "+ Đã cập nhật câu Listening!", "✅", Color(0xFF10B981))
                },
                onMismatch = { blanks, ans -> mismatchDialog = blanks to ans }
            )
        }
    }
}

@Composable
private fun ListeningListContent(
    colors: LearnsyColors,
    listVm: ListeningListViewModel,
    listState: ListeningListUiState,
    formVm: ListeningFormViewModel,
    onOpenForm: () -> Unit,
    onRequestDelete: (ListeningItem) -> Unit,
    onRequestBulkDelete: () -> Unit
) {
    val displayItems = listVm.displayItems()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (listState.items.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = listState.searchQuery, onValueChange = listVm::setSearch,
                    placeholder = { Text("Tìm câu...") }, singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { listVm.toggleBulkMode() }) {
                    Text(if (listState.bulkMode) "Thoát" else "Chọn nhiều", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
            // Tương đương 2 <select> filter/sort trong listening-panel.jsx
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ListeningFilterDropdown(colors = colors, filter = listState.filter, onSelect = listVm::setFilter, modifier = Modifier.weight(1f))
                ListeningSortDropdown(colors = colors, sortBy = listState.sortBy, onSelect = listVm::setSortBy, modifier = Modifier.weight(1f))
            }
        }

        if (listState.bulkMode) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFDC2626).copy(alpha = 0.06f))
                    .border(1.5.dp, Color(0xFFDC2626).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(10.dp, 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (listState.selected.isNotEmpty()) "Đã chọn ${listState.selected.size} câu" else "Chọn câu để xoá hàng loạt",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), modifier = Modifier.weight(1f)
                )
                if (listState.selected.isNotEmpty()) {
                    TextButton(onClick = onRequestBulkDelete) { Text("Xoá ${listState.selected.size}", color = Color(0xFFDC2626), fontWeight = FontWeight.Black) }
                } else {
                    TextButton(onClick = { listVm.selectAll() }) { Text("Chọn tất cả") }
                }
            }
        }

        if (listState.items.isEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Chưa có câu Listening nào.", fontSize = 12.sp, color = colors.text3, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { formVm.resetForm(); onOpenForm() }) { Text("+ Thêm câu đầu tiên") }
            }
        } else if (displayItems.isEmpty()) {
            Text("Không tìm thấy câu nào khớp với bộ lọc.", fontSize = 12.sp, color = colors.text3, modifier = Modifier.padding(vertical = 16.dp))
        }

        displayItems.forEachIndexed { idx, item ->
            ListeningItemCard(
                item = item, colors = colors,
                bulkMode = listState.bulkMode, selected = item.id in listState.selected,
                onToggleSelect = { listVm.toggleSelect(item.id) },
                onEdit = { formVm.openForEdit(item); onOpenForm() },
                onDuplicate = { listVm.duplicateItem(item) { _, msg -> ToastCenter.show(msg, "✓", Color(0xFF059669)) } },
                onDelete = { onRequestDelete(item) },
                // Nút lên/xuống thay cho kéo-thả (phù hợp thao tác chạm hơn trên mobile) —
                // chỉ có ý nghĩa khi đang sắp theo "Thứ tự" và không ở chế độ chọn nhiều.
                showReorder = listState.sortBy == ListeningSort.ORDER && !listState.bulkMode,
                canMoveUp = idx > 0,
                canMoveDown = idx < displayItems.size - 1,
                onMoveUp = { if (idx > 0) listVm.reorder(item.id, displayItems[idx - 1].id) },
                onMoveDown = { if (idx < displayItems.size - 1) listVm.reorder(item.id, displayItems[idx + 1].id) }
            )
        }
    }
}

@Composable
private fun ListeningItemCard(
    item: ListeningItem,
    colors: LearnsyColors,
    bulkMode: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    showReorder: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.lavPale else colors.bg2)
            .border(1.5.dp, if (selected) colors.lav else colors.border, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (bulkMode) {
            Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
        }
        if (showReorder) {
            Column {
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, "Lên", tint = if (canMoveUp) colors.text3 else colors.border, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, "Xuống", tint = if (canMoveDown) colors.text3 else colors.border, modifier = Modifier.size(16.dp))
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                com.learnsy.admin.data.stripHTML(item.text).take(90).ifBlank { "(Chưa có nội dung)" },
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text, maxLines = 2
            )
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (item.answers.isNotEmpty()) Tag("${item.answers.size} chỗ trống", Color(0xFF059669), colors)
                if (item.wordBox.isNotEmpty()) Tag("${item.wordBox.size} từ WB", Color(0xFF4338CA), colors)
                if (item.statements.isNotEmpty()) Tag("${item.statements.size} T/F/NM", Color(0xFFDC2626), colors)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            IconButton(onClick = onDuplicate, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.ContentCopy, "Nhân đôi", tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Edit, "Sửa", tint = colors.lav, modifier = Modifier.size(14.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Delete, "Xoá", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun Tag(text: String, color: Color, colors: LearnsyColors) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 9.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
private fun ListeningFilterDropdown(colors: LearnsyColors, filter: ListeningFilter, onSelect: (ListeningFilter) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        ListeningFilter.ALL to "Tất cả", ListeningFilter.HAS_WORD_BOX to "Có Word Box",
        ListeningFilter.HAS_TFNM to "Có T/F/NM", ListeningFilter.NO_WORD_BOX to "Không có WB"
    )
    val label = options.find { it.first == filter }?.second ?: "Tất cả"
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; onSelect(value) })
            }
        }
    }
}

@Composable
private fun ListeningSortDropdown(colors: LearnsyColors, sortBy: ListeningSort, onSelect: (ListeningSort) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        ListeningSort.ORDER to "Thứ tự", ListeningSort.CREATED to "Mới nhất", ListeningSort.BLANKS to "Nhiều chỗ trống"
    )
    val label = options.find { it.first == sortBy }?.second ?: "Thứ tự"
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; onSelect(value) })
            }
        }
    }
}
