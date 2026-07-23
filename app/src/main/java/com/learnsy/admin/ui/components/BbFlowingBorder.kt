package com.learnsy.admin.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Tương đương .bb-input-wrap::before + @keyframes bb-border-flow trong banh-beo-ui.css —
// viền gradient 4 màu chạy khi input được focus. Compose không có background-position
// animation trực tiếp, nên dịch chuyển điểm bắt đầu/kết thúc của linear gradient theo
// thời gian để tạo hiệu ứng "chảy" tương tự.
@Composable
fun bbFlowingBorderModifier(base: Modifier, focused: Boolean, cornerRadius: Dp = 12.dp): Modifier {
    if (!focused) return base

    val transition = rememberInfiniteTransition(label = "bb-border-flow")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
        label = "bb-border-flow-phase"
    )

    val colors = listOf(Color(0xFFF472B6), Color(0xFFA855F7), Color(0xFF6EE7B7), Color(0xFF818CF8), Color(0xFFF472B6))
    val angleRad = (phase * 2 * Math.PI).toFloat()
    val dx = kotlin.math.cos(angleRad) * 200f
    val dy = kotlin.math.sin(angleRad) * 200f

    return base.border(
        width = 2.5.dp,
        brush = Brush.linearGradient(
            colors = colors,
            start = Offset(0f - dx, 0f - dy),
            end = Offset(200f + dx, 200f + dy)
        ),
        shape = RoundedCornerShape(cornerRadius + 2.dp)
    )
}
