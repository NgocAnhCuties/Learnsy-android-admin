package com.learnsy.admin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.learnsy.admin.data.PasswordGenerator
import com.learnsy.admin.data.Student
import com.learnsy.admin.ui.theme.LearnsyColors

// Tương đương modal 'add' trong student-manager.jsx
@Composable
fun AddStudentModal(
    colors: LearnsyColors,
    dark: Boolean,
    classes: List<String>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (username: String, name: String, className: String, password: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }
    var password by remember { mutableStateOf(PasswordGenerator.generate()) }
    var showPass by remember { mutableStateOf(false) }

    ModalSheet(colors = colors, onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(Icons.Default.PersonAdd, null, tint = colors.rose)
            Text("Tạo tài khoản học sinh", fontSize = 16.sp, fontWeight = FontWeight.Black, color = colors.text)
        }
        Spacer(Modifier.height(16.dp))

        FormField("USERNAME *", colors) {
            OutlinedTextField(
                value = username, onValueChange = { username = it.replace(" ", "") },
                placeholder = { Text("vd: nguyenvana") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }
        FormField("TÊN HIỂN THỊ", colors) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                placeholder = { Text("vd: Nguyễn Văn A") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }
        FormField("LỚP", colors) {
            var classExpanded by remember { mutableStateOf(false) }
            val suggestions = classes.filter { it.contains(className, ignoreCase = true) && it != className }
            Box {
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it; classExpanded = classes.isNotEmpty() },
                    placeholder = { Text("vd: 11A7") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (classes.isNotEmpty()) {
                            IconButton(onClick = { classExpanded = !classExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, null, tint = colors.text3)
                            }
                        }
                    }
                )
                // Gợi ý lớp đã tồn tại — tương đương <datalist> trong bản web
                DropdownMenu(expanded = classExpanded && suggestions.isNotEmpty(), onDismissRequest = { classExpanded = false }) {
                    suggestions.forEach { c ->
                        DropdownMenuItem(text = { Text(c) }, onClick = { className = c; classExpanded = false })
                    }
                }
            }
        }
        FormField("MẬT KHẨU (để trống = tự sinh)", colors) {
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Row {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = colors.text3)
                        }
                        IconButton(onClick = { password = PasswordGenerator.generate() }) {
                            Icon(Icons.Default.Refresh, "Tạo mật khẩu ngẫu nhiên", tint = colors.lav)
                        }
                    }
                }
            )
            Text("🔒 Mã hoá bcrypt+pepper (cost 12) — admin không thể đọc lại", fontSize = 10.sp, color = colors.text3, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Huỷ") }
            Button(
                onClick = { onSubmit(username, name, className, password) },
                enabled = !saving,
                modifier = Modifier.weight(2f),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.rose)
            ) { Text(if (saving) "Đang lưu..." else "Tạo tài khoản ✨", fontWeight = FontWeight.Black) }
        }
    }
}

// Tương đương modal 'edit' trong student-manager.jsx
@Composable
fun EditStudentModal(
    student: Student,
    colors: LearnsyColors,
    dark: Boolean,
    saving: Boolean,
    classes: List<String>,
    avatarUrl: String?,
    uploading: Boolean,
    onAvatarClick: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (name: String, className: String) -> Unit
) {
    var name by remember { mutableStateOf(student.displayName ?: "") }
    var className by remember { mutableStateOf(student.className) }

    ModalSheet(colors = colors, onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(17.dp))
                    .background(Brush.linearGradient(listOf(colors.rose, colors.lav)))
                    .clickable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) {
                if (uploading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else if (avatarUrl != null) {
                    Image(rememberAsyncImagePainter(avatarUrl), null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(17.dp)), contentScale = ContentScale.Crop)
                } else {
                    Text((student.displayName ?: student.username).take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
            }
            Column {
                Text("Sửa thông tin", fontSize = 16.sp, fontWeight = FontWeight.Black, color = colors.text)
                Text("@${student.username}", fontSize = 12.sp, color = colors.text3)
            }
        }
        Spacer(Modifier.height(18.dp))

        FormField("TÊN HIỂN THỊ", colors) {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        FormField("LỚP", colors) {
            var classExpanded by remember { mutableStateOf(false) }
            val suggestions = classes.filter { it.contains(className, ignoreCase = true) && it != className }
            Box {
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it; classExpanded = classes.isNotEmpty() },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (classes.isNotEmpty()) {
                            IconButton(onClick = { classExpanded = !classExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, null, tint = colors.text3)
                            }
                        }
                    }
                )
                DropdownMenu(expanded = classExpanded && suggestions.isNotEmpty(), onDismissRequest = { classExpanded = false }) {
                    suggestions.forEach { c ->
                        DropdownMenuItem(text = { Text(c) }, onClick = { className = c; classExpanded = false })
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Huỷ") }
            Button(
                onClick = { onSubmit(name, className) },
                enabled = !saving,
                modifier = Modifier.weight(2f),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.rose)
            ) { Text(if (saving) "Đang lưu..." else "Lưu thay đổi", fontWeight = FontWeight.Black) }
        }
    }
}

// Tương đương modal 'view' (sau khi tạo) trong student-manager.jsx
@Composable
fun ViewCreatedModal(
    student: Student,
    plainPassword: String?,
    colors: LearnsyColors,
    dark: Boolean,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    ModalSheet(colors = colors, onDismiss = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("🎉", fontSize = 32.sp)
            Spacer(Modifier.height(6.dp))
            Text("Đã tạo tài khoản!", fontSize = 16.sp, fontWeight = FontWeight.Black, color = colors.text)
            Text("Lưu lại mật khẩu — sẽ không hiện lại sau khi đóng", fontSize = 12.sp, color = colors.text3, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))

        FormField("TÀI KHOẢN", colors) {
            Text(
                "@${student.username}" + (student.displayName?.let { " · $it" } ?: "") + (student.className.ifBlank { null }?.let { " · $it" } ?: ""),
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.bg).padding(12.dp)
            )
        }
        FormField("MẬT KHẨU", colors) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.bg).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    plainPassword ?: "(đã đặt riêng, không hiển thị)",
                    fontSize = 14.sp, fontWeight = FontWeight.Black, color = colors.rose, modifier = Modifier.weight(1f)
                )
                if (plainPassword != null) {
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(plainPassword))
                        Toast.makeText(context, "Đã copy mật khẩu!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, "Copy", tint = colors.lav, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.rose)
        ) { Text("Đã lưu, đóng lại", fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun ModalSheet(colors: LearnsyColors, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surface)
                .border(1.5.dp, colors.border, RoundedCornerShape(24.dp)).padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun FormField(label: String, colors: LearnsyColors, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = colors.text3, letterSpacing = 0.8.sp, modifier = Modifier.padding(bottom = 4.dp))
        content()
    }
}
