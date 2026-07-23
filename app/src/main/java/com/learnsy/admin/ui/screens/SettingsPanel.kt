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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learnsy.admin.data.CardBlurLevel
import com.learnsy.admin.data.Lesson
import com.learnsy.admin.data.SettingsStore
import com.learnsy.admin.ui.ToastCenter
import com.learnsy.admin.ui.theme.LearnsyColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Tương đương SettingsPanel trong dashboard.jsx.
// Export HTML (Full/Lite) hiện chỉ hiện thông báo "chưa hỗ trợ" — cần
// export-builder.jsx để port buildExportHTML()/buildExportLiteHTML() thật.
@Composable
fun SettingsPanel(
    dark: Boolean,
    colors: LearnsyColors,
    onDarkToggle: () -> Unit,
    lessons: List<Lesson>,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    var name by remember { mutableStateOf(store.adminName) }
    var school by remember { mutableStateOf(store.school) }
    var cardBlur by remember { mutableStateOf(store.cardBlur) }
    var themePreset by remember {
        mutableStateOf(
            runCatching { com.learnsy.admin.ui.theme.ThemePreset.valueOf(store.themePreset.uppercase()) }
                .getOrDefault(com.learnsy.admin.ui.theme.ThemePreset.DEFAULT)
        )
    }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        SettingsCard(colors) {
            SettingsSectionHeader("Thông tin Admin", Icons.Default.Person, Color(0xFFF472B6), colors)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text("Tên hiển thị", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = colors.text3)
                    Spacer(Modifier.height(5.dp))
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        placeholder = { Text("Tên admin...") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                Column {
                    Text("Tên trường / lớp", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = colors.text3)
                    Spacer(Modifier.height(5.dp))
                    OutlinedTextField(
                        value = school, onValueChange = { school = it },
                        placeholder = { Text("VD: THPT An Nhơn Tây - Lớp 11A7") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                Button(
                    onClick = {
                        store.adminName = name
                        store.school = school
                        saved = true
                        scope.launch { delay(2000); saved = false }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (saved) Color(0xFF10B981) else Color(0xFFA855F7)
                    )
                ) {
                    Text(if (saved) "Đã lưu!" else "Lưu thông tin", fontWeight = FontWeight.Black)
                }
            }
        }

        SettingsCard(colors) {
            SettingsSectionHeader("Giao diện màu", Icons.Default.Palette, Color(0xFFD946EF), colors)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                com.learnsy.admin.ui.theme.ThemePreset.values().forEach { preset ->
                    val active = themePreset == preset
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) preset.dotColor.copy(alpha = 0.13f) else Color.Transparent)
                            .clickable {
                                themePreset = preset
                                store.themePreset = preset.name.lowercase()
                            }
                            .padding(10.dp, 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(20.dp).clip(RoundedCornerShape(999.dp)).background(preset.gradient()),
                            contentAlignment = Alignment.Center
                        ) {
                            if (active) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(11.dp))
                        }
                        Text(preset.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    }
                }
            }
        }

        SettingsCard(colors) {
            SettingsSectionHeader("Giao diện", Icons.Default.Palette, Color(0xFFA855F7), colors)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF472B6).copy(alpha = 0.06f))
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                    .padding(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (dark) "Chế độ tối" else "Chế độ sáng", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
                    Text("Thay đổi giao diện", fontSize = 11.sp, color = colors.text3)
                }
                Switch(checked = dark, onCheckedChange = { onDarkToggle() })
            }

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFA855F7).copy(alpha = 0.05f))
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                    .padding(13.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Blur card", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
                        Text("Xuyên thấu nền qua card bài học", fontSize = 11.sp, color = colors.text3)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(
                        Triple(CardBlurLevel.OFF, "Tắt", "Không blur"),
                        Triple(CardBlurLevel.FIFTY, "50%", "Nhẹ · xuyên card"),
                        Triple(CardBlurLevel.EIGHTY_FIVE, "85%", "Mạnh · trong suốt")
                    ).forEach { (level, lbl, desc) ->
                        val active = cardBlur == level
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(13.dp))
                                .background(if (active) Color(0xFFA855F7).copy(alpha = 0.1f) else Color.Transparent)
                                .border(1.5.dp, if (active) Color(0xFFA855F7) else colors.border, RoundedCornerShape(13.dp))
                                .clickable {
                                    cardBlur = level
                                    store.cardBlur = level
                                }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(lbl, fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (active) Color(0xFFA855F7) else colors.text3)
                            Text(desc, fontSize = 8.sp, color = colors.text3, maxLines = 1)
                        }
                    }
                }
            }
        }

        SettingsCard(colors) {
            SettingsSectionHeader("Về ứng dụng", Icons.Default.Info, Color(0xFF10B981), colors)
            listOf(
                "Phiên bản" to "Learnsy Admin (native) v0.1",
                "Tác giả" to "EnglishFun · Việt Nam",
                "Stack" to "Kotlin · Jetpack Compose · Supabase",
                "Hỗ trợ" to "Tiếng Anh, Lịch Sử, Vật Lý..."
            ).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(pair.first, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.text3)
                    Text(pair.second, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = colors.text2)
                }
                HorizontalDivider(color = colors.border)
            }
        }

        // Đăng xuất — chuyển từ header chính vào đây theo yêu cầu
        var showLogoutConfirm by remember { mutableStateOf(false) }
        if (showLogoutConfirm) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirm = false },
                icon = { Icon(Icons.Default.Logout, null, tint = Color(0xFFEF4444)) },
                title = { Text("Đăng xuất?", fontWeight = FontWeight.Black) },
                text = { Text("Bạn sẽ cần đăng nhập lại để tiếp tục quản trị.") },
                confirmButton = {
                    TextButton(onClick = { showLogoutConfirm = false; onLogout() }) {
                        Text("Đăng xuất", color = Color(0xFFEF4444), fontWeight = FontWeight.Black)
                    }
                },
                dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Huỷ") } }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEF4444).copy(alpha = 0.06f))
                .border(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .clickable { showLogoutConfirm = true }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Logout, null, tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
            }
            Text("Đăng xuất", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
        }
    }
}

@Composable
private fun ExportOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    colors: LearnsyColors,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.06f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f))
                    .border(1.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            }
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
                Text(subtitle, fontSize = 11.sp, color = colors.text3)
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(38.dp),
            shape = RoundedCornerShape(11.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color)
        ) {
            Icon(Icons.Default.Download, null, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text("Tải về", fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SettingsCard(colors: LearnsyColors, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.5.dp, colors.border, RoundedCornerShape(20.dp))
            .padding(15.dp, 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector, color: Color, colors: LearnsyColors) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
        }
        Text(title.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Black, color = colors.text2)
    }
}
