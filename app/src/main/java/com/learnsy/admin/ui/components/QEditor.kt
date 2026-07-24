package com.learnsy.admin.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learnsy.admin.data.LETTERS
import com.learnsy.admin.data.Question
import com.learnsy.admin.data.TFItem
import com.learnsy.admin.data.stripHTML
import com.learnsy.admin.ui.components.FlowerIcon
import com.learnsy.admin.ui.components.HeartIcon
import com.learnsy.admin.ui.components.SparkleIcon
import com.learnsy.admin.ui.theme.LearnsyColors

// Tương đương QEditor trong question-editor.jsx.
// Không port autoAI/onAIAnswer — chưa có model AI ở bản native.
@Composable
fun QEditor(
    q: Question,
    qi: Int,
    onQuestionChange: (Question) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
    colors: LearnsyColors,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }

    val accentColor = when (q) {
        is Question.TrueFalse -> colors.lav
        is Question.Multiple -> colors.rose
        is Question.MultiSelect -> colors.mint
        is Question.FillBlank -> colors.peach
    }
    val typeShort = when (q) {
        is Question.TrueFalse -> "ĐS"
        is Question.Multiple -> "TN"
        is Question.MultiSelect -> "CN"
        is Question.FillBlank -> "ĐT"
    }
    val previewText = when (q) {
        is Question.TrueFalse -> stripHTML(q.passage).take(52).ifBlank { "(Chưa nhập đoạn tư liệu...)" }
        is Question.Multiple -> stripHTML(q.question).take(52).ifBlank { "(Chưa nhập câu hỏi...)" }
        is Question.MultiSelect -> stripHTML(q.question).take(52).ifBlank { "(Chưa nhập câu hỏi...)" }
        is Question.FillBlank -> stripHTML(q.question).take(52).ifBlank { "(Chưa nhập câu hỏi...)" }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            // Đổ bóng nhẹ hồng — khớp boxShadow:'0 3px 16px rgba(255,100,150,0.06)' trong JSX
            .shadow(3.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFFFF6496), spotColor = Color(0xFFFF6496))
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(1.5.dp, colors.border, RoundedCornerShape(18.dp))
            // Viền trên dày 3px theo màu loại câu hỏi — khớp borderTop:'3px solid accentColor' trong app.jsx
            // (Modifier.border() vẽ viền đều 4 cạnh nên cần drawBehind riêng cho cạnh trên).
            .drawBehind {
                drawLine(
                    color = accentColor,
                    start = androidx.compose.ui.geometry.Offset(0f, 1.5.dp.toPx() / 2),
                    end = androidx.compose.ui.geometry.Offset(size.width, 1.5.dp.toPx() / 2),
                    strokeWidth = 3.dp.toPx()
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .background(if (open) colors.surface else colors.bg)
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .shadow(2.dp, RoundedCornerShape(9.dp), ambientColor = Color(0xFFA855F7), spotColor = Color(0xFFA855F7))
                    .size(26.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(colors.grad),
                contentAlignment = Alignment.Center
            ) {
                Text("${qi + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
                    .padding(start = 4.dp, end = 8.dp, top = 3.dp, bottom = 3.dp)
            ) {
                // Icon theo loại câu hỏi — khớp getTypes() trong app.jsx:
                // true_false→Flower, multiple→Heart, multi_select→Star, fill_blank→Sparkle
                when (q) {
                    is Question.TrueFalse -> FlowerIcon(size = 15, color = accentColor)
                    is Question.Multiple -> HeartIcon(size = 15, color = accentColor)
                    is Question.MultiSelect -> Icon(Icons.Default.Star, null, tint = accentColor, modifier = Modifier.size(15.dp))
                    is Question.FillBlank -> SparkleIcon(size = 15, color = accentColor)
                }
                Text(typeShort, fontSize = 10.sp, fontWeight = FontWeight.Black, color = accentColor)
            }
            Text(
                previewText,
                modifier = Modifier.weight(1f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text2,
                maxLines = 1
            )
            if (canRemove) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(colors.rosePale)
                        .border(1.5.dp, Color(0xFFFECDD3), CircleShape)
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Xoá câu hỏi", tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                }
            }
            val rotation by animateFloatAsState(if (open) 180f else 0f, animationSpec = tween(200), label = "chevron")
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (open) colors.lavL else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = if (open) "Thu gọn" else "Mở rộng",
                    tint = if (open) colors.lav else colors.text4,
                    modifier = Modifier.size(14.dp).rotate(rotation)
                )
            }
        }

        if (open) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Viền phân cách header/body — khớp borderTop:'1px solid C.border' trong JSX
                    .drawBehind {
                        drawLine(
                            color = colors.border,
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(start = 13.dp, top = 11.dp, end = 13.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (q) {
                    is Question.TrueFalse -> TrueFalseFields(q, onQuestionChange, colors)
                    is Question.Multiple -> MultipleFields(q, onQuestionChange, colors)
                    is Question.MultiSelect -> MultiSelectFields(q, onQuestionChange, colors)
                    is Question.FillBlank -> FillBlankFields(q, onQuestionChange, colors)
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String, colors: LearnsyColors) {
    Text(
        text.uppercase(java.util.Locale("vi")), fontSize = 11.sp, fontWeight = FontWeight.Black,
        color = colors.text2, letterSpacing = 0.8.sp
    )
}

// Nút "Thêm ý" / "Thêm lựa chọn" — khớp JSX: padding '6px 14px', borderRadius 999,
// border 1.5px dashed C.lav2, background C.lavL, gap 5, fontSize 12 weight 800.
@Composable
private fun AddButton(text: String, colors: LearnsyColors, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.lavL)
            .drawBehind {
                val strokeWidthPx = 1.5.dp.toPx()
                val cornerRadiusPx = size.minDimension / 2
                drawRoundRect(
                    color = colors.lav2,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidthPx,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Icon(Icons.Default.Add, null, tint = colors.lav, modifier = Modifier.size(12.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Black, color = colors.lav)
    }
}

@Composable
private fun TrueFalseFields(
    q: Question.TrueFalse,
    onChange: (Question) -> Unit,
    colors: LearnsyColors
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Default.MenuBook, null, tint = colors.lav, modifier = Modifier.size(12.dp))
            FieldLabel("Đoạn tư liệu", colors)
        }
        RichInp(
            valueHtml = q.passage,
            onChange = { onChange(q.copy(passage = it)) },
            placeholder = "Nhập đoạn trích tư liệu lịch sử...",
            colors = colors,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Default.Schedule, null, tint = colors.text3, modifier = Modifier.size(12.dp))
            FieldLabel("Nguồn (tùy chọn)", colors)
        }
        OutlinedTextField(
            value = q.source,
            onValueChange = { onChange(q.copy(source = it)) },
            placeholder = { Text("(NXB, năm, trang...)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.lav2,
                unfocusedBorderColor = colors.border,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface
            )
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Edit, null, tint = colors.rose, modifier = Modifier.size(12.dp))
            FieldLabel("Các ý — bấm ✓ ✗ để đặt đáp án", colors)
        }
        // Danh sách các ý — khớp JSX: div flexDirection:column, gap:7
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            q.items.forEachIndexed { ii, item ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Khớp bản gốc: width 22, height 22, borderRadius 7, marginTop 9, fontSize 11
                    Box(
                        modifier = Modifier
                            .padding(top = 9.dp)
                            .size(22.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(colors.lavL)
                            .border(1.dp, colors.border2, RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(('a' + ii).toString(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = colors.lav)
                    }
                    MiniRichInp(
                        valueHtml = item.text,
                        onChange = { text ->
                            onChange(q.copy(items = q.items.mapIndexed { i, it -> if (i == ii) it.copy(text = text) else it }))
                        },
                        placeholder = "Ý ${('a' + ii)}...",
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                    // Cụm nút ✓ ✗ – khớp số đo bản gốc question-editor.jsx:
                    // nút ✓/✗ là 34×34, radius 9, icon 14dp, border 1.5px, gap 4px, marginTop 4.
                    // Nút xoá là 30×34 (không vuông), radius 9, icon 12dp.
                    // Dùng Box + clickable thay vì IconButton: IconButton ép minimum touch
                    // target 48dp bất kể .size() đặt sau, khiến slot cố định bị tràn ra
                    // ngoài card. Box + clickable tôn trọng đúng kích thước mình khai báo.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .then(
                                    if (item.answer)
                                        Modifier.shadow(2.dp, RoundedCornerShape(9.dp), ambientColor = Color(0xFF10B981), spotColor = Color(0xFF10B981))
                                    else Modifier
                                )
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (item.answer) Color(0xFF10B981) else colors.mintL)
                                .border(1.5.dp, if (item.answer) Color.Transparent else Color(0xFFBBF7D0), RoundedCornerShape(9.dp))
                                .clickable {
                                    onChange(q.copy(items = q.items.mapIndexed { i, it -> if (i == ii) it.copy(answer = true) else it }))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, "Đúng", tint = if (item.answer) Color.White else colors.mint, modifier = Modifier.size(14.dp))
                        }
                        Box(
                            modifier = Modifier
                                .then(
                                    if (!item.answer)
                                        Modifier.shadow(2.dp, RoundedCornerShape(9.dp), ambientColor = Color(0xFFEF4444), spotColor = Color(0xFFEF4444))
                                    else Modifier
                                )
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (!item.answer) Color(0xFFEF4444) else colors.rosePale)
                                .border(1.5.dp, if (!item.answer) Color.Transparent else Color(0xFFFECDD3), RoundedCornerShape(9.dp))
                                .clickable {
                                    onChange(q.copy(items = q.items.mapIndexed { i, it -> if (i == ii) it.copy(answer = false) else it }))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, "Sai", tint = if (!item.answer) Color.White else Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                        }
                        // Slot xoá luôn chiếm chỗ cố định (kể cả khi ẩn nút) để cột
                        // không bị co giãn khi items.size == 2. 30×34 khớp bản gốc.
                        Box(modifier = Modifier.width(30.dp).height(34.dp), contentAlignment = Alignment.Center) {
                            if (q.items.size > 2) {
                                Box(
                                    modifier = Modifier
                                        .width(30.dp)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(colors.bg)
                                        .border(1.5.dp, colors.border, RoundedCornerShape(9.dp))
                                        .clickable {
                                            onChange(q.copy(items = q.items.filterIndexed { i, _ -> i != ii }))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Remove, "Xoá ý", tint = colors.text4, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        AddButton("Thêm ý", colors) { onChange(q.copy(items = q.items + TFItem("", true))) }
    }
}

@Composable
private fun MultipleFields(
    q: Question.Multiple,
    onChange: (Question) -> Unit,
    colors: LearnsyColors
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel("Câu hỏi", colors)
        RichInp(
            valueHtml = q.question,
            onChange = { onChange(q.copy(question = it)) },
            placeholder = "Nhập nội dung câu hỏi...",
            colors = colors,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        FieldLabel("Lựa chọn — bấm chữ cái để chọn đáp án đúng", colors)
        q.options.forEachIndexed { i, opt ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                val isCorrect = q.correct == i
                Box(
                    modifier = Modifier
                        .then(
                            if (isCorrect)
                                Modifier.shadow(2.dp, CircleShape, ambientColor = Color(0xFF10B981), spotColor = Color(0xFF10B981))
                            else Modifier
                        )
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isCorrect) Color(0xFF10B981) else colors.lavL)
                        .clickable { onChange(q.copy(correct = i)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(LETTERS[i], fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (isCorrect) Color.White else colors.lav)
                }
                MiniRichInp(
                    valueHtml = opt,
                    onChange = { v ->
                        onChange(q.copy(options = q.options.mapIndexed { idx, o -> if (idx == i) v else o }))
                    },
                    placeholder = "Lựa chọn ${LETTERS[i]}...",
                    colors = colors,
                    modifier = Modifier.weight(1f)
                )
                if (q.options.size > 2) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.bg)
                            .border(1.5.dp, colors.border, RoundedCornerShape(8.dp))
                            .clickable {
                                val newOptions = q.options.filterIndexed { idx, _ -> idx != i }
                                val newCorrect = when {
                                    q.correct == i -> 0
                                    q.correct > i -> q.correct - 1
                                    else -> q.correct
                                }
                                onChange(q.copy(options = newOptions, correct = newCorrect))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, "Xoá lựa chọn", tint = colors.text4, modifier = Modifier.size(11.dp))
                    }
                }
            }
        }
        if (q.options.size < 6) {
            AddButton("Thêm lựa chọn", colors) { onChange(q.copy(options = q.options + "")) }
        }
    }
}

@Composable
private fun MultiSelectFields(
    q: Question.MultiSelect,
    onChange: (Question) -> Unit,
    colors: LearnsyColors
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel("Câu hỏi", colors)
        RichInp(
            valueHtml = q.question,
            onChange = { onChange(q.copy(question = it)) },
            placeholder = "Nhập nội dung câu hỏi...",
            colors = colors,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        FieldLabel("Lựa chọn — bấm để chọn nhiều đáp án đúng", colors)
        q.options.forEachIndexed { i, opt ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                val isCorrect = i in q.correct
                Box(
                    modifier = Modifier
                        .then(
                            if (isCorrect)
                                Modifier.shadow(2.dp, RoundedCornerShape(9.dp), ambientColor = Color(0xFF10B981), spotColor = Color(0xFF10B981))
                            else Modifier
                        )
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isCorrect) Color(0xFF10B981) else colors.lavL)
                        .clickable {
                            onChange(q.copy(correct = if (i in q.correct) q.correct - i else q.correct + i))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(LETTERS[i], fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (isCorrect) Color.White else colors.lav)
                }
                MiniRichInp(
                    valueHtml = opt,
                    onChange = { v ->
                        onChange(q.copy(options = q.options.mapIndexed { idx, o -> if (idx == i) v else o }))
                    },
                    placeholder = "Lựa chọn ${LETTERS[i]}...",
                    colors = colors,
                    modifier = Modifier.weight(1f)
                )
                if (q.options.size > 2) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.bg)
                            .border(1.5.dp, colors.border, RoundedCornerShape(8.dp))
                            .clickable {
                                val newOptions = q.options.filterIndexed { idx, _ -> idx != i }
                                val newCorrect = q.correct.mapNotNull { c ->
                                    when {
                                        c == i -> null
                                        c > i -> c - 1
                                        else -> c
                                    }
                                }
                                onChange(q.copy(options = newOptions, correct = newCorrect))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, "Xoá lựa chọn", tint = colors.text4, modifier = Modifier.size(11.dp))
                    }
                }
            }
        }
        if (q.options.size < 6) {
            AddButton("Thêm lựa chọn", colors) { onChange(q.copy(options = q.options + "")) }
        }
    }
}

@Composable
private fun FillBlankFields(
    q: Question.FillBlank,
    onChange: (Question) -> Unit,
    colors: LearnsyColors
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel("Câu hỏi (dùng ___ cho chỗ trống)", colors)
        RichInp(
            valueHtml = q.question,
            onChange = { onChange(q.copy(question = it)) },
            placeholder = "Ví dụ: Ngô Quyền đánh tan quân ___ năm 938.",
            colors = colors,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel("Đáp án đúng", colors)
        OutlinedTextField(
            value = q.answer,
            onValueChange = { onChange(q.copy(answer = it)) },
            placeholder = { Text("Nhập đáp án chính xác...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.lav2,
                unfocusedBorderColor = colors.border,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface
            )
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel("Gợi ý (tùy chọn)", colors)
        OutlinedTextField(
            value = q.hint,
            onValueChange = { onChange(q.copy(hint = it)) },
            placeholder = { Text("Gợi ý dành cho học sinh...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.lav2,
                unfocusedBorderColor = colors.border,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface
            )
        )
    }
}
