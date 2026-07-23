package com.learnsy.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learnsy.admin.data.ListeningStatement
import com.learnsy.admin.data.TfnmAnswer
import com.learnsy.admin.data.countBlanks
import com.learnsy.admin.ui.ListeningFormUiState
import com.learnsy.admin.ui.ListeningFormViewModel
import com.learnsy.admin.ui.ListeningListUiState
import com.learnsy.admin.ui.ToastCenter
import com.learnsy.admin.ui.theme.LearnsyColors

// Tương đương phần form ('tab==form') trong ListeningManager (listening-panel.jsx).
// TTS (text-to-speech) không port — Android cần TextToSpeech engine riêng, không có
// tương đương trực tiếp trong phạm vi các file đã gửi.
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ListeningFormContent(
    colors: LearnsyColors,
    formVm: ListeningFormViewModel,
    formState: ListeningFormUiState,
    listState: ListeningListUiState,
    onCancel: () -> Unit,
    onSaved: (isNew: Boolean) -> Unit,
    onMismatch: (blanks: Int, answers: Int) -> Unit
) {
    var wbInput by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }
    val blanks = countBlanks(formState.text)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.lavPale)
            .border(1.5.dp, colors.border2, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (formState.editingId != null) "Sửa câu Listening" else "Thêm câu Listening mới",
                fontSize = 13.sp, fontWeight = FontWeight.Black, color = colors.lav, modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onCancel) { Text("Quay lại", fontSize = 12.sp) }
        }

        Column {
            Row {
                Text("Đoạn văn để đọc (dùng ___ cho chỗ trống)", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = colors.text3)
                if (blanks > 0) Text(" · $blanks chỗ trống", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669))
            }
            Spacer(Modifier.height(5.dp))
            OutlinedTextField(
                value = formState.text, onValueChange = formVm::setText,
                placeholder = { Text("VD: Trang An is famous ___ its beautiful landscape.") },
                minLines = 5, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(7.dp))
            TextButton(onClick = {
                formVm.syncBlanksFromText { n ->
                    if (n == 0) ToastCenter.show("! Không tìm thấy ___ trong văn bản", "⚠️", Color(0xFFF59E0B))
                    else ToastCenter.show("✓ Đồng bộ $n chỗ trống từ văn bản", "✅", Color(0xFF10B981))
                }
            }) {
                Icon(Icons.Default.Sync, null, modifier = Modifier.size(12.dp), tint = Color(0xFF059669))
                Spacer(Modifier.width(5.dp))
                Text("Đồng bộ $blanks chỗ trống → ${formState.answers.size} đáp án", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Black)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(colors.surface).border(1.5.dp, colors.border2, RoundedCornerShape(12.dp)).padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Word Box — từ cho học sinh chọn", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF4338CA), modifier = Modifier.weight(1f))
                if (formState.wordBox.size > 1) {
                    IconButton(onClick = { formVm.setShuffleWordBox(!formState.shuffleWordBox) }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Shuffle, "Tự tráo Word Box", tint = if (formState.shuffleWordBox) Color(0xFFDC2626) else Color(0xFF4338CA), modifier = Modifier.size(13.dp))
                    }
                }
                IconButton(onClick = {
                    formVm.suggestWordBoxFromAnswers { added ->
                        when {
                            added < 0 -> ToastCenter.show("! Chưa có đáp án nào", "⚠️", Color(0xFFF59E0B))
                            added == 0 -> ToastCenter.show("! Tất cả đáp án đã có trong Word Box", "⚠️", Color(0xFFF59E0B))
                            else -> ToastCenter.show("✓ Đã thêm $added từ vào Word Box", "✅", Color(0xFF10B981))
                        }
                    }
                }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.AutoAwesome, "Gợi ý từ đáp án", tint = Color(0xFF4338CA), modifier = Modifier.size(13.dp))
                }
            }
            Spacer(Modifier.height(7.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                formState.wordBox.forEachIndexed { i, w ->
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF4338CA).copy(alpha = 0.12f)).padding(start = 10.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(w, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4338CA))
                        IconButton(onClick = { formVm.removeWord(i) }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Close, "Xoá từ", tint = Color(0xFF4338CA), modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = wbInput, onValueChange = { wbInput = it },
                    placeholder = { Text("Nhập từ rồi Enter...") }, singleLine = true, modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    formVm.addWord(wbInput) { ToastCenter.show("! Từ này đã có trong Word Box", "⚠️", Color(0xFFF59E0B)) }
                    wbInput = ""
                }) { Text("+ Thêm") }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF10B981).copy(alpha = 0.05f)).border(1.5.dp, Color(0xFF10B981).copy(alpha = 0.25f), RoundedCornerShape(12.dp)).padding(12.dp)
        ) {
            Text("Đáp án đúng theo thứ tự (1),(2),(3)...", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF059669))
            Spacer(Modifier.height(8.dp))
            formState.answers.forEachIndexed { i, a ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    Text("(${i + 1})", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669))
                    OutlinedTextField(
                        value = a, onValueChange = { formVm.updateAnswer(i, it) },
                        placeholder = { Text("Đáp án ${i + 1}") }, singleLine = true, modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { formVm.removeAnswer(i) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Remove, "Xoá đáp án", tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                    }
                }
            }
            TextButton(onClick = { formVm.addAnswer() }) { Text("+ Thêm chỗ trống", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Black) }
        }

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFDC2626).copy(alpha = 0.04f)).border(1.5.dp, Color(0xFFDC2626).copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("True / False / Not Mentioned (tuỳ chọn)", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDC2626), modifier = Modifier.weight(1f))
                TextButton(onClick = { formVm.setShuffleStatements(!formState.shuffleStatements) }) {
                    Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(11.dp), tint = if (formState.shuffleStatements) Color.White else colors.text3)
                    Spacer(Modifier.width(3.dp))
                    Text("Tráo thứ tự", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (formState.shuffleStatements) Color.White else colors.text3)
                }
            }
            if (formState.shuffleStatements) {
                Text("Đang bật: mỗi học sinh sẽ thấy các nhận định theo thứ tự ngẫu nhiên khác nhau.", fontSize = 10.sp, color = Color(0xFFB45309), modifier = Modifier.padding(bottom = 6.dp))
            }
            formState.statements.forEachIndexed { i, s ->
                StatementRow(
                    index = i, statement = s, colors = colors,
                    canMoveUp = i > 0, canMoveDown = i < formState.statements.size - 1,
                    onTextChange = { formVm.updateStatementText(i, it) },
                    onAnswerChange = { formVm.updateStatementAnswer(i, it) },
                    onMoveUp = { formVm.moveStatement(i, -1) },
                    onMoveDown = { formVm.moveStatement(i, 1) },
                    onRemove = { formVm.removeStatement(i) }
                )
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = { formVm.addStatement() }) { Text("+ Thêm nhận định", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Black) }
        }

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF7C3AED).copy(alpha = 0.04f)).border(1.5.dp, Color(0xFF7C3AED).copy(alpha = 0.25f), RoundedCornerShape(12.dp)).padding(12.dp)
        ) {
            Text("Nhãn (Tags) (tuỳ chọn)", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7C3AED))
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                formState.tags.forEach { t ->
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF7C3AED).copy(alpha = 0.12f)).padding(start = 10.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(t, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                        IconButton(onClick = { formVm.removeTag(t) }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, "Xoá nhãn", tint = Color(0xFF7C3AED), modifier = Modifier.size(9.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = tagInput, onValueChange = { tagInput = it },
                    placeholder = { Text("VD: Unit 5, Beginner, ...") }, singleLine = true, modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { formVm.addTag(tagInput); tagInput = "" }) { Text("+ Thêm") }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Huỷ") }
            Button(
                onClick = {
                    formVm.requestSave(
                        allItems = listState.items,
                        onDupText = { ToastCenter.show("Đã có câu Listening khác với nội dung giống y hệt!", "❌", Color(0xFFEF4444)) },
                        onEmptyText = { ToastCenter.show("Nhập đoạn văn để đọc trước!", "⚠️", Color(0xFFF59E0B)) },
                        onMismatchConfirmNeeded = { b, a -> onMismatch(b, a) },
                        onSaved = { _, isNew -> onSaved(isNew) },
                        onError = { msg -> ToastCenter.show("Lưu thất bại: $msg", "❌", Color(0xFFEF4444)) }
                    )
                },
                enabled = !formState.saving,
                modifier = Modifier.weight(2f),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.lav)
            ) {
                Text(if (formState.saving) "Đang lưu..." else if (formState.editingId != null) "Lưu thay đổi" else "Thêm câu", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun StatementRow(
    index: Int,
    statement: ListeningStatement,
    colors: LearnsyColors,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onTextChange: (String) -> Unit,
    onAnswerChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp)).background(colors.surface)
            .border(1.dp, Color(0xFFDC2626).copy(alpha = 0.15f), RoundedCornerShape(10.dp)).padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${index + 1}.", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
            com.learnsy.admin.ui.components.UnderlineOnlyInp(
                valueHtml = statement.statement, onChange = onTextChange,
                placeholder = "Nhận định ${index + 1}", colors = colors, modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, "Lên", tint = if (canMoveUp) colors.text3 else colors.text4, modifier = Modifier.size(15.dp))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, "Xuống", tint = if (canMoveDown) colors.text3 else colors.text4, modifier = Modifier.size(15.dp))
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.Remove, "Xoá", tint = Color(0xFFDC2626), modifier = Modifier.size(13.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TfnmAnswer.values().forEach { ans ->
                val selected = statement.answer == ans.stored
                val color = when (ans) {
                    TfnmAnswer.TRUE -> Color(0xFF16A34A)
                    TfnmAnswer.FALSE -> Color(0xFFDC2626)
                    TfnmAnswer.NOT_MENTIONED -> Color(0xFF6366F1)
                }
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(if (selected) color else color.copy(alpha = 0.08f))
                        .border(1.5.dp, color.copy(alpha = if (selected) 1f else 0.35f), RoundedCornerShape(8.dp))
                        .clickable { onAnswerChange(ans.stored) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(ans.label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (selected) Color.White else color)
                }
            }
        }
    }
}
