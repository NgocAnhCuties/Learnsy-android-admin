package com.learnsy.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.learnsy.admin.data.SettingsStore
import com.learnsy.admin.data.SupabaseConfig
import com.learnsy.admin.ui.branding.AtomBadge
import com.learnsy.admin.ui.screens.*
import com.learnsy.admin.ui.theme.Baloo2FontFamily
import com.learnsy.admin.ui.theme.DarkColors
import com.learnsy.admin.ui.theme.LightColors
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class MainTab(val label: String) {
    LESSONS("Bài học"), LISTENING("Listening"), STUDENTS("Học sinh"), DASHBOARD("Dashboard")
}

// Tương đương phần điều phối chính của admin.html + app.jsx (auth gate rồi vào app),
// gộp thêm bottom navigation giữa các module vì bản web dùng tab trên header
// còn Compose Material3 quy ước bottom nav cho mobile.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    var dark by remember { mutableStateOf(settings.darkMode) }

    var authed by remember { mutableStateOf<Boolean?>(null) }
    var currentTab by remember { mutableStateOf(MainTab.LESSONS) }
    var showDashboard by remember { mutableStateOf(false) }
    // Tương đương ẩn header tab/toolbar khi vào màn soạn bài (URL admin#e/...) trong app.jsx
    var lessonEditorOpen by remember { mutableStateOf(false) }
    // Tương đương nút refresh trong header (ảnh mẫu student app) — tăng để trigger reload
    var refreshTick by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        SupabaseConfig.client.auth.sessionStatus.collectLatest { status ->
            authed = status is SessionStatus.Authenticated
        }
    }

    val colors = if (dark) DarkColors else LightColors

    fun toggleDark() {
        dark = !dark
        settings.darkMode = dark
    }

    when (authed) {
        null -> {
            // Đang kiểm tra session — tránh nháy màn login rồi lại vào app
        }
        false -> {
            LoginScreen(dark = dark, onAuth = { })
        }
        true -> {
            if (showDashboard) {
                val lessonsVm: LessonListViewModel = viewModel()
                val lessonsState by lessonsVm.uiState.collectAsState()
                LaunchedEffect(Unit) { lessonsVm.load() }
                AdminDashboardScreen(
                    lessons = lessonsState.lessons,
                    dark = dark,
                    colors = colors,
                    adminName = settings.adminName,
                    onDarkToggle = ::toggleDark,
                    onClose = { showDashboard = false },
                    onLogout = {
                        showDashboard = false
                        scope.launch {
                            SupabaseConfig.client.auth.signOut()
                            currentTab = MainTab.LESSONS
                        }
                    }
                )
            } else {
                Scaffold(
                    topBar = {
                        if (!lessonEditorOpen) {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Logo — khối "L" trên nền gradient hồng-tím bo góc,
                                        // đồng bộ với header index (app học sinh).
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .background(
                                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                                        listOf(Color(0xFFF9A8D4), Color(0xFFC084FC))
                                                    ),
                                                    RoundedCornerShape(9.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AtomBadge(size = 17.dp, badgeColor = Color.White, backgroundColor = Color.Transparent)
                                        }
                                        Text(
                                            "Learnsy",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            fontFamily = Baloo2FontFamily,
                                            color = colors.text
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(colors.lavL)
                                                .border(1.dp, colors.border2, RoundedCornerShape(999.dp))
                                                .padding(horizontal = 9.dp, vertical = 3.dp)
                                        ) {
                                            Text("Admin", fontSize = 10.sp, fontWeight = FontWeight.Black, color = colors.lav)
                                        }
                                    }
                                },
                                actions = {
                                    // Refresh + Dark/Light — Box + clickable thuần (không dùng IconButton)
                                    // vì IconButton M3 tự thêm khung chạm 48dp làm nút to/dính hơn index.
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(17.dp))
                                            .background(if (dark) Color(0x26F59E0B) else Color(0x1AA855F7))
                                            .border(1.5.dp, if (dark) Color(0x4DF59E0B) else Color(0x40A855F7), RoundedCornerShape(17.dp))
                                            .clickable(
                                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                indication = null
                                            ) { refreshTick++ },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Làm mới",
                                            tint = if (dark) Color(0xFFF59E0B) else Color(0xFFA855F7),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(17.dp))
                                            .background(if (dark) Color(0x26F59E0B) else Color(0x1AA855F7))
                                            .border(1.5.dp, if (dark) Color(0x4DF59E0B) else Color(0x40A855F7), RoundedCornerShape(17.dp))
                                            .clickable(
                                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                indication = null
                                            ) { toggleDark() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "Đổi giao diện",
                                            tint = if (dark) Color(0xFFF59E0B) else Color(0xFFA855F7),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = colors.surface,
                                    titleContentColor = colors.text
                                )
                            )
                        }
                    },
                    bottomBar = {
                        if (!lessonEditorOpen) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentTab == MainTab.LESSONS,
                                    onClick = { currentTab = MainTab.LESSONS },
                                    icon = { Icon(Icons.Default.MenuBook, null) },
                                    label = { Text("Bài học") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == MainTab.LISTENING,
                                    onClick = { currentTab = MainTab.LISTENING },
                                    icon = { Icon(Icons.Default.Headphones, null) },
                                    label = { Text("Listening") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == MainTab.STUDENTS,
                                    onClick = { currentTab = MainTab.STUDENTS },
                                    icon = { Icon(Icons.Default.Groups, null) },
                                    label = { Text("Học sinh") }
                                )
                                NavigationBarItem(
                                    selected = false,
                                    onClick = { showDashboard = true },
                                    icon = { Icon(Icons.Default.Dashboard, null) },
                                    label = { Text("Dashboard") }
                                )
                            }
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(if (lessonEditorOpen) androidx.compose.foundation.layout.PaddingValues(0.dp) else padding)) {
                        when (currentTab) {
                            MainTab.LESSONS -> LessonListScreen(
                                colors = colors,
                                refreshKey = refreshTick,
                                onEditingChanged = { lessonEditorOpen = it }
                            )
                            MainTab.LISTENING -> ListeningManagerScreen(colors = colors, refreshKey = refreshTick)
                            MainTab.STUDENTS -> StudentManagerScreen(colors = colors, dark = dark, refreshKey = refreshTick)
                            MainTab.DASHBOARD -> { }
                        }
                    }
                }
            }
        }
    }
}
