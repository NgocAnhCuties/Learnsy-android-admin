package com.learnsy.admin.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

// Tương đương .bb-btn + .bb-clicking + @keyframes bb-marshmallow trong banh-beo-ui.css.
// Web có 2 trạng thái: hover (bb-wiggle) và click giữ (bb-marshmallow, squash 2 trục).
// Mobile không có hover thật, nên chỉ port marshmallow squash khi nhấn giữ — đó là
// phản hồi chạm quan trọng nhất, tương đương press feedback chuẩn của Material.
@Composable
fun rememberBbButtonScale(interactionSource: MutableInteractionSource): State<Float> {
    val pressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bb-marshmallow-scale"
    )
}

fun Modifier.bbButtonPress(scale: Float): Modifier = this.scale(scale)
